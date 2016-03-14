package com.mastahed.gonav;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        TextToSpeech.OnInitListener {

    //Define a request code to send to Google Play services
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private TextToSpeech tts;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private EditText txtlocation;
    private double currentLatitude;
    private double currentLongitude;
    private int REQUEST_CODE = 0;
    int PLACE_PICKER_REQUEST = 1;

    private int userIcon, foodIcon, drinkIcon, shopIcon, otherIcon;
    private Marker[] placeMarkers;
    private final int MAX_PLACES = 20;
    private MarkerOptions[] places;
    PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        tts = new TextToSpeech(this, this);

        buildGoogleApiClient();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        txtlocation = (EditText) findViewById(R.id.txtLocation);

        txtlocation.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;

                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String location = txtlocation.getText().toString();
                    showLocation(location);
                    handled = true;
                }

                return handled;
            }
        });

        ImageButton btnVoice = (ImageButton) findViewById(R.id.imgBtnMic);

        btnVoice.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }

        });

        userIcon = R.drawable.yellow_point;
        foodIcon = R.drawable.red_point;
        drinkIcon = R.drawable.blue_point;
        shopIcon = R.drawable.green_point;
        otherIcon = R.drawable.purple_point;

    }

    private void promptSpeechInput() {

        String DIALOG_TEXT = "Search Location";
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, DIALOG_TEXT);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, REQUEST_CODE);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Not Supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultcode, Intent intent) {
        super.onActivityResult(requestCode, resultcode, intent);
        ArrayList<String> speech;

        if (resultcode == RESULT_OK) {

            switch(requestCode) {
                case 0:
                    speech = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    txtlocation.setText(speech.get(0));
                    showLocation(speech.get(0));

                    break;
                case 1:
                    // The user has selected a place. Extract the name and address.
                    final Place place = PlacePicker.getPlace(this, intent);

                    final CharSequence name = place.getName();
                    final CharSequence address = place.getAddress();
                    final CharSequence phone = place.getPhoneNumber();
                    final Uri site = place.getWebsiteUri();
                    final LatLng latlng = place.getLatLng();

                    Intent resultIntent = new Intent(this, ResultActivity.class);
                    resultIntent.putExtra("name", name);
                    resultIntent.putExtra("address", address);
                    resultIntent.putExtra("phone", phone);
                    resultIntent.putExtra("site", site);
                    resultIntent.putExtra("latlng", latlng);
                    startActivity(resultIntent);

                    //showLocationByLatLng(String.valueOf(name), latlng);
                    //updateMapView(latlng);

                    //finishActivity(PLACE_PICKER_REQUEST);

                    break;
            }

        }
        /*else {
            Toast.makeText(MapsActivity.this, "Unable to search. Please try again.", Toast.LENGTH_SHORT).show();
            tts.speak("Unable to search. Please try again.", TextToSpeech.QUEUE_ADD, null);
        }*/

    }

    protected void showLocation(String location) {

        if (!location.equals("")) {
            // Speak location
            tts.speak("Searching for " + location + ". Please wait.", TextToSpeech.QUEUE_FLUSH, null);

            Geocoder geocoder = new Geocoder(MapsActivity.this);
            List<Address> addressList = null;

            try {
                // Populate nearby addresses
                addressList = geocoder.getFromLocationName(location, 1); // select only one result

            } catch (IOException e) {
                e.printStackTrace();
            }

            Address address;

            if (addressList != null) {
                address = addressList.get(0); // get the first result
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                showLocationByLatLng(location, latLng);

            } else {
                tts.speak("I'm sorry. I can't find " + location + ". Please try again.", TextToSpeech.QUEUE_ADD, null);
            }
        }
        else {
            tts.speak("Please enter your desired location.", TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void showLocationByLatLng(String location, LatLng latLng) {
        // current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == 0)
        {
            Location curloc  = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            LatLng curLatlng = new LatLng(curloc.getLatitude(), curloc.getLongitude());

            showNearbyPlaces(new LatLngBounds(latLng, curLatlng));

            updateMapView(latLng);

            showDistance(location, curLatlng, latLng);
        }
    }

    private void showDistance(String location, LatLng startLatLng, LatLng endLatLng) {
        Double distance = CalculationByDistance(startLatLng, endLatLng);
        String diststr = String.valueOf(distance);

        tts.speak("Your distance from " + location + " is " + diststr + " kilometers.", TextToSpeech.QUEUE_ADD, null);

        Toast.makeText(MapsActivity.this, "Distance (km): " + diststr, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().
                findFragmentById(R.id.map);
        View mapView = mapFragment.getView();

        if (mapView != null &&
                mapView.findViewById(1) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(1).getParent()).findViewById(2);
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(30, 30, 0, 0);
        }
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient
                    .Builder(this)
                    .enableAutoManage(this, 0, this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Now lets connect to the API
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(this.getClass().getSimpleName(), "onPause()");

        //Disconnect from API onPause()
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mMap.setMyLocationEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == 0) {

            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (location == null) {
                tts.speak("Cannot find your current location. Please check your settings.",TextToSpeech.QUEUE_FLUSH, null);

            } else {
                //If everything went fine lets get latitude and longitude
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();

                // Add a marker in my current location and move the camera
                LatLng curLoc = new LatLng(currentLatitude, currentLongitude);
                updateMapView(curLoc);

                //showNearbyPlaces(new LatLngBounds(curLoc, curLoc));
            }
        }
    }

    private void updateMapView(LatLng loc) {
        mMap.addMarker(new MarkerOptions().position(loc));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
             * Google Play services can resolve some errors it detects.
             * If the error has a resolution, try sending an Intent to
             * start a Google Play services activity that can resolve
             * error.
             */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                    /*
                     * Thrown if Google Play services canceled the original
                     * PendingIntent
                     */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
                /*
                 * If no resolution is available, display a dialog to the
                 * user with the error.
                 */
            Log.e("Error", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    /**
     * If locationChanges change lat and long
     *
     *
     * @param location
     */

    public void onLocationChanged(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    private void showNearbyPlaces(LatLngBounds latLngBounds) {
        intentBuilder.setLatLngBounds(latLngBounds);
        Intent intent = null;

        try {
            intent = intentBuilder.build(MapsActivity.this);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
        startActivityForResult(intent, PLACE_PICKER_REQUEST);

    }

    private class GetPlaces extends AsyncTask<String, Void, String> {

        //fetch and parse place data
        @Override
        protected String doInBackground(String... placesURL) {
            StringBuilder placesBuilder = new StringBuilder();

            Log.i("Places URL", String.valueOf(placesURL));

            //process search parameter string(s)
            for (String placeSearchURL : placesURL) {
                //execute search
                HttpClient placesClient = new DefaultHttpClient();

                try {
                    //try to fetch the data
                    HttpGet placesGet = new HttpGet(placeSearchURL);
                    HttpResponse placesResponse = placesClient.execute(placesGet);
                    StatusLine placeSearchStatus = placesResponse.getStatusLine();

                    if (placeSearchStatus.getStatusCode() == 200) {
                        //we have an OK response
                        HttpEntity placesEntity = placesResponse.getEntity();
                        InputStream placesContent = placesEntity.getContent();
                        InputStreamReader placesInput = new InputStreamReader(placesContent);
                        BufferedReader placesReader = new BufferedReader(placesInput);

                        String lineIn;
                        while ((lineIn = placesReader.readLine()) != null) {
                            placesBuilder.append(lineIn);
                        }
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

            return placesBuilder.toString();
        }

        @Override
        public void onPostExecute(String result) {
            //parse place data returned from Google Places
            if(placeMarkers!=null){
                for(int pm=0; pm<placeMarkers.length; pm++){
                    if(placeMarkers[pm]!=null)
                        placeMarkers[pm].remove();
                }
            }

            try {
                //parse JSON
                JSONObject resultObject = new JSONObject(result);
                JSONArray placesArray = resultObject.getJSONArray("results");

                Log.i("Places Array", String.valueOf(placesArray));

                places = new MarkerOptions[placesArray.length()];

                boolean missingValue=false;
                LatLng placeLL=null;
                String placeName="";
                String vicinity="";
                int currIcon = otherIcon;

                //loop through places
                for (int p=0; p<placesArray.length(); p++) {
                    //parse each place
                    try{
                        //attempt to retrieve place data values
                        missingValue=false;

                        JSONObject placeObject = placesArray.getJSONObject(p);

                        JSONObject loc = placeObject.getJSONObject("geometry").getJSONObject("location");

                        placeLL = new LatLng(
                                Double.valueOf(loc.getString("lat")),
                                Double.valueOf(loc.getString("lng")));

                        JSONArray types = placeObject.getJSONArray("types");

                        for(int t=0; t<types.length(); t++){
                            String thisType=types.get(t).toString();

                            if(thisType.contains("restaurant")){
                                currIcon = foodIcon;
                                break;
                            }
                            else if(thisType.contains("bar")){
                                currIcon = drinkIcon;
                                break;
                            }
                            else if(thisType.contains("store")){
                                currIcon = shopIcon;
                                break;
                            }
                        }

                        vicinity = placeObject.getString("vicinity");
                        placeName = placeObject.getString("name");
                    }
                    catch(JSONException jse){
                        missingValue=true;
                        jse.printStackTrace();
                    }

                    if(missingValue)    places[p]=null;
                    else
                        places[p]=new MarkerOptions()
                                .position(placeLL)
                                .title(placeName)
                                .icon(BitmapDescriptorFactory.fromResource(currIcon))
                                .snippet(vicinity);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            if(places!=null && placeMarkers!=null){
                for(int p=0; p<places.length && p<placeMarkers.length; p++){
                    //will be null if a value was missing
                    if(places[p]!=null)
                        placeMarkers[p]=mMap.addMarker(places[p]);
                }
            }
        }
    }

    public double CalculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        //return Radius * c;
        return kmInDec;
    }
}
