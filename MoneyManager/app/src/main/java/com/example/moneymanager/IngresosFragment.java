package com.example.moneymanager;
import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IngresosFragment extends Fragment implements TransactionAdapter.OnTransactionActionListener<Ingreso> {
    private RecyclerView recyclerViewIngresos;
    private TransactionAdapter<Ingreso> adapter;
    private List<Ingreso> ingresosList;
    private FloatingActionButton fabAddIngreso;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Uri selectedImageUri;
    private ImageView imageViewComprobante;
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private static final String TAG = "IngresosFragment";

    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imageViewComprobante.setImageURI(selectedImageUri);
                    imageViewComprobante.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                    selectedImageUri = null;
                    imageViewComprobante.setImageResource(R.drawable.ic_image_placeholder);
                    imageViewComprobante.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Permiso concedido para guardar archivos.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Permiso denegado. No se podrán guardar archivos.", Toast.LENGTH_LONG).show();
                }
            });

    public IngresosFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        ingresosList = new ArrayList<>();

        if (getActivity() != null) {
            servicioAlmacenamiento = ((MyApplication) getActivity().getApplication()).getServicioAlmacenamiento();
        } else {
            servicioAlmacenamiento = new ServicioAlmacenamiento(getContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ingresos, container, false);

        recyclerViewIngresos = view.findViewById(R.id.recyclerViewIngresos);
        recyclerViewIngresos.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter<>(getContext(), ingresosList, true, this);
        recyclerViewIngresos.setAdapter(adapter);
        fabAddIngreso = view.findViewById(R.id.fabAddIngreso);
        fabAddIngreso.setOnClickListener(v -> showAddEditTransactionDialog(null));

        if (currentUser == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        } else {
            loadIngresos();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        return view;
    }

    private void loadIngresos() {
        if (currentUser == null)
            return;
        db.collection("ingresos")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ingresosList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Ingreso ingreso = document.toObject(Ingreso.class);
                            if (ingreso != null) {
                                ingreso.setId(document.getId());
                                String urlFromFirebase = ingreso.getComprobanteUrl();
                                if (urlFromFirebase != null && urlFromFirebase.startsWith("http://")) {
                                    ingreso.setComprobanteUrl(urlFromFirebase.replace("http://", "https://"));
                                } else {
                                    Log.d(TAG, "Firebase Ingreso URL es ya HTTPS o nula: " + urlFromFirebase);
                                }
                                ingresosList.add(ingreso);
                            }
                        }
                        Collections.sort(ingresosList, (i1, i2) -> {
                            if (i1.getFecha() == null || i2.getFecha() == null) return 0;
                            return i2.getFecha().compareTo(i1.getFecha());
                        });
                        adapter.setTransactionList(ingresosList);
                    } else {
                        Toast.makeText(getContext(), "Error al cargar ingresos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddEditTransactionDialog(@Nullable Ingreso ingresoToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_transaction, null);
        builder.setView(dialogView);
        EditText editTextTitulo = dialogView.findViewById(R.id.editTextTitulo);
        EditText editTextMonto = dialogView.findViewById(R.id.editTextMonto);
        EditText editTextDescripcion = dialogView.findViewById(R.id.editTextDescripcion);
        EditText editTextFecha = dialogView.findViewById(R.id.editTextFecha);
        Button buttonSave = dialogView.findViewById(R.id.buttonSave);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        imageViewComprobante = dialogView.findViewById(R.id.imageViewComprobante);
        Button buttonSelectImage = dialogView.findViewById(R.id.buttonSelectImage);
        selectedImageUri = null;
        imageViewComprobante.setImageResource(R.drawable.ic_image_placeholder);
        imageViewComprobante.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        AlertDialog dialog = builder.create();

        if (ingresoToEdit != null) {
            dialogTitle.setText("Editar Ingreso");
            editTextTitulo.setText(ingresoToEdit.getTitulo());
            editTextMonto.setText(String.valueOf(ingresoToEdit.getMonto()));
            editTextDescripcion.setText(ingresoToEdit.getDescripcion());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (ingresoToEdit.getFecha() != null) {
                editTextFecha.setText(sdf.format(ingresoToEdit.getFecha()));
            }

            if (ingresoToEdit.getComprobanteUrl() != null && !ingresoToEdit.getComprobanteUrl().isEmpty()) {
                Picasso.get()
                        .load(ingresoToEdit.getComprobanteUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(imageViewComprobante);
                imageViewComprobante.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }

        } else {
            dialogTitle.setText("Nuevo Ingreso");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            editTextFecha.setText(sdf.format(Calendar.getInstance().getTime()));
        }

        editTextFecha.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (ingresoToEdit == null && editTextFecha.getText().toString().isEmpty()) {
                calendar = Calendar.getInstance();
            } else if (ingresoToEdit != null && ingresoToEdit.getFecha() != null) {
                calendar.setTime(ingresoToEdit.getFecha());
            } else if (!editTextFecha.getText().toString().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    calendar.setTime(sdf.parse(editTextFecha.getText().toString()));
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Formato de fecha inválido. Usando fecha actual.", Toast.LENGTH_SHORT).show();
                }
            }

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        editTextFecha.setText(sdf.format(selectedDate.getTime()));
                    }, year, month, day);
            datePickerDialog.show();
        });

        buttonSelectImage.setOnClickListener(v -> selectImageLauncher.launch("image/*"));

        buttonSave.setOnClickListener(v -> {
            String titulo = editTextTitulo.getText().toString().trim();
            String montoStr = editTextMonto.getText().toString().trim();
            String descripcion = editTextDescripcion.getText().toString().trim();
            String fechaStr = editTextFecha.getText().toString().trim();

            if (titulo.isEmpty() || montoStr.isEmpty() || fechaStr.isEmpty()) {
                Toast.makeText(getContext(), "Por favor, complete los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            double monto;
            try {
                monto = Double.parseDouble(montoStr);
                if (monto <= 0) {
                    Toast.makeText(getContext(), "El monto debe ser positivo", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "El monto debe ser un número válido (ej. 100.00)", Toast.LENGTH_SHORT).show();
                return;
            }

            Date fecha;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                fecha = sdf.parse(fechaStr);
            } catch (java.text.ParseException e) {
                Toast.makeText(getContext(), "Formato de fecha no válido", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser == null) {
                Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedImageUri != null) {
                Toast.makeText(getContext(), "Subiendo comprobante", Toast.LENGTH_SHORT).show();
                servicioAlmacenamiento.guardarArchivo(selectedImageUri, new ServicioAlmacenamiento.UploadResultListener() {
                    @Override
                    public void onSuccess(String url) {
                        Toast.makeText(getContext(), "Comprobante subido", Toast.LENGTH_SHORT).show();
                        saveOrUpdateIngreso(ingresoToEdit, titulo, monto, descripcion, fecha, url, dialog);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getContext(), "Error al subir comprobante", Toast.LENGTH_LONG).show();
                        saveOrUpdateIngreso(ingresoToEdit, titulo, monto, descripcion, fecha, (ingresoToEdit != null ? ingresoToEdit.getComprobanteUrl() : null), dialog);
                    }
                });
            } else {
                String existingImageUrl = (ingresoToEdit != null ? ingresoToEdit.getComprobanteUrl() : null);
                saveOrUpdateIngreso(ingresoToEdit, titulo, monto, descripcion, fecha, existingImageUrl, dialog);
            }
        });
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveOrUpdateIngreso(@Nullable Ingreso ingresoToEdit, String titulo, double monto, String descripcion, Date fecha, @Nullable String comprobanteUrl, AlertDialog dialog) {
        if (ingresoToEdit != null) {
            DocumentReference ingresoRef = db.collection("ingresos").document(ingresoToEdit.getId());
            ingresoRef.update("monto", monto, "descripcion", descripcion, "fecha", fecha, "comprobanteUrl", comprobanteUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Ingreso actualizado",Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadIngresos();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al actualizar ingreso",Toast.LENGTH_SHORT).show();
                    });
        } else {
            Ingreso nuevoIngreso = new Ingreso(currentUser.getUid(), titulo, monto, descripcion, fecha, comprobanteUrl);
            db.collection("ingresos")
                    .add(nuevoIngreso)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Ingreso agregado",Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadIngresos();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al agregar ingreso",Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onEditClick(String transactionId, Ingreso ingreso) {
        showAddEditTransactionDialog(ingreso);
    }

    @Override
    public void onDeleteClick(String transactionId, Ingreso ingreso) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("ingresos").document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Ingreso eliminado",Toast.LENGTH_SHORT).show();
                    loadIngresos();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al eliminar ingreso",Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDownloadClick(String comprobanteUrl, Ingreso ingreso) { // aqui cree una funcion del boton de descarga del comrobante
        if (comprobanteUrl != null && !comprobanteUrl.isEmpty()) { // para descargar la imagen desde la nube
            final String finalDownloadUrl; // para ello validamos si exite el URL
            if (comprobanteUrl.startsWith("http://")) { // aqui si ue ia porque algunas url se guardaban como HTTP
                finalDownloadUrl = comprobanteUrl.replace("http://", "https://"); // y como este dispostivo requiere HTTPS lo reemplazo para una mayor seguridad y para que sea visible el codigo
            } else { // en un inicio lo he probado con http pero no me funcionaba
                finalDownloadUrl = comprobanteUrl;
            }

            servicioAlmacenamiento.obtenerArchivo(finalDownloadUrl, new ServicioAlmacenamiento.DownloadResultListener() {
                @Override
                public void onSuccess(File file) { // de esta forma asegura la conexion y funcionamiento correcto de descarga
                    Toast.makeText(getContext(), "Comprobante descargado", Toast.LENGTH_LONG).show();
                    openFile(file); // una vez que fue extiosa , se llama el OpenFile para mostrar el selector de apps y abrir dicha imagen
                } // lo mismo va ser en egresos

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Error al descargar comprobante", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "No hay comprobante asociado", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile(File file) {
        if (!file.exists()) {
            Toast.makeText(getContext(), "El archivo descargado no existe", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String authority = getContext().getPackageName() + ".fileprovider";
            Uri fileUri = FileProvider.getUriForFile(getContext(), authority, file);
            String mimeType = getContext().getContentResolver().getType(fileUri);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType != null ? mimeType : "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "No hay aplicación para abrir este tipo de archivo", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalArgumentException e) {
            Toast.makeText(getContext(), "Error de configuración al abrir el archivo", Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No se encontró una aplicación para abrir el archivo", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error inesperado al intentar abrir el archivo", Toast.LENGTH_LONG).show();
        }
    }
}