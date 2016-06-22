package com.example.rodrigo.raccoon_android;
// Android
import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.content.Context;
// Java
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
// Location
import android.location.Geocoder;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
// View
import android.util.Log;
import android.view.View;
// Widgets
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
// Google
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
// JSON
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.os.SystemClock.sleep;

public class EuQueroReciclar extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);

        setContentView(R.layout.activity_eu_quero_reciclar);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        LocationManager locManager;
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location location = locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

        LatLng current_location = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(current_location));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_location, 16));

        try {
            define_events();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void define_events() throws IOException
    {
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker)
            {
                View vMapInfo = EuQueroReciclar.this.getLayoutInflater().
                        inflate(R.layout.map_info_layout, null);

                TextView titleUi   = ((TextView) vMapInfo.findViewById(R.id.address_info));
                titleUi.setText(marker.getTitle());

                TextView snippetUi = ((TextView)vMapInfo.findViewById(R.id.garbage_info));
                snippetUi.setText(marker.getSnippet());

                return(vMapInfo);
            }

            @Override
            public View getInfoContents(Marker marker)
            {
                View v             = getLayoutInflater().inflate(R.layout.map_info_layout, null);
                String title       = marker.getTitle();
                TextView titleUi   = ((TextView) v.findViewById(R.id.address_info));
                String snippet     = marker.getSnippet();
                TextView snippetUi = ((TextView) v.findViewById(R.id.garbage_info));

                titleUi.setText(title);
                snippetUi.setText(snippet);
                return(v);
            }
        });

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener()
        {
            Marker marker;
            String        url = "http://euqueroreciclar.eco.br/street/garbage?";

            private void spinner()
            {
                final ProgressDialog progress = ProgressDialog.show(EuQueroReciclar.this,"","message");
                progress.show();
            }

            @Override
            public void onMapLongClick(LatLng point) {
                MarkerOptions info      = new MarkerOptions();
                Geocoder      geocoder  = new Geocoder(EuQueroReciclar.this);
                String        title;
                Address       address   = null;
                List<Address> addresses = null;
                String[]      parts;

                spinner();

                try {
                    address   = geocoder.getFromLocation(point.latitude, point.longitude, 1).get(0);
                    addresses = geocoder.getFromLocation(point.latitude, point.longitude, 100);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if((address.getThoroughfare() == null) || (address.getMaxAddressLineIndex() < 2)){
                    for (int i = 0; i < addresses.size(); i++) {
                        if(addresses.get(i).getThoroughfare() != null){
                            address = addresses.get(i);
                            break;
                        }
                    }
                }

                mMap.clear();

                if(address.getThoroughfare() == null)
                    return;

                title = address.getAddressLine(0);
                parts = title.split("\\s-\\s");

                info.position(point);
                info.title(parts[0]+"\n"+parts[1]);

                mMap.clear();
                marker = mMap.addMarker(info);

                System.out.println(address.toString());
                try {
                    String google = "https://maps.googleapis.com/maps/api/geocode/json?latlng=";
                    url += "city="+URLEncoder.encode(address.getLocality(), "utf-8");
                    url += "&address="+URLEncoder.encode(address_by_google(google+point.latitude+ "," + point.longitude), "utf-8");
                    url += "&number=1";
                    url += "&hood="+URLEncoder.encode(parts[1], "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String output = "";

                try {
                    marker.setSnippet(street_garbage(url));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("==> OUTPUT: " + output);
                marker.setSnippet(output);

                marker.setTitle(parts[0]+"\n"+parts[1]);
                marker.showInfoWindow();
            }
        });
    }

    public String address_by_google(String address) throws IOException
    {
        JSONObject jsonObj=null;
        URL url = new URL(address);
        String address_long = "";

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            byte[] contents = new byte[1024];
            int bytesRead = 0;
            String strFileContents = "";
            while((bytesRead = in.read(contents)) != -1) {
                strFileContents += new String(contents, 0, bytesRead);
            }

            try {
                jsonObj = new JSONObject(strFileContents);
                JSONArray array =  jsonObj.getJSONArray("results");
                String address_comp = array.getJSONObject(0).getString("address_components");
                JSONArray xarray = new JSONArray(address_comp);
                JSONObject addr_long_json   = (JSONObject) xarray.get(1);
                address_long = addr_long_json.getString("long_name");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } finally {
            urlConnection.disconnect();
        }
        return(address_long);
    }

    public String street_garbage(String address) throws IOException
    {
        String[] address_long = {""};
        String url_p  = address;

        System.out.println("==> street_garbage");
        JSONObject jsonObj=null;
        URL url = null;
        try {
            url = new URL(url_p);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        System.out.println("==> 1");

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        urlConnection.setRequestProperty("Content-Type","application/json");
        urlConnection.setRequestProperty("Accept", "application/json");
        try {
            urlConnection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        System.out.println("==> 2");
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            byte[] contents = new byte[1024];
            int bytesRead = 0;
            String strFileContents = "";
            while((bytesRead = in.read(contents)) != -1) {
                strFileContents += new String(contents, 0, bytesRead);
            }

            try {
                jsonObj = new JSONObject(strFileContents);
                address_long[0] += "Lixo Comum:\n" + jsonObj.getString("organic");
                address_long[0] += "\n";
                address_long[0] += "\n";
                address_long[0] += "Lixo Reciclável:\n" + jsonObj.getString("recycle");
                address_long[0] += "\n";
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
            System.out.println("==> 3:" + address_long[0]);
        }

        return(address_long[0]);
    }
}
