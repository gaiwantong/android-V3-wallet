package info.blockchain.merchant.directory;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import org.json.simple.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.location.LocationListener;
import android.widget.ProgressBar;
//import android.util.Log;

import info.blockchain.wallet.R;

public class SuggestMerchant extends Activity	{

	private LocationManager locationManager = null;
	private LocationListener locationListener = null;
    private Location currLocation = null;
    private boolean gps_enabled = false;
//    private boolean net_enabled = false;
    
    private ProgressBar progress = null;

    private EditText edName = null;
    private EditText edDescription = null;
    private EditText edStreetAddress = null;
    private EditText edCity = null;
    private EditText edZip = null;
    private EditText edTelephone = null;
    private EditText edWeb = null;
    private TextView tvLatitude = null;
    private TextView tvLongitude = null;
    
    private LinearLayout lat_layout = null;
    private LinearLayout lon_layout = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    this.setContentView(R.layout.suggest_merchant);
	    this.setTitle(R.string.suggest_merchant);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
	    progress = (ProgressBar) findViewById(R.id.progressBar);
	    progress.setVisibility(View.GONE);

		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener();

        edName = (EditText)findViewById(R.id.name);
        edDescription = (EditText)findViewById(R.id.description);
        edStreetAddress = (EditText)findViewById(R.id.street_address);
        edCity = (EditText)findViewById(R.id.city);
        edZip = (EditText)findViewById(R.id.zip);
        edTelephone = (EditText)findViewById(R.id.telephone);
        edWeb = (EditText)findViewById(R.id.web);
        tvLatitude = (TextView)findViewById(R.id.latitude);
        tvLongitude = (TextView)findViewById(R.id.longitude);

        lat_layout = (LinearLayout)findViewById(R.id.lat_layout);
        lat_layout.setVisibility(View.GONE);
        lon_layout = (LinearLayout)findViewById(R.id.lon_layout);
        lon_layout.setVisibility(View.GONE);

        final List<String> categories = new ArrayList<String>();
		categories.add(getString(R.string.merchant_cat_hint));
		categories.add(getString(R.string.merchant_cat1));
		categories.add(getString(R.string.merchant_cat2));
		categories.add(getString(R.string.merchant_cat3));
		categories.add(getString(R.string.merchant_cat4));
		categories.add(getString(R.string.merchant_cat5));

    	final Spinner spCategory = (Spinner)findViewById(R.id.category);
        ArrayAdapter<String> categorySpinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.layout_spinner_item, categories);
        categorySpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categorySpinnerArrayAdapter);
        
		try {
			gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch(Exception e) {
			gps_enabled = false;
		}

		if(gps_enabled) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		}

        Button bOK = (Button)findViewById(R.id.ok);
        bOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	if(spCategory.getSelectedItemPosition() == 0) {
					Toast.makeText(SuggestMerchant.this, R.string.missing_merchant_category, Toast.LENGTH_SHORT).show();
            		return;
            	}

            	if(edName.getText().toString() == null || edName.getText().toString().length() == 0) {
					Toast.makeText(SuggestMerchant.this, R.string.missing_merchant_name, Toast.LENGTH_SHORT).show();
            		return;
            	}

            	/*
            	if(edStreetAddress.getText().toString() == null || edStreetAddress.getText().toString().length() == 0) {
					Toast.makeText(SuggestMerchant.this, R.string.missing_merchant_street_address, Toast.LENGTH_SHORT).show();
            		return;
            	}
            	
            	if(edCity.getText().toString() == null || edCity.getText().toString().length() == 0) {
					Toast.makeText(SuggestMerchant.this, R.string.missing_merchant_city, Toast.LENGTH_SHORT).show();
            		return;
            	}
            	
            	if(edZip.getText().toString() == null || edZip.getText().toString().length() == 0) {
					Toast.makeText(SuggestMerchant.this, R.string.missing_merchant_zip, Toast.LENGTH_SHORT).show();
            		return;
            	}
            	*/
            	
            	final HashMap<Object,Object> params = new HashMap<Object,Object>();
            	params.put("NAME", edName.getText().toString());
            	params.put("DESCRIPTION", edDescription.getText().toString());
            	params.put("STREET_ADDRESS", edStreetAddress.getText().toString());
            	params.put("CITY", edCity.getText().toString());
            	params.put("ZIP", edZip.getText().toString());
            	params.put("TELEPHONE", edTelephone.getText().toString());
            	params.put("WEB", edWeb.getText().toString());
            	params.put("LATITUDE", tvLatitude.getText().toString());
            	params.put("LONGITUDE", tvLongitude.getText().toString());
            	params.put("CATEGORY", Integer.toString(spCategory.getSelectedItemPosition()));
            	params.put("SOURCE", "Android");
/*
        		final Handler handler = new Handler();

        		new Thread(new Runnable() {
        			@Override
        			public void run() {
        				
        				Looper.prepare();

                    	String res = null;
                    	try {
                        	res = piuk.blockchain.android.util.WalletUtils.postURLWithParams("s://merchant-directory.blockchain.info/cgi-bin/btcp.pl", params);
                    	}
                    	catch(Exception e) {
        					Toast.makeText(SuggestMerchant.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    	}
                    	
                    	if(res.contains("\"result\":1")) {
        					Toast.makeText(SuggestMerchant.this, R.string.ok_writing_merchant, Toast.LENGTH_SHORT).show();
                        	setResult(RESULT_OK);
                        	finish();
                    	}
                    	else {
        					Toast.makeText(SuggestMerchant.this, R.string.error_writing_merchant, Toast.LENGTH_SHORT).show();
                    	}

        				Looper.loop();

        			}

        		}).start();
*/
            }
        });

        Button bCancel = (Button)findViewById(R.id.cancel);
        bCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	setResult(RESULT_CANCELED);
            	finish();
            }
        });

        final Button bLocation = (Button)findViewById(R.id.location);
        bLocation.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	if(currLocation != null) {
        	        lat_layout.setVisibility(View.VISIBLE);
        	        lon_layout.setVisibility(View.VISIBLE);

            		tvLatitude.setText(Double.toString(currLocation.getLatitude()));
            		tvLongitude.setText(Double.toString(currLocation.getLongitude()));
            		
            		bLocation.setVisibility(View.GONE);
            	}
            	else {
                	bLocation.setVisibility(View.GONE);
        			progress.setVisibility(View.VISIBLE);

                	if(gps_enabled) {

                		currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                		
                		if(currLocation != null) {
                	        lat_layout.setVisibility(View.VISIBLE);
                	        lon_layout.setVisibility(View.VISIBLE);

                    		tvLatitude.setText(Double.toString(currLocation.getLatitude()));
                    		tvLongitude.setText(Double.toString(currLocation.getLongitude()));

                    		bLocation.setVisibility(View.GONE);
                			progress.setVisibility(View.GONE);
                		}
                		else {
                        	bLocation.setVisibility(View.VISIBLE);
                			progress.setVisibility(View.GONE);
                		}

                	}
                	else {
                		
                		/*
                		try {
                			gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                		} catch(Exception e) {
                			gps_enabled = false;
                		}

                		if(gps_enabled) {
                			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                		}
                		*/
                		
                	}
            	}

            }
        });

    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(locationListener); 
	}

	private class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			if(location != null) {
				currLocation = location;
			}
			
			if(progress != null && progress.isShown()) {
				progress.setVisibility(View.GONE);
			}
			
		}

		@Override
		public void onProviderDisabled(String provider) { ; }

		@Override
		public void onProviderEnabled(String provider) { ; }

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) { ; }
	}

}
