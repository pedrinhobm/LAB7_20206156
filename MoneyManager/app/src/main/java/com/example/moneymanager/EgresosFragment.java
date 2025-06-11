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

public class EgresosFragment extends Fragment implements TransactionAdapter.OnTransactionActionListener<Egreso> {

    private RecyclerView recyclerViewEgresos;
    private TransactionAdapter<Egreso> adapter;
    private List<Egreso> egresosList;
    private FloatingActionButton fabAddEgreso;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private static final String TAG = "EgresosFragment";

    public EgresosFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        egresosList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_egresos, container, false);

        recyclerViewEgresos = view.findViewById(R.id.recyclerViewEgresos);
        recyclerViewEgresos.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter<>(getContext(), egresosList, false, this); // 'false' para egresos
        recyclerViewEgresos.setAdapter(adapter);

        fabAddEgreso = view.findViewById(R.id.fabAddEgreso);
        fabAddEgreso.setOnClickListener(v -> showAddEditTransactionDialog(null));

        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
        } else {
            loadEgresos();
        }

        return view;
    }

    private void loadEgresos() {
        if (currentUser == null) return;

        db.collection("egresos")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        egresosList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Egreso egreso = document.toObject(Egreso.class);
                            egreso.setId(document.getId());
                            egresosList.add(egreso);
                        }
                        Collections.sort(egresosList, (e1, e2) -> {
                            if (e1.getFecha() == null || e2.getFecha() == null) return 0;
                            return e2.getFecha().compareTo(e1.getFecha());
                        });
                        adapter.setTransactionList(egresosList);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(getContext(), "Error al cargar egresos: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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

            if (egresoToEdit != null) {
                DocumentReference egresoRef = db.collection("egresos").document(egresoToEdit.getId());
                egresoRef.update(
                                "monto", monto,
                                "descripcion", descripcion,
                                "fecha", fecha)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Egreso actualizado con éxito.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadEgresos();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating egreso", e);
                            Toast.makeText(getContext(), "Error al actualizar egreso: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Egreso nuevoEgreso = new Egreso(currentUser.getUid(), titulo, monto, descripcion, fecha);
                db.collection("egresos")
                        .add(nuevoEgreso)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(getContext(), "Egreso agregado con éxito.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadEgresos();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error adding new egreso", e);
                            Toast.makeText(getContext(), "Error al agregar egreso: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onEditClick(String transactionId, Egreso egreso) {
        showAddEditTransactionDialog(egreso);
    }

    @Override
    public void onDeleteClick(String transactionId, Egreso egreso) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("egresos").document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Egreso eliminado con éxito.", Toast.LENGTH_SHORT).show();
                    loadEgresos();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting egreso", e);
                    Toast.makeText(getContext(), "Error al eliminar egreso: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}