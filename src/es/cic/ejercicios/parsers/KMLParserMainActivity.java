package es.cic.ejercicios.parsers;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.ekito.simpleKML.Serializer;
import com.ekito.simpleKML.model.Kml;
import com.example.googlemaps.*;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;

public class KMLParserMainActivity extends Activity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
	new TareaAsincrona().execute("test.kml");
	
	
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.main, menu);
	return true;
    }
    
    public class TareaAsincrona extends AsyncTask<String, String, Kml>{

    	private Serializer kmlSerializer;
    	private File output;
    	
    	public TareaAsincrona(){
    	    kmlSerializer = new Serializer();
    	}

	@Override
	protected Kml doInBackground(String... params) {
	    Kml kml = null;
	    try {
		InputStream is = getResources().getAssets().open(params[0]);
		Log.d("debug", "Parseando archivo..");
		kml = kmlSerializer.read(is);
		Log.d("debug", "Parseado finalizado.");
	    } catch (IOException e) {
		e.printStackTrace();
		Log.e("error", "Fallo al cargar el archivo KML.");
	    }
	    catch(Exception e){
		e.printStackTrace();
		Log.e("error", "Error durante el parseado del archivo KML.");
	    }
	    
	    if(kml != null){
		Log.d("debug", "Comienza la escritura");
		output = new File(getDir("assets", Context.MODE_PRIVATE), "aver.kml");
		try {
		    output = kmlSerializer.write(kml, output);
		} catch (Exception e) {
		    e.printStackTrace();
		    Log.d("debug", "Error durante la escritura del archivo de salida");
		}
	    }
	    
	    
	    return kml;
	}

}

}


