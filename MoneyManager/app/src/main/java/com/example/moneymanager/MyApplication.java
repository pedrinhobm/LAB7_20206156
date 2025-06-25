package com.example.moneymanager;
import android.app.Application;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application { // esta clase lo he creado para probar que me he conectado con el servicio de nube Cloudinary
    private static final String TAG = "MyApplication"; // en un inicio quise realizarlo con FireStorage, pero me resulto complicado dado que me pedian registrar credenciales más delicadas como la tarjeta
    private ServicioAlmacenamiento servicioAlmacenamientoInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        servicioAlmacenamientoInstance = new ServicioAlmacenamiento(this);
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "doima1ui0"); // estas son las credenciales del nombre de mi nube y los apis solicitados
            config.put("api_key", "612228851356863");
            config.put("api_secret", "Iku01hLIbbHzOYKtSPjf852EJyQ");
            config.put("secure", "true"); // aquí su use IA porque no podía guardar las fotos HTTP y trate de asegurar con HTTPS
            MediaManager.init(this, config);
            Log.d(TAG, "Cloudinary inicializado con éxito"); // los logs indicaran la conexion establecida
        } catch (IllegalStateException e) {
            Log.w(TAG, "Cloudinary ya estaba inicializado");
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar servicio");
        }
    }

    public ServicioAlmacenamiento getServicioAlmacenamiento() {
        return servicioAlmacenamientoInstance; // aqui llamamos a la clase solicitada del laboratorio ServicioAlmacenamiento
    }
}