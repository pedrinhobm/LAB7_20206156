package com.example.moneymanager;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Map;

public class ServicioAlmacenamiento {

    private static final String TAG = "ServicioAlmacenamiento";
    private final Context context;

    public interface UploadResultListener {
        void onSuccess(String url);
        void onFailure(String error);
    }

    public interface DownloadResultListener {
        void onSuccess(File file);
        void onFailure(String error);
    }

    public ServicioAlmacenamiento(Context context) {
        // Al recibir el contexto, asegúrate de almacenar el ApplicationContext para uso futuro.
        // Esto previene fugas de memoria si se le pasa un Activity Context.
        this.context = context.getApplicationContext();
        Log.d(TAG, "ServicioAlmacenamiento inicializado con Application Context.");
    }

    // *** MÉTODO conexionAlServicio ELIMINADO ***
    // La inicialización de Cloudinary ahora se maneja exclusivamente en MyApplication.java
    // Esto asegura que MediaManager.init() se llama una única vez con el Application Context.


    /**
     * Uploads a file to Cloudinary.
     *
     * @param fileUri The URI of the file to upload.
     * @param listener Callback for upload results.
     */
    public void guardarArchivo(Uri fileUri, final UploadResultListener listener) {
        try {
            // Asegúrate de que MediaManager ha sido inicializado (esto lo hace MyApplication)
            // Si MediaManager no se ha inicializado, MediaManager.get() lanzará una IllegalStateException.
            MediaManager.get().upload(fileUri)
                    .option("folder", "money_manager_comprobantes")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Subida iniciada para requestId: " + requestId);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            double progress = (double) bytes / totalBytes;
                            Log.d(TAG, "Progreso de subida para requestId " + requestId + ": " + (int)(progress * 100) + "%");
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = (String) resultData.get("url");
                            Log.d(TAG, "Uploaded URL from Cloudinary: " + url);
                            listener.onSuccess(url);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e(TAG, "Error en la subida para requestId " + requestId + ": " + error.getDescription());
                            listener.onFailure(error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Subida reprogramada para requestId " + requestId + ": " + error.getDescription());
                        }
                    }).dispatch();
        } catch (IllegalStateException e) {
            // Este catch es vital. Ocurrirá si guardarArchivo se llama ANTES de que MyApplication
            // haya inicializado Cloudinary. Si el flujo es correcto, esto no debería pasar.
            listener.onFailure("Cloudinary no está inicializado. Error de configuración de la aplicación.");
            Log.e(TAG, "Error: MediaManager.get() llamado antes de la inicialización adecuada en MyApplication: " + e.getMessage());
        } catch (Exception e) {
            listener.onFailure("Error inesperado al intentar subir archivo: " + e.getMessage());
            Log.e(TAG, "Error inesperado en guardarArchivo: " + e.getMessage(), e);
        }
    }

    /**
     * Downloads a file from a given URL.
     *
     * @param fileUrl The URL of the file to download.
     * @param listener Callback for download results.
     */
    public void obtenerArchivo(String fileUrl, final DownloadResultListener listener) {
        Log.d(TAG, "ServicioAlmacenamiento recibió URL para descargar: " + fileUrl);
        if (fileUrl == null || fileUrl.isEmpty()) {
            listener.onFailure("URL de archivo vacía o nula.");
            return;
        }

        // Usa el AsyncTask anidado estático para evitar fugas de memoria
        // Pasa el Application Context almacenado al AsyncTask
        new DownloadFileAsyncTask(context, listener).execute(fileUrl);
    }

    // Declara AsyncTask como una clase anidada estática para prevenir fugas de memoria
    private static class DownloadFileAsyncTask extends AsyncTask<String, Void, File> {
        private final Context context;
        private final DownloadResultListener listener;
        private String errorMessage = null;
        private static final String TAG = "ServicioAlmacenamiento.DownloadTask"; // Nombre de TAG más específico

        // El constructor recibe el ApplicationContext de ServicioAlmacenamiento
        DownloadFileAsyncTask(Context context, DownloadResultListener listener) {
            this.context = context; // Ya es el ApplicationContext, no necesitas .getApplicationContext() aquí de nuevo
            this.listener = listener;
        }

        @Override
        protected File doInBackground(String... params) {
            String urlString = params[0];
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            File file = null;
            try {
                java.net.URL url = new java.net.URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    errorMessage = "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage() + " para URL: " + urlString;
                    Log.e(TAG, errorMessage);
                    return null;
                }

                input = connection.getInputStream();
                // Crea un archivo temporal en el directorio de caché de la aplicación
                // Asegúrate de que context.getCacheDir() no sea nulo.
                // context aquí es el ApplicationContext, que siempre debería tener un directorio de caché.
                file = File.createTempFile("image_comprobante_", ".jpg", context.getCacheDir());
                output = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                Log.d(TAG, "Archivo descargado exitosamente a: " + file.getAbsolutePath());
                return file;
            } catch (IOException e) {
                errorMessage = "Error al descargar el archivo desde " + urlString + ": " + e.getMessage();
                Log.e(TAG, errorMessage, e);
                return null;
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                    Log.e(TAG, "Error al cerrar streams/desconectar después de la descarga: " + e.getMessage(), e);
                }
            }
        }

        @Override
        protected void onPostExecute(File downloadedFile) {
            if (listener != null) {
                if (downloadedFile != null) {
                    listener.onSuccess(downloadedFile);
                } else {
                    listener.onFailure(errorMessage != null ? errorMessage : "No se pudo descargar el archivo.");
                }
            }
        }
    }
}