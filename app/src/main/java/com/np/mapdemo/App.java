package com.np.mapdemo;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.np.mapdemo.util.VolleySingleton;

public class App extends Application {
    private RequestQueue requestQueue;
    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = VolleySingleton.getInstance(getApplicationContext()).getRequestQueue();
    }
    public RequestQueue getVolleyRequestQueue(){
        return requestQueue;
    }
}
