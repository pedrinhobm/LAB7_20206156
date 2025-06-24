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

public class EgresosFragment extends Fragment implements TransactionAdapter.OnTransactionActionListener<Egreso> {
    private RecyclerView recyclerViewEgresos;
    private TransactionAdapter<Egreso> adapter;
    private List<Egreso> egresosList;
    private FloatingActionButton fabAddEgreso;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private Uri selectedImageUri;
    private ImageView imageViewComprobante;
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private static final String TAG = "EgresosFragment";

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


    public EgresosFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        egresosList = new ArrayList<>();

        if (getActivity() != null) {
            servicioAlmacenamiento = ((MyApplication) getActivity().getApplication()).getServicioAlmacenamiento();
            Log.d(TAG, "ServicioAlmacenamiento obtenido de MyApplication.");
        } else {
            Log.e(TAG, "Error: Actividad nula al intentar obtener ServicioAlmacenamiento. Inicializando localmente (fallback).");
            servicioAlmacenamiento = new ServicioAlmacenamiento(getContext());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_egresos, container, false);

        recyclerViewEgresos = view.findViewById(R.id.recyclerViewEgresos);
        recyclerViewEgresos.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter<>(getContext(), egresosList, false, this);
        recyclerViewEgresos.setAdapter(adapter);
        fabAddEgreso = view.findViewById(R.id.fabAddEgreso);
        fabAddEgreso.setOnClickListener(v -> showAddEditTransactionDialog(null));

        if (currentUser == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        } else {
            loadEgresos();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        return view;
    }

