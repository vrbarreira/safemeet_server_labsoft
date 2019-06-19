package br.usp.poli.labsoft.safemeetserver;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import br.usp.poli.labsoft.safemeet.LocationGPS;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback{

    private MapView mapView;
    private GoogleMap gmap;
    private String message = "";
    private ServerSocket serverSocket;
    private TextView info, infoip;
    private ListView listView;
    private Button btnClearReg;
    private int countSckThread;

    private LatLng pos;

    private List<LocationGPS> locList;
    private ArrayAdapter<LocationGPS> locAdapter;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        countSckThread = 0;

        info = (TextView) findViewById(R.id.info);
        infoip = (TextView) findViewById(R.id.infoip);

        infoip.setText(getIpAddress());

        final Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView = (MapView) findViewById(R.id.map_view);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        locList = new ArrayList<LocationGPS>();
        locAdapter = new ArrayAdapter<LocationGPS>(
                this, android.R.layout.simple_list_item_1, locList);

        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(locAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LocationGPS loc = locList.get(i);
                atualizaMapa(loc.getDatetime(), loc.getLatitude(), loc.getLongitude(), 17);
            }
        });

        btnClearReg = (Button) findViewById(R.id.btnClearReg);
        btnClearReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locList.clear();
                locAdapter.clear();
                gmap.clear();
                countSckThread = 0;
                Toast.makeText(getApplicationContext(),"Lista de registros limpa", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        //gmap.setMinZoomPreference(12);
        gmap.setIndoorEnabled(true);

        UiSettings uiSettings = gmap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);

        //pos = new LatLng(-34, 151);
        //gmap.addMarker(new MarkerOptions().position(pos).title("Marker in Sydney"));
        //gmap.moveCamera(CameraUpdateFactory.newLatLng(pos));

        mapView.onResume();
    }

    private class SocketServerThread extends Thread {

        static final int SocketServerPORT = 8080;
        //int count = 0;
        private Date datetime;
        private double latitude, longitude;

        @Override
        public void run() {
            Socket socket = null;
            ObjectInputStream objInputStream = null;
            //DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        info.setText("I'm waiting here: "
                                + serverSocket.getLocalPort());
                    }
                });

                while (true) {
                    socket = serverSocket.accept();
                    objInputStream = new ObjectInputStream(socket.getInputStream());
                    //dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    //String messageFromClient = "";
                    LocationGPS messageFromClient = null;

                    //If no message sent from client, this code will block the program
                    //messageFromClient = dataInputStream.readUTF();
                    messageFromClient = (LocationGPS) objInputStream.readObject();

                    countSckThread++;
                    /*
                    message = "#" + count + " from " + socket.getInetAddress()
                            + ":" + socket.getPort() + "\n"
                            + "Msg from client: " + messageFromClient + "\n";
                    */
                    message = messageFromClient.toString() + "\n";
                    datetime = messageFromClient.getDatetime();
                    latitude = messageFromClient.getLatitude();
                    longitude = messageFromClient.getLongitude();

                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            //msg.setText(message);
                            locAdapter.add(new LocationGPS(datetime,latitude,longitude));
                            atualizaMapa(datetime, latitude, longitude, 15);
                            //Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        }
                    });

                    String msgReply = "Hello from Android, you are #" + countSckThread;
                    dataOutputStream.writeUTF(msgReply);

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                final String errMsg = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        //msg.setText(errMsg);
                        Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (objInputStream != null) {
                    try {
                        objInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    private void atualizaMapa(Date datetime, double latitude, double longitude, int zoomLevel){
        Toast.makeText(getApplicationContext(),"Latitude: "+latitude
                +"\nLongitude: "+longitude,Toast.LENGTH_SHORT).show();

        MarkerOptions mko = new MarkerOptions();

        pos = new LatLng(latitude, longitude);
        mko.position(pos);

        if(datetime != null)
            mko.title("Registrado em: "
                    + DateFormat.getDateTimeInstance().format(datetime).toString());

        gmap.clear();
        gmap.addMarker(mko);
        gmap.moveCamera(CameraUpdateFactory.newLatLng(pos));
        gmap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
    }

}
