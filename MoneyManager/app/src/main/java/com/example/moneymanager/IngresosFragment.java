package com.example.moneymanager;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IngresosFragment extends Fragment implements TransactionAdapter.OnTransactionActionListener<Ingreso> {

    private RecyclerView recyclerViewIngresos;
    private TransactionAdapter<Ingreso> adapter;
    private List<Ingreso> ingresosList;
    private FloatingActionButton fabAddIngreso;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private static final String TAG = "IngresosFragment";

    public IngresosFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        ingresosList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ingresos, container, false);

        recyclerViewIngresos = view.findViewById(R.id.recyclerViewIngresos);
        recyclerViewIngresos.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter<>(getContext(), ingresosList, true, this); // 'true' para ingresos
        recyclerViewIngresos.setAdapter(adapter);

        fabAddIngreso = view.findViewById(R.id.fabAddIngreso);
        fabAddIngreso.setOnClickListener(v -> showAddEditTransactionDialog(null));

        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            // Considerar redirigir al login si no hay usuario
        } else {
            loadIngresos();
        }

        return view;
    }

    private void loadIngresos() {
        if (currentUser == null) return;

        db.collection("ingresos")
                .whereEqualTo("userId", currentUser.getUid()) // Filtrar por el ID del usuario actual
                .orderBy("fecha", Query.Direction.DESCENDING) // Ordenar por fecha, más reciente primero
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ingresosList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Ingreso ingreso = document.toObject(Ingreso.class);
                            ingreso.setId(document.getId()); // Guardar el ID del documento de Firestore
                            ingresosList.add(ingreso);
                        }
                        // Opcional: ordenar nuevamente por fecha si Firebase no lo garantiza perfectamente por el tipo Date
                        Collections.sort(ingresosList, (i1, i2) -> {
                            if (i1.getFecha() == null || i2.getFecha() == null) return 0;
                            return i2.getFecha().compareTo(i1.getFecha());
                        });
                        adapter.setTransactionList(ingresosList);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(getContext(), "Error al cargar ingresos: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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

        AlertDialog dialog = builder.create();

        // Inicializar campos si se está editando
        if (ingresoToEdit != null) {
            dialogTitle.setText("Editar Ingreso");
            editTextTitulo.setText(ingresoToEdit.getTitulo());
            editTextMonto.setText(String.valueOf(ingresoToEdit.getMonto()));
            editTextDescripcion.setText(ingresoToEdit.getDescripcion());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (ingresoToEdit.getFecha() != null) {
                editTextFecha.setText(sdf.format(ingresoToEdit.getFecha()));
            }
            editTextTitulo.setEnabled(false); // No se puede editar el título al editar un ingreso/egreso
            editTextFecha.setEnabled(false); // No se puede editar la fecha al editar un ingreso/egreso
        } else {
            dialogTitle.setText("Nuevo Ingreso");
            // Establecer la fecha actual por defecto para nuevos registros
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            editTextFecha.setText(sdf.format(Calendar.getInstance().getTime()));
        }

        // Configurar DatePicker para el campo de fecha
        editTextFecha.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (ingresoToEdit == null && editTextFecha.getText().toString().isEmpty()) {
                // Si es un nuevo ingreso y la fecha está vacía, usa la fecha actual
                calendar = Calendar.getInstance();
            } else if (ingresoToEdit != null && ingresoToEdit.getFecha() != null) {
                // Si se está editando, usa la fecha del ingreso existente
                calendar.setTime(ingresoToEdit.getFecha());
            } else if (!editTextFecha.getText().toString().isEmpty()) {
                // Si la fecha ya está en el campo (por ejemplo, fecha actual en nuevo registro)
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    calendar.setTime(sdf.parse(editTextFecha.getText().toString()));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing date for DatePicker", e);
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


        buttonSave.setOnClickListener(v -> {
            String titulo = editTextTitulo.getText().toString().trim();
            String montoStr = editTextMonto.getText().toString().trim();
            String descripcion = editTextDescripcion.getText().toString().trim();
            String fechaStr = editTextFecha.getText().toString().trim();

            if (titulo.isEmpty() || montoStr.isEmpty() || fechaStr.isEmpty()) {
                Toast.makeText(getContext(), "Por favor, complete los campos obligatorios (Título, Monto, Fecha).", Toast.LENGTH_SHORT).show();
                return;
            }

            double monto;
            try {
                monto = Double.parseDouble(montoStr);
                if (monto <= 0) {
                    Toast.makeText(getContext(), "El monto debe ser un número positivo.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Monto no válido. Use números decimales.", Toast.LENGTH_SHORT).show();
                return;
            }

            Date fecha;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                fecha = sdf.parse(fechaStr);
            } catch (java.text.ParseException e) {
                Toast.makeText(getContext(), "Formato de fecha no válido. Use DD/MM/AAAA.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser == null) {
                Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Si estamos editando
            if (ingresoToEdit != null) {
                DocumentReference ingresoRef = db.collection("ingresos").document(ingresoToEdit.getId());
                ingresoRef.update(
                                "monto", monto,
                                "descripcion", descripcion,
                                "fecha", fecha) // Aunque la fecha no se edite, se actualiza para asegurar consistencia
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Ingreso actualizado con éxito.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadIngresos(); // Recargar la lista
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating ingreso", e);
                            Toast.makeText(getContext(), "Error al actualizar ingreso: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else { // Si estamos agregando un nuevo ingreso
                Ingreso nuevoIngreso = new Ingreso(currentUser.getUid(), titulo, monto, descripcion, fecha);
                db.collection("ingresos")
                        .add(nuevoIngreso)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(getContext(), "Ingreso agregado con éxito.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadIngresos(); // Recargar la lista
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error adding new ingreso", e);
                            Toast.makeText(getContext(), "Error al agregar ingreso: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onEditClick(String transactionId, Ingreso ingreso) {
        showAddEditTransactionDialog(ingreso);
    }

    @Override
    public void onDeleteClick(String transactionId, Ingreso ingreso) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("ingresos").document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Ingreso eliminado con éxito.", Toast.LENGTH_SHORT).show();
                    loadIngresos(); // Recargar la lista
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting ingreso", e);
                    Toast.makeText(getContext(), "Error al eliminar ingreso: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}