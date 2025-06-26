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

// como se indicó en el enuncado , esta clase tiene como rol de gestionar las operaciones de almacenamiento de los archivos
// con el CLoudinary tanto subidas como descargas , sobretodo de las imagenes

public class ServicioAlmacenamiento {
    private static final String TAG = "ServicioAlmacenamiento";
    private final Context context; // servirá para las operaciones del archivo y Cloudinary

    public interface UploadResultListener { // asi que dividimos la clase en 2 ártes : subida o actualización / descarga
        void onSuccess(String url);  // en cada, se explicará los casos de error y éxito
        void onFailure(String error);
    }
    public interface DownloadResultListener {
        void onSuccess(File file);
        void onFailure(String error);
    }

    public ServicioAlmacenamiento(Context context) {
        this.context = context.getApplicationContext(); // Al recibir el contexto, se asegura de almacenar el ApplicationContext para uso futuro
        // Así se evita fugas de memoria, es decir que se estalece la conexion al servicio Cloudinary con la clase anterior MyApplication
    }

    public void guardarArchivo(Uri fileUri, final UploadResultListener listener) {
        try { // aqui si use IA porque necesitaba como se tenía que llevar los datos al servidor
            MediaManager.get().upload(fileUri) // el proceso inicia con la subida del archivo
                    .option("folder", "money_manager_comprobantes") // esta es la carpeta donde se guardarán las imagenes en Cloudinary
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Subida iniciada para requestId");
                        }

                        @Override // aqui se llama para reportar el progreso de subida
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            double progress = (double) bytes / totalBytes; // lo distribuimos por como ha avanzado el requestId
                            Log.d(TAG, "Progreso de subida para requestId " + requestId + ": " + (int)(progress * 100) + "%");
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = (String) resultData.get("url"); // se llama una subida exotsa
                            listener.onSuccess(url); // el resutlData es el contenido de la imagen subida junto al URL
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            listener.onFailure(error.getDescription()); // en los casos no deseados, llamos por si hubo un error
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) { // esto si use ia en caso hubiera fallos por problemas técicos de red
                            Log.w(TAG, "Subida reprogramada para requestId " + requestId + ": " + error.getDescription());
                        }

                    }).dispatch();
        } catch (IllegalStateException e) {
            listener.onFailure("Cloudinary no está inicializado");
        } catch (Exception e) {
            listener.onFailure("Error inesperado al intentar subir archivo");
        }
    }

    // la siguiente funcion es la descarga del archivo
    // aqui si use un hilo AysncTask para que bloquee la UI
    public void obtenerArchivo(String fileUrl, final DownloadResultListener listener) {
        if (fileUrl == null || fileUrl.isEmpty()) { // si no hay nada, confirmara como falla
            listener.onFailure("URL de archivo vacía");
            return;
        } // el proceso inicia con el metodo indicado tambien para evitar fuga de memoria
        new DownloadFileAsyncTask(context, listener).execute(fileUrl); //  DownloadFileAsyncTask, similar a lo visto en IEE06
    }

    private static class DownloadFileAsyncTask extends AsyncTask<String, Void, File> {
        private final Context context; // tambien use IA ara el asynctask porque
        private final DownloadResultListener listener; // se requieria manejar operaciones de red del hilo main
        private String errorMessage = null;
        private static final String TAG = "ServicioAlmacenamiento.DownloadTask";

        DownloadFileAsyncTask(Context context, DownloadResultListener listener) {
            this.context = context; // es por ello que armo un constructor para recibir su applicationContext
            this.listener = listener;
        }

        @Override
        protected File doInBackground(String... params) {
            String urlString = params[0]; // para esa función, obtiene el URL
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            File file = null;
            try {
                java.net.URL url = new java.net.URL(urlString); // luego abre y establece la conexión HTTP
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) { // despues, valida la respuesta del servidor si es conforme
                    errorMessage = "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage() + " para URL: " + urlString;
                    Log.e(TAG, errorMessage);
                    return null; // si sucede el escenario de fallo de respuesta HTTP lo returna nulo
                }

                input = connection.getInputStream(); // aquí se obtiene el stream de entrada de conexión
                // para ello , se crea un archivo temporal en el directorio de caché de la aplicación
                file = File.createTempFile("image_comprobante_", ".jpg", context.getCacheDir());
                output = new FileOutputStream(file); //  luego la salida

                byte[] buffer = new byte[4096]; // este es el buffer que lee los datos por bloque
                int bytesRead; // en la iterativa leera el stream de entrada y luego ecibe la salida
                while ((bytesRead = input.read(buffer)) != -1) { // hasta que no haya datos
                    output.write(buffer, 0, bytesRead);
                }
                return file; // y es asi como se devuelve el archivo ya descargado
            } catch (IOException e) {
                errorMessage = "Error al descargar el archivo desde " + urlString + ": " + e.getMessage();
                return null;
            } finally {
                try { // y ya por ultimo se confgirma que la conexion y los streams esten cerrados
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                    Log.e(TAG, "Error al cerrar streams/desconectar después de la descarga");
                }
            }
        }

        @Override
        protected void onPostExecute(File downloadedFile) { // este es el hilo main que mencione anteriormente
            if (listener != null) {// y actualiza la UI y notifica el resultado al listener del archivo a descargar
                if (downloadedFile != null) {
                    listener.onSuccess(downloadedFile);
                } else { // en caso de error , lo informará
                    listener.onFailure(errorMessage != null ? errorMessage : "No se pudo descargar el archivo.");
                }
            }
        }
    }
}