package com.np.mapdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.np.mapdemo.model.DirectionObject;
import com.np.mapdemo.model.LegsObject;
import com.np.mapdemo.model.PolylineObject;
import com.np.mapdemo.model.RouteObject;
import com.np.mapdemo.model.StepsObject;
import com.np.mapdemo.model.places.PlacesPOJO;
import com.np.mapdemo.model.places.ResultDistanceMatrix;
import com.np.mapdemo.model.places.StoreModel;
import com.np.mapdemo.retrofit.APIClient;
import com.np.mapdemo.retrofit.ApiInterface;
import com.np.mapdemo.util.AppConstants;
import com.np.mapdemo.util.GpsUtils;
import com.np.mapdemo.util.GsonRequest;
import com.np.mapdemo.util.Helper;
import com.np.mapdemo.util.VolleySingleton;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import noman.googleplaces.NRPlaces;
import noman.googleplaces.Place;
import noman.googleplaces.PlaceType;
import noman.googleplaces.PlacesException;
import noman.googleplaces.PlacesListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, PlacesListener {
    @BindView(R.id.mapview)
    MapView mapview;
    @BindView(R.id.btnDirection)
    Button btnDirection;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean isContinue = false;
    private boolean isGPS = false;
    Marker mCurrLocationMarker;
    GoogleApiClient mGoogleApiClient;
    Circle circle;
    CircleOptions circleOptions;
    BottomSheetBehavior mBottomSheetBehavior2;
    private List<LatLng> latLngList;
    private MarkerOptions SourceLocationMarker;
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    private final static int ALL_PERMISSIONS_RESULT = 101;
    List<StoreModel> storeModels;
    ApiInterface apiService;

    String latLngString;
    LatLng latLng;
    List<PlacesPOJO.CustomA> results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);
        latLngList = new ArrayList<LatLng>();

        final View bottomSheet2 = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior2 = BottomSheetBehavior.from(bottomSheet2);
        mBottomSheetBehavior2.setHideable(false);
        mBottomSheetBehavior2.setPeekHeight(0);
        mBottomSheetBehavior2.setState(BottomSheetBehavior.STATE_COLLAPSED);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        circleOptions = new CircleOptions();
        setLocationRequest();

        locationCallback();
        try {
            mapview.onCreate(savedInstanceState);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapview.getMapAsync(this);

        changeStatusBarColor();
        mBottomSheetBehavior2.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    //  ivIncidentArrow.setImageResource(R.drawable.down_arrow);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    //  ivIncidentArrow.setImageResource(R.drawable.added_icon);
                }

            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
            }
        });

    }

   void getSchools(){
     //  apiService = APIClient.getClient().create(ApiInterface.class);
     //  fetchStores("school","school");

       new NRPlaces.Builder()
               .listener(this)
               .key(APIClient.GOOGLE_PLACE_API_KEY)
               .latlng(latLng.latitude, latLng.longitude)
               .radius(1500).type(PlaceType.SCHOOL)
               .build()
               .execute();
    }
    void setLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000); // 10 seconds
        locationRequest.setFastestInterval(5 * 1000);
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void refreshMap(GoogleMap mapInstance) {
        mapInstance.clear();
    }

    void getDirection(LatLng latLng) {
        if (latLngList.size() > 0) {
            refreshMap(mMap);
            latLngList.clear();
        }
        latLngList.add(latLng);
        //    Log.d(TAG, "Marker number " + latLngList.size());
        mMap.addMarker(SourceLocationMarker);
        mMap.addMarker(new MarkerOptions().position(latLng));
        LatLng defaultLocation = SourceLocationMarker.getPosition();
        LatLng destinationLocation = latLng;
        //use Google Direction API to get the route between these Locations
        String directionApiPath = Helper.getUrl(String.valueOf(defaultLocation.latitude), String.valueOf(defaultLocation.longitude),
                String.valueOf(destinationLocation.latitude), String.valueOf(destinationLocation.longitude));
        // Log.d(TAG, "Path " + directionApiPath);
        getDirectionFromDirectionApiServer(directionApiPath);
    }

    void setBehavior() {

        if (mBottomSheetBehavior2.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior2.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (mBottomSheetBehavior2.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            mBottomSheetBehavior2.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

    }

    void GpsEnable() {
        new GpsUtils(this).turnGPSOn(new GpsUtils.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                // turn on GPS
                isGPS = isGPSEnable;

            }
        });
    }


    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    AppConstants.LOCATION_REQUEST);

        } else {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    void locationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                       latLng=new LatLng(location.getLatitude(),location.getLongitude());
                        latLngString = location.getLatitude() + "," + location.getLongitude();
                        setMapLocation(location);

                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapview != null)
            mapview.onResume();
        GpsEnable();
        getLocation();
    }

    void setMapLocation(Location location) {

        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        setGeoFence(latLng);
       /* SourceLocationMarker = new MarkerOptions();
        SourceLocationMarker.position(latLng);
        // markerOptions.title("Current Position");
        SourceLocationMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mCurrLocationMarker = mMap.addMarker(SourceLocationMarker);
*/
        //move map camera

       /* CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(15.0f).build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(cameraUpdate);*/
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e("", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("", "Can't find style. Error: ", e);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                setBehavior();
                return false;
            }
        });
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                    mMap.setMyLocationEnabled(true);

                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppConstants.GPS_REQUEST) {
                isGPS = true; // flag maintain before get location
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapview != null)
            mapview.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapview != null)
            mapview.onDestroy();

    }


    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        setLocationRequest();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (lastLocation != null)
                setMapLocation(lastLocation);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    void setGeoFence(LatLng point) {
        if (circle == null) {
            // Specifying the center of the circle
            circleOptions.center(point);

            // Radius of the circle
            circleOptions.radius(1 * 1609.34);// Converting Miles into Meters...

            // Border color of the circle
            circleOptions.strokeColor(Color.parseColor("#fff0f5"));

            // Fill color of the circle
            // 0x represents, this is an hexadecimal code
            // 55 represents percentage of transparency. For 100% transparency, specify 00.
            // For 0% transparency ( ie, opaque ) , specify ff
            // The remaining 6 characters(00ff00) specify the fill color
            //circleOptions.fillColor(Color.parseColor("#F8F4E3"));

            // Border width of the circle
            circleOptions.strokeWidth(50);

           /* circle.remove();
        }*/
            // Adding the circle to the GoogleMap
            circle = mMap.addCircle(circleOptions);

            float currentZoomLevel = getZoomLevel(circle);
            float animateZomm = currentZoomLevel + 5;

            Log.e("Zoom Level:", currentZoomLevel + "");
            Log.e("Zoom Level Animate:", animateZomm + "");


            //move map camera

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 14));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);
            getSchools();
        }
    }

    public float getZoomLevel(Circle circle) {
        float zoomLevel = 0;
        if (circle != null) {
            double radius = circle.getRadius();
            double scale = radius / 400;
            zoomLevel = (int) (16 - Math.log(scale) / Math.log(2));
        }
        return zoomLevel + .5f;
    }


    private void getDirectionFromDirectionApiServer(String url) {
        GsonRequest<DirectionObject> serverRequest = new GsonRequest<DirectionObject>(
                Request.Method.GET,
                url,
                DirectionObject.class,
                createRequestSuccessListener(),
                createRequestErrorListener());
        serverRequest.setRetryPolicy(new DefaultRetryPolicy(
                Helper.MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(serverRequest);
    }

    private Response.Listener<DirectionObject> createRequestSuccessListener() {
        return new Response.Listener<DirectionObject>() {
            @Override
            public void onResponse(DirectionObject response) {
                try {
                    Log.d("JSON Response", response.toString());
                    if (response.getStatus().equals("OK")) {
                        List<LatLng> mDirections = getDirectionPolylines(response.getRoutes());
                        drawRouteOnMap(mMap, mDirections);
                    } else {
                        Toast.makeText(MapsActivity.this, R.string.server_error, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ;
        };
    }

    private List<LatLng> getDirectionPolylines(List<RouteObject> routes) {
        List<LatLng> directionList = new ArrayList<LatLng>();
        for (RouteObject route : routes) {
            List<LegsObject> legs = route.getLegs();
            for (LegsObject leg : legs) {
                List<StepsObject> steps = leg.getSteps();
                for (StepsObject step : steps) {
                    PolylineObject polyline = step.getPolyline();
                    String points = polyline.getPoints();
                    List<LatLng> singlePolyline = decodePoly(points);
                    for (LatLng direction : singlePolyline) {
                        directionList.add(direction);
                    }
                }
            }
        }
        return directionList;
    }

    private Response.ErrorListener createRequestErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        };
    }

    private void drawRouteOnMap(GoogleMap map, List<LatLng> positions) {
        circle.setVisible(false);
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLACK).geodesic(true);
        options.addAll(positions);
        int p=1;
        if(positions!=null && positions.size()>2){
            p=positions.size()/2;
        }
        Polyline polyline = map.addPolyline(options);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(positions.get(p).latitude, positions.get(p).longitude))
                .zoom(12)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }

    /**
     * Method to decode polyline points
     * Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     */
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    @OnClick(R.id.btnDirection)
    public void onViewClicked() {
        LatLng latLng=new LatLng(28.540600,77.212967);

        getDirection(latLng);
    }




    public void fetchStores(String placeType, String businessName) {


        /**
         * For Locations In India McDonalds stores aren't returned accurately
         */

        //Call<PlacesPOJO.Root> call = apiService.doPlaces(placeType, latLngString,"\""+ businessName +"\"", true, "distance", APIClient.GOOGLE_PLACE_API_KEY);

        Call<PlacesPOJO.Root> call = apiService.doPlaces(placeType, latLngString, businessName, true, "distance", APIClient.GOOGLE_PLACE_API_KEY);
        call.enqueue(new Callback<PlacesPOJO.Root>() {

            @Override
            public void onResponse(Call<PlacesPOJO.Root> call, retrofit2.Response<PlacesPOJO.Root> response) {
                PlacesPOJO.Root root = response.body();


                if (response.isSuccessful()) {

                    if (root.status.equals("OK")) {

                        results = root.customA;
                        storeModels = new ArrayList<>();
                        for (int i = 0; i < results.size(); i++) {

                            if (i == 10)
                                break;
                            PlacesPOJO.CustomA info = results.get(i);

                            storeModels.add(new StoreModel(info.name, info.vicinity, "", "",info.geometry.locationA.lat,info.geometry.locationA.lng));


                           // fetchDistance(info);

                        }
                        for(int i=0;i<storeModels.size();i++) {
                            if(i%2==0) {
                                final Marker hamburg = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(storeModels.get(i).lat), Double.parseDouble(storeModels.get(i).longi))).title("" + i).icon(BitmapDescriptorFactory.fromResource(R.drawable.primary)));

                            }//    markers.put(hamburg.getId(), "http://img.india-forums.com/images/100x100/37525-a-still-image-of-akshay-kumar.jpg");
                            else if(i%3==0) {
                                final Marker hamburg = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(storeModels.get(i).lat), Double.parseDouble(storeModels.get(i).longi))).title("" + i).icon(BitmapDescriptorFactory.fromResource(R.drawable.upper_primary)));

                            }
                            else if(i%5==0) {
                                final Marker hamburg = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(storeModels.get(i).lat), Double.parseDouble(storeModels.get(i).longi))).title("" + i).icon(BitmapDescriptorFactory.fromResource(R.drawable.secondary)));

                            }
                            else {
                                final Marker hamburg = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(storeModels.get(i).lat), Double.parseDouble(storeModels.get(i).longi))).title("" + i).icon(BitmapDescriptorFactory.fromResource(R.drawable.higher_secondary)));

                            }
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "No matches found near you", Toast.LENGTH_SHORT).show();
                    }

                } else if (response.code() != 200) {
                    Toast.makeText(getApplicationContext(), "Error " + response.code() + " found.", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<PlacesPOJO.Root> call, Throwable t) {
                // Log error here since request failed
                call.cancel();
            }
        });


    }
    private void fetchDistance(final PlacesPOJO.CustomA info) {

        Call<ResultDistanceMatrix> call = apiService.getDistance(APIClient.GOOGLE_PLACE_API_KEY, latLngString, info.geometry.locationA.lat + "," + info.geometry.locationA.lng);
        call.enqueue(new Callback<ResultDistanceMatrix>() {

            @Override
            public void onResponse(Call<ResultDistanceMatrix> call, retrofit2.Response<ResultDistanceMatrix> response) {

                ResultDistanceMatrix resultDistance = response.body();
                if ("OK".equalsIgnoreCase(resultDistance.status)) {

                    ResultDistanceMatrix.InfoDistanceMatrix infoDistanceMatrix = resultDistance.rows.get(0);
                    ResultDistanceMatrix.InfoDistanceMatrix.DistanceElement distanceElement = infoDistanceMatrix.elements.get(0);
                    if ("OK".equalsIgnoreCase(distanceElement.status)) {
                        ResultDistanceMatrix.InfoDistanceMatrix.ValueItem itemDuration = distanceElement.duration;
                        ResultDistanceMatrix.InfoDistanceMatrix.ValueItem itemDistance = distanceElement.distance;
                        String totalDistance = String.valueOf(itemDistance.text);
                        String totalDuration = String.valueOf(itemDuration.text);

                        /*if (storeModels.size() == 10 || storeModels.size() == results.size()) {
                            RecyclerViewAdapter adapterStores = new RecyclerViewAdapter(results, storeModels);
                            recyclerView.setAdapter(adapterStores);
                        }*/

                    }

                }

            }

            @Override
            public void onFailure(Call<ResultDistanceMatrix> call, Throwable t) {
                call.cancel();
            }
        });

    }

    @Override
    public void onPlacesFailure(PlacesException e) {

    }

    @Override
    public void onPlacesStart() {

    }

    @Override
    public void onPlacesSuccess(List<Place> places) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                storeModels = new ArrayList<>();
                for (int i = 0; i < places.size(); i++) {

                    if (i == 10)
                        break;
                    Place info = places.get(i);

                    storeModels.add(new StoreModel(info.getName(), info.getVicinity(), "", "",""+info.getLatitude(),""+info.getLongitude()));
                    // fetchDistance(info);

                }

                for(int i=0;i<storeModels.size();i++) {
                    if(i%2==0) {
                        final Marker hamburg = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(storeModels.get(i).lat), Double.parseDouble(storeModels.get(i).longi))).title("" + i).icon(BitmapDescriptorFactory.fromResource(R.drawable.primary)));

                    }//    markers.put(hamburg.getId(), "http://img.india-forums.com/images/100x100/37525-a-still-image-of-akshay-kumar.jpg");
                    else if(i%3==0) {
                        final Marker hamburg = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(storeModels.get(i).lat), Double.parseDouble(storeModels.get(i).longi))).title("" + i).icon(BitmapDescriptorFactory.fromResource(R.drawable.upper_primary)));

                    }
                    else if(i%5==0) {
                        final Marker hamburg = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(storeModels.get(i).lat), Double.parseDouble(storeModels.get(i).longi))).title("" + i).icon(BitmapDescriptorFactory.fromResource(R.drawable.secondary)));

                    }
                    else {
                        final Marker hamburg = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(storeModels.get(i).lat), Double.parseDouble(storeModels.get(i).longi))).title("" + i).icon(BitmapDescriptorFactory.fromResource(R.drawable.higher_secondary)));

                    }
                }
            }
        });


    }

    @Override
    public void onPlacesFinished() {

    }
}