    private void loadEgresos() {
        if (currentUser == null)
            return;
        db.collection("egresos")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        egresosList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Egreso egreso = document.toObject(Egreso.class);
                            if (egreso != null) {
                                egreso.setId(document.getId());

                                String urlFromFirebase = egreso.getComprobanteUrl();
                                if (urlFromFirebase != null && urlFromFirebase.startsWith("http://")) {
                                    Log.w(TAG, "Firebase Egreso URL es HTTP, convirtiendo a HTTPS: " + urlFromFirebase);
                                    egreso.setComprobanteUrl(urlFromFirebase.replace("http://", "https://"));
                                } else {
                                    Log.d(TAG, "Firebase Egreso URL es ya HTTPS o nula: " + urlFromFirebase);
                                }
                                egresosList.add(egreso);
                            }
                        }
                        Collections.sort(egresosList, (e1, e2) -> {
                            if (e1.getFecha() == null || e2.getFecha() == null) return 0;
                            return e2.getFecha().compareTo(e1.getFecha());
                        });
                        adapter.setTransactionList(egresosList);
                    } else {
                        Toast.makeText(getContext(), "Error al cargar egresos: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading egresos", task.getException());
                    }
                });
    }

    private void showAddEditTransactionDialog(@Nullable Egreso egresoToEdit) {
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

        if (egresoToEdit != null) {
            dialogTitle.setText("Editar Egreso");
            editTextTitulo.setText(egresoToEdit.getTitulo());
            editTextMonto.setText(String.valueOf(egresoToEdit.getMonto()));
            editTextDescripcion.setText(egresoToEdit.getDescripcion());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (egresoToEdit.getFecha() != null) {
                editTextFecha.setText(sdf.format(egresoToEdit.getFecha()));
            }
            editTextTitulo.setEnabled(false);
            editTextFecha.setEnabled(false);

            if (egresoToEdit.getComprobanteUrl() != null && !egresoToEdit.getComprobanteUrl().isEmpty()) {
                Picasso.get()
                        .load(egresoToEdit.getComprobanteUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(imageViewComprobante);
                imageViewComprobante.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }

        } else {
            dialogTitle.setText("Nuevo Egreso");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            editTextFecha.setText(sdf.format(Calendar.getInstance().getTime()));
        }

        editTextFecha.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (egresoToEdit == null && editTextFecha.getText().toString().isEmpty()) {
                calendar = Calendar.getInstance();
            } else if (egresoToEdit != null && egresoToEdit.getFecha() != null) {
                calendar.setTime(egresoToEdit.getFecha());
            } else if (!editTextFecha.getText().toString().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    calendar.setTime(sdf.parse(editTextFecha.getText().toString()));
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Formato de fecha inválido. Usando fecha actual.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error parsing date: " + e.getMessage());
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
                Log.e(TAG, "Error parsing date: " + e.getMessage());
                return;
            }

            if (currentUser == null) {
                Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedImageUri != null) {
                Toast.makeText(getContext(), "Subiendo comprobante...", Toast.LENGTH_SHORT).show();
                servicioAlmacenamiento.guardarArchivo(selectedImageUri, new ServicioAlmacenamiento.UploadResultListener() {
                    @Override
                    public void onSuccess(String url) {
                        Toast.makeText(getContext(), "Comprobante subido.", Toast.LENGTH_SHORT).show();
                        saveOrUpdateEgreso(egresoToEdit, titulo, monto, descripcion, fecha, url, dialog);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getContext(), "Error al subir comprobante: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error uploading file: " + error);
                        saveOrUpdateEgreso(egresoToEdit, titulo, monto, descripcion, fecha, (egresoToEdit != null ? egresoToEdit.getComprobanteUrl() : null), dialog);
                    }
                });
            } else {
                String existingImageUrl = (egresoToEdit != null ? egresoToEdit.getComprobanteUrl() : null);
                saveOrUpdateEgreso(egresoToEdit, titulo, monto, descripcion, fecha, existingImageUrl, dialog);
            }
        });
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveOrUpdateEgreso(@Nullable Egreso egresoToEdit, String titulo, double monto, String descripcion, Date fecha, @Nullable String comprobanteUrl, AlertDialog dialog) {
        if (egresoToEdit != null) {
            DocumentReference egresoRef = db.collection("egresos").document(egresoToEdit.getId());
            egresoRef.update("monto", monto, "descripcion", descripcion, "fecha", fecha, "comprobanteUrl", comprobanteUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Egreso actualizado con éxito",Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadEgresos();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al actualizar egreso: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error updating egreso", e);
                    });
        } else {
            Egreso nuevoEgreso = new Egreso(currentUser.getUid(), titulo, monto, descripcion, fecha, comprobanteUrl);
            db.collection("egresos")
                    .add(nuevoEgreso)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Egreso agregado con éxito",Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadEgresos();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al agregar egreso: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error adding egreso", e);
                    });
        }
    }

    @Override
    public void onEditClick(String transactionId, Egreso egreso) {
        showAddEditTransactionDialog(egreso);
    }

    @Override
    public void onDeleteClick(String transactionId, Egreso egreso) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("egresos").document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Egreso eliminado con éxito",Toast.LENGTH_SHORT).show();
                    loadEgresos();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al eliminar egreso: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting egreso", e);
                });
    }

    @Override
    public void onDownloadClick(String comprobanteUrl, Egreso egreso) {
        if (comprobanteUrl != null && !comprobanteUrl.isEmpty()) {
            Log.d(TAG, "Intentando descargar URL de Firestore (en onDownloadClick): " + comprobanteUrl);

            final String finalDownloadUrl;
            if (comprobanteUrl.startsWith("http://")) {
                finalDownloadUrl = comprobanteUrl.replace("http://", "https://");
                Log.w(TAG, "Corrigiendo URL de descarga a HTTPS: " + finalDownloadUrl);
            } else {
                finalDownloadUrl = comprobanteUrl;
            }

            servicioAlmacenamiento.obtenerArchivo(finalDownloadUrl, new ServicioAlmacenamiento.DownloadResultListener() {
                @Override
                public void onSuccess(File file) {
                    Toast.makeText(getContext(), "Comprobante descargado: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    openFile(file);
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Error al descargar comprobante: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Descarga fallida para URL: " + finalDownloadUrl + " Error: " + error);
                }
            });
        } else {
            Toast.makeText(getContext(), "No hay comprobante asociado a esta transacción.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile(File file) {
        if (!file.exists()) {
            Toast.makeText(getContext(), "El archivo descargado no existe.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Intento de abrir archivo que no existe: " + file.getAbsolutePath());
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
                Toast.makeText(getContext(), "No hay aplicación para abrir este tipo de archivo.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "No hay aplicación disponible para abrir el archivo con MIME type: " + (mimeType != null ? mimeType : "*/*"));
            }
        } catch (IllegalArgumentException e) {
            Toast.makeText(getContext(), "Error de configuración al abrir el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error de FileProvider: " + e.getMessage(), e);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No se encontró una aplicación para abrir el archivo.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "ActivityNotFoundException al abrir el archivo: " + e.getMessage(), e);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error inesperado al intentar abrir el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error general al abrir el archivo: " + e.getMessage(), e);
        }
    }
}