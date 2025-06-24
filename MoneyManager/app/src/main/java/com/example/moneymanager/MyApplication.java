package com.example.moneymanager;

import android.app.Application;
import android.util.Log;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private ServicioAlmacenamiento servicioAlmacenamientoInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MyApplication onCreate iniciado.");
        servicioAlmacenamientoInstance = new ServicioAlmacenamiento(this);
        Log.d(TAG, "Inicializando Cloudinary desde MyApplication...");
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "doima1ui0"); //
            config.put("api_key", "612228851356863"); //
            config.put("api_secret", "Iku01hLIbbHzOYKtSPjf852EJyQ"); //
            config.put("secure", "true"); // Asegura que siempre use HTTPS
            MediaManager.init(this, config); // Usa 'this' (Application Context) para MediaManager.init()
            Log.d(TAG, "Cloudinary inicializado con éxito desde MyApplication.");
        } catch (IllegalStateException e) {
            Log.w(TAG, "Cloudinary ya estaba inicializado o hubo un error de estado (configuración): " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar Cloudinary desde MyApplication: " + e.getMessage(), e);
        }
    }

    public ServicioAlmacenamiento getServicioAlmacenamiento() {
        return servicioAlmacenamientoInstance;
    }
}