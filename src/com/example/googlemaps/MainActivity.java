package com.example.googlemaps;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.googlemaps.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import es.cic.ejercicios.parsers.JSONParser;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity  extends android.support.v4.app.FragmentActivity {

	private LatLng posActual=null;
	private LatLng posIra=null;
	private GoogleMap mapa;
	private Location inicio=null;
	private LocationListener locListener;
	private boolean hasMarker = false;
	private Location actual=null;
	private Location origen=null;
	private Location destino=null;
	public Marker marker;
	public String url;
	private connectAsyncTask tarea;
	private ActionBar barra;
	private int modo =1;
	private Boolean hasPath = false;
	public Polyline line;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        barra = getActionBar();
        barra.setDisplayHomeAsUpEnabled(false);
        barra.setDisplayShowTitleEnabled(false);
        barra.setDisplayShowCustomEnabled(true);
        
       
        mapa = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        mapa.setMyLocationEnabled(true);
        LocationManager locManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        actual = new Location("Santander");
        origen = new Location("Santander");
        
         locListener = new LocationListener() {	
         
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
			
			@Override
			public void onProviderEnabled(String provider) {
			}
			
			@Override
			public void onProviderDisabled(String provider) {
			}
			
			@Override
			public void onLocationChanged(Location location) {
				try{
		
				actual.setLatitude(location.getLatitude());
				actual.setLongitude(location.getLongitude());
				origen.setLatitude(location.getLatitude());
				origen.setLongitude(location.getLongitude());

				actualizarPosicion(location);	
				}
				catch(NullPointerException e)
				{
					Toast.makeText(getBaseContext(), "PETOSE", Toast.LENGTH_LONG).show();
				}
			}	
		};
		
		if(!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			 alert.setTitle("Recurso de ubicación");
			 alert.setMessage("Se ha detectado que no tienes habilitado ninguna fuente de localizacion de ubicación. Debes activar al menos una para poder ejecutar la aplicación.\n Deseas activarla ahora?");


			 alert.setPositiveButton("Activar ahora", new DialogInterface.OnClickListener() {
				 
			 public void onClick(DialogInterface dialog, int whichButton) {
				 startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				
			   }
			 });
			 
			//Codigo a ejecutar si se cancela el dialog
			 alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
			   public void onClick(DialogInterface dialog, int whichButton) {
				   android.os.Process.killProcess(android.os.Process.myPid()) ;
			   }
			 });

			 alert.show();	
		}
		
		//Llamada de actualizacion de los providers
		locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 0, locListener);
		locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 0, locListener);
		
		//Al crearlo, necesito una actualizacion rápida, pero imprecisa
		try{
		inicio = new Location(locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
		origen = inicio;
		if(inicio != null)
		{
			actualizarPosicion(inicio);
			actual = inicio;
		}
		
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
			mapa.setOnMarkerClickListener(new OnMarkerClickListener() {
			
		
				@Override
				public boolean onMarkerClick(Marker marker) {		
				
					TrazarRuta(modo);
					return false;
				}
			});
			
			mapa.setOnMapLongClickListener(new OnMapLongClickListener() {
			
			@Override
			public void onMapLongClick(LatLng loc) {
				invalidateOptionsMenu();
				Toast.makeText(getApplication(), "Elige el tipo de transporte en el menú RUTA en la esquina superior derecha.", Toast.LENGTH_LONG).show();
					if(!hasMarker)
					{
						PonerMarcador(loc);
					}
					else
					{
						mapa.clear();
						PonerMarcador(loc);
					}}
		});
			
		    }


	protected void TrazarRuta(int modo) {
		if(hasPath)
		{
			mapa.clear();
		}
		
		url = makeURL(posActual.latitude ,posActual.longitude,destino.getLatitude(),destino.getLongitude(), modo);
		
		//Se invoca la tarea asincrona pasandole como argumento la url que contiene el JSON a tratar
		tarea = new connectAsyncTask(url);
		tarea.execute();
		hasPath = true;
		
	}

	protected void PonerMarcador(LatLng loc) {
    
		  String calle = "";
	    	
	    	Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
		 
				List<Address> addresses = null;
				try {
					addresses = geocoder.getFromLocation(loc.latitude,loc.longitude, 1);
				} catch (IOException e) {
					e.printStackTrace();
				}
				calle = addresses.get(0).getAddressLine(0);
				calle = calle + addresses.get(0).getAddressLine(1);
				

 		mapa.addMarker(new MarkerOptions()
		     .position(loc)
		     .title(calle)
		     .snippet("Posición: lat=" + loc.latitude + " long=" + loc.longitude)
		     .visible(true)
			 .draggable(false));
 		
 	     actual.setLatitude(loc.latitude);
	     actual.setLongitude(loc.longitude);
	     destino = actual;
	     destino.setLatitude(actual.getLatitude());
	     destino.setLongitude(actual.getLongitude());

	 	hasMarker = true;

	 	origen.setLatitude(loc.latitude);
	 	origen.setLongitude(loc.longitude);
	 	destino.setLatitude(loc.latitude);
	 	destino.setLongitude(loc.longitude);
	}
    
    
	private void actualizarPosicion(Location location) {
			posActual = new LatLng(location.getLatitude(),location.getLongitude());
	        CameraPosition camPos = new CameraPosition.Builder()
	        .target(posActual)
	        .zoom(15)         
	        .build();
	 
	        CameraUpdate camUpdate2 = CameraUpdateFactory.newCameraPosition(camPos);
	        mapa.animateCamera(camUpdate2);
	}
	
	private void Limpieza() {
			mapa.clear();
			actualizarPosicion(actual);
	}
	
	private void GoTo() {
		 AlertDialog.Builder alert = new AlertDialog.Builder(this);

		 alert.setTitle("Ir a...");
		 alert.setMessage("Introduce la dirección a localizar:");

		 // Set an EditText view to get user input 
		 final EditText input = new EditText(this);
		 alert.setView(input);

		 alert.setPositiveButton("Vamos!", new DialogInterface.OnClickListener() {
		 public void onClick(DialogInterface dialog, int whichButton) {
		    String direccion = input.getText().toString();
		  
		    Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
		    List<Address> addresses;
		    
			try {
				addresses = geocoder.getFromLocationName(direccion,1);
				if(addresses.get(0) != null){

					posIra = new LatLng(addresses.get(0).getLatitude(),addresses.get(0).getLongitude());
			        CameraPosition camPos = new CameraPosition.Builder()
			        .target(posIra)
			        .zoom(16)         
			        .build();
			 
			        CameraUpdate camUpdate2 = CameraUpdateFactory.newCameraPosition(camPos);
			        mapa.animateCamera(camUpdate2);	    
				    
				}
			
				    
			} catch (IOException e) {
				e.printStackTrace();
			}		
			
		   }
		 });
		
		 alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
		   public void onClick(DialogInterface dialog, int whichButton) {
		     //Codigo a ejecutar si se cancela el dialog
		   }
		 });

		 alert.show();
		
	}

	private void Trafico() {
		if(mapa.isTrafficEnabled())
		{
			mapa.setTrafficEnabled(false);
		}
		else
		{
			mapa.setTrafficEnabled(true);
		}

	}
	

	//Concatenador de url
		public String makeURL (double sourcelat, double sourcelog, double destlat, double destlog , int modo){
		    StringBuilder urlString = new StringBuilder();
		    urlString.append("http://maps.googleapis.com/maps/api/directions/json");
		    urlString.append("?origin=");// from
		    urlString.append(Double.toString(sourcelat));
		    urlString.append(",");
		    urlString.append(Double.toString( sourcelog));
		    urlString.append("&destination=");// to
		    urlString.append(Double.toString( destlat));
		    urlString.append(",");
		    urlString.append(Double.toString( destlog));
		    urlString.append("&sensor=true&alternatives=true&mode=");
		    
		    if(modo == 1)
		    {
		    	urlString.append("driving");
		    }
		    else if(modo == 2)
		    {
		    	urlString.append("walking");
		    }
		    else if(modo == 3)
		    {
		    	urlString.append("transit");
		    }
		    else if(modo == 4)
		    {
		    	urlString.append("bicycling");
		    }
		    return urlString.toString();
		}
	
	
	public void drawPath(String  result) {
	
	    try {
	            //Tranform the string into a json object
	           final JSONObject json = new JSONObject(result);
	           JSONArray routeArray = json.getJSONArray("routes");
	           JSONObject routes = routeArray.getJSONObject(0);
	           JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
	           String encodedString = overviewPolylines.getString("points");
	           List<LatLng> list =decodePoly(encodedString);

	           for(int z = 0; z<list.size()-1;z++){
	                LatLng src= list.get(z);
	                LatLng dest= list.get(z+1);
			
					 line = mapa.addPolyline(new PolylineOptions()
	                .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude))
	                .width(7)
	                .color(Color.BLUE).geodesic(true));
	            }
	    } 
	    catch (JSONException e) {

	    }
	} 
	
	private List<LatLng> decodePoly(String encoded) {

	    List<LatLng> poly = new ArrayList<LatLng>();
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

	        LatLng p = new LatLng( (((double) lat / 1E5)),
	                 (((double) lng / 1E5) ));
	        poly.add(p);
	    }

	    return poly;
	}
	
	
	private class connectAsyncTask extends AsyncTask<Void, Void, String>{
	    private ProgressDialog progressDialog;
	    String url;
	    connectAsyncTask(String urlPass){
	        url = urlPass;
	    }
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        progressDialog = new ProgressDialog(MainActivity.this);
	        progressDialog.setMessage("Calculando ruta, espere un momento...");
	        progressDialog.setIndeterminate(true);
	        progressDialog.show();
	    }
	    @Override
	    protected String doInBackground(Void... params) {
	        JSONParser jParser = new JSONParser();
	        String json = jParser.getJSONFromUrl(url);
	        return json;
	    }
	    @Override
	    protected void onPostExecute(String result) {
	        super.onPostExecute(result);   
	        progressDialog.hide();        
	        if(result!=null){
	            drawPath(result);
	        }
	        else
	        {
	        	Toast.makeText(getBaseContext(), "Selecciona una carretera válida.", Toast.LENGTH_SHORT).show();
	        }
	    }
	}
	
	private void StreetView() {
        Uri streetViewUri = Uri.parse(
                "google.streetview:cbll=" + actual.getLatitude() + "," + actual.getLongitude() + "&cbp=1,90,,0,1.0&mz=20");
        Intent streetViewIntent = new Intent(Intent.ACTION_VIEW, streetViewUri);
        startActivity(streetViewIntent);	  
	}
	
	
	//Menus
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int menuid = item.getItemId();
	    if(menuid == R.id.trafico)
	    {
	    	Trafico();
	    }
	    else if(menuid ==R.id.limpieza)
	    {
	    	Limpieza();
	    }
	    else if(menuid ==R.id.ira)
	    {
	    	GoTo();
	    }
	    else if(menuid ==R.id.street)
	    {
	    	StreetView();
	    }
	    else if(menuid ==R.id.normal)
	    {
	    	mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	    }
	    else if(menuid ==R.id.hibrido)
	    {
	    	mapa.setMapType(GoogleMap.MAP_TYPE_HYBRID);
	    }
	    else if(menuid ==R.id.satelite)
	    {
	    	mapa.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
	    }   
	    else if(menuid ==R.id.terreno)
	    {
	    	mapa.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
	    }
	    else if(menuid ==R.id.coche)
	    {
	    	modo=1;
	    	TrazarRuta(modo);
	    }
	    else if(menuid ==R.id.caminando)
	    {
	    	modo = 2;
	    	TrazarRuta(modo);
	    }
	    else if(menuid ==R.id.publico)
	    {
	    	modo =3;
	    	TrazarRuta(modo);
	    
	    }
	    else if(menuid ==R.id.bici)
	    {
	    	modo = 4;
	    	TrazarRuta(modo);
	    }

	    return true;
	}
	
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
	
		MenuItem register = menu.findItem(R.id.ruta);      
	    if(!hasMarker) 
	    {           
	        register.setVisible(false);
	    }
	    else
	    {
	        register.setVisible(true);
	    }
	    return true;

	}

}