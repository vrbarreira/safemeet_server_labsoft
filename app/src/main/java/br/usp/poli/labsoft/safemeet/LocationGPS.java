package br.usp.poli.labsoft.safemeet;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

public class LocationGPS implements Serializable {

    private Date datetime;
    private double latitude, longitude;

    public LocationGPS(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public LocationGPS(Date datetime, double latitude, double longitude){
        this.datetime = datetime;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Date getDatetime() {
        return datetime;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {

        return "Datetime: " + DateFormat.getDateTimeInstance().format(datetime).toString()
                +"\nLatitude: " + this.latitude + "\nLongitude: " + this.longitude;
    }
}
