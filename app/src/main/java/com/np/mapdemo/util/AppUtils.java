package com.np.mapdemo.util;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.np.mapdemo.R;

public class AppUtils {
   public static int getMarkerColor(int i){
        if(i%2==0) {
            return AppConstants.RED;
        }//    markers.put(hamburg.getId(), "http://img.india-forums.com/images/100x100/37525-a-still-image-of-akshay-kumar.jpg");
        else if(i%3==0) {
            return AppConstants.GREEN;
        }
        else if(i%5==0) {
            return AppConstants.VIOLET;
        }
        else {
            return AppConstants.BLUE;
        }
    }
}
