package com.np.mapdemo.model.places;

public class StoreModel {
    public String name, address, distance, duration;
    public  String lat,longi;

    public StoreModel(String name, String address, String distance, String duration,String lat, String longi) {

        this.name = name;
        this.address = address;
        this.distance = distance;
        this.duration = duration;
        this.lat = lat;
        this.longi = longi;

    }
}
