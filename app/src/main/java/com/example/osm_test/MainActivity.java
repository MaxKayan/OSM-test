package com.example.osm_test;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.osm_test.databinding.ActivityMainBinding;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    private final LabelledGeoPoint POINT_RGUTIS = new LabelledGeoPoint(55.4331145, 37.5562910, "RGUTIS");

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's
        //tile servers will get you banned based on this string

        //inflate and create the map
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initMap();

        setDefaultView();

        setPolylines();

        setMarkers();

        requestPermissionsIfNecessary(new String[]{
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        binding.map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }


    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        binding.map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }


    private void initMap() {
        final Context context = this;
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();

        binding.map.setTileSource(TileSourceFactory.MAPNIK);
        //needed for pinch zooms
        binding.map.setMultiTouchControls(true);

        //scales tiles to the current screen's DPI, helps with readability of labels
        binding.map.setTilesScaledToDpi(true);

        //My Location
        //note you have handle the permissions yourself, the overlay did not do it for you
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), binding.map);
        locationOverlay.enableMyLocation();
        binding.map.getOverlays().add(locationOverlay);


        //Mini map
        MinimapOverlay minimapOverlay = new MinimapOverlay(context, binding.map.getTileRequestCompleteHandler());
        minimapOverlay.setWidth(dm.widthPixels / 5);
        minimapOverlay.setHeight(dm.heightPixels / 5);
        binding.map.getOverlays().add(minimapOverlay);


        //Copyright overlay
        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(context);
        //i hate this very much, but it seems as if certain versions of android and/or
        //device types handle screen offsets differently
        binding.map.getOverlays().add(copyrightOverlay);


        //On screen compass
        CompassOverlay compassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context),
                binding.map);
        compassOverlay.enableCompass();
        binding.map.getOverlays().add(compassOverlay);


        //support for map rotation
        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(binding.map);
        rotationGestureOverlay.setEnabled(true);
        binding.map.getOverlays().add(rotationGestureOverlay);
    }


    private void setDefaultView() {
        IMapController mapController = binding.map.getController();
        mapController.setZoom(14.5);
        mapController.setCenter(POINT_RGUTIS);
    }


    private void setPolylines() {
        IMapController mapController = binding.map.getController();

        List<GeoPoint> geoPoints = new ArrayList<>();
        //add your points here
        Polyline line = new Polyline();   //see note below!
        line.setPoints(geoPoints);
        line.setOnClickListener(new Polyline.OnClickListener() {
            @Override
            public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                Toast.makeText(mapView.getContext(), "polyline with " + polyline.getActualPoints().size() + "pts was tapped", Toast.LENGTH_LONG).show();
                return false;
            }
        });

        binding.map.getOverlayManager().add(line);
    }


    private void setMarkers() {
        Marker rgutMarker = new Marker(binding.map);
        rgutMarker.setPosition(POINT_RGUTIS);
        rgutMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        rgutMarker.setTitle(POINT_RGUTIS.getLabel());
        binding.map.getOverlays().add(rgutMarker);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        List<String> permissionsToRequest = new ArrayList<>();
        permissionsToRequest.addAll(Arrays.asList(permissions).subList(0, grantResults.length));

        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


    private void requestPermissionsIfNecessary(@NonNull String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}
