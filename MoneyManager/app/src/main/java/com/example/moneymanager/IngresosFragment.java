package com.example.moneymanager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

    public IngresosFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance(); // para los ingresos realizamos la autenticacion
        mAuth = FirebaseAuth.getInstance(); // y la recolección con la base de datos en el Firebase
        currentUser = mAuth.getCurrentUser(); // lo cual lo guardamos en una lista
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

        if (currentUser == null) { // primero validamos que la cuenta fue autenticada
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        } else {
            loadIngresos(); // si funciona seguimos la sigueinte función
        }
        return view;
    }

    private void loadIngresos() {
        if (currentUser == null)
            return; // de esa coleccion buscamos la fecha, el id con un QuerySnapshot
        db.collection("ingresos")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ingresosList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Ingreso ingreso = document.toObject(Ingreso.class);
                            ingreso.setId(document.getId());
                            ingresosList.add(ingreso);
                        }
                        Collections.sort(ingresosList, (i1, i2) -> {
                            if (i1.getFecha() == null || i2.getFecha() == null) return 0;
                            return i2.getFecha().compareTo(i1.getFecha());
                        });
                        adapter.setTransactionList(ingresosList);
                    } else {
                        Toast.makeText(getContext(), "Error al cargar ingresos",Toast.LENGTH_SHORT).show();
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

        if (ingresoToEdit != null) { // aqui realizamos las funciones de los CRUDS
            dialogTitle.setText("Editar Ingreso"); // para editar
            editTextTitulo.setText(ingresoToEdit.getTitulo()); // lo realizamos para el titulo, monto, descripcion
            editTextMonto.setText(String.valueOf(ingresoToEdit.getMonto())); // y la fecha
            editTextDescripcion.setText(ingresoToEdit.getDescripcion());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (ingresoToEdit.getFecha() != null) {
                editTextFecha.setText(sdf.format(ingresoToEdit.getFecha()));
            }
            editTextTitulo.setEnabled(false);
            editTextFecha.setEnabled(false);
        } else {
            dialogTitle.setText("Nuevo Ingreso"); // lo mismo sucedera en caso deseamos agregar un nuevo ingreso
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            editTextFecha.setText(sdf.format(Calendar.getInstance().getTime())); // para que tengos la facilidiad de editar
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
                }
            }

            int year = calendar.get(Calendar.YEAR); // para las fechas si lo saque de las funciones
            int month = calendar.get(Calendar.MONTH); // de los anteriores laboratorios con el DatePickerDialog
            int day = calendar.get(Calendar.DAY_OF_MONTH);  // que selecciona desde un calendario la fecha exacta

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        editTextFecha.setText(sdf.format(selectedDate.getTime()));
                    }, year, month, day);
            datePickerDialog.show();
        });


        buttonSave.setOnClickListener(v -> { // este es el boton para guardar
            String titulo = editTextTitulo.getText().toString().trim();
            String montoStr = editTextMonto.getText().toString().trim();
            String descripcion = editTextDescripcion.getText().toString().trim();
            String fechaStr = editTextFecha.getText().toString().trim();

            if (titulo.isEmpty() || montoStr.isEmpty() || fechaStr.isEmpty()) { // verifica que no esté vacio sino te alerta que deben completarse
                Toast.makeText(getContext(), "Por favor, complete los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            double monto;
            try { // el caso del monto tambien le añador Toast para indicar
                monto = Double.parseDouble(montoStr); // cuando debe ser un número positivo
                if (monto <= 0) { // o en el otro caso que debe ir con decimales , sabemos que tambien valen los centimos
                    Toast.makeText(getContext(), "El monto debe ser positivo", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "El monto debe ser con decimales", Toast.LENGTH_SHORT).show();
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

            if (ingresoToEdit != null) { // al momento de editar se actualiza
                DocumentReference ingresoRef = db.collection("ingresos").document(ingresoToEdit.getId()); // y eso tambien llega a editarse en la base de datos
                ingresoRef.update("monto", monto, "descripcion", descripcion, "fecha", fecha) // para cada uno de los campos
                        .addOnSuccessListener(aVoid -> { // por eso envias un Toast de edición exitosa
                            Toast.makeText(getContext(), "Ingreso actualizado con éxito",Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadIngresos();
                        })
                        .addOnFailureListener(e -> { // en caso de error, lo va a indicar
                            Toast.makeText(getContext(), "Error al actualizar ingreso",Toast.LENGTH_SHORT).show();
                        });
            } else {
                Ingreso nuevoIngreso = new Ingreso(currentUser.getUid(), titulo, monto, descripcion, fecha);  // lo mismo hacemos agregando ingresos
                db.collection("ingresos")
                        .add(nuevoIngreso)
                        .addOnSuccessListener(documentReference -> { // a traves de la colección dentro de la base de datos
                            Toast.makeText(getContext(), "Ingreso agregado con éxito",Toast.LENGTH_SHORT).show(); // tambien con un Toast de haber añadido correctamente
                            dialog.dismiss();
                            loadIngresos();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error al agregar ingreso",Toast.LENGTH_SHORT).show();
                        });
            }
        }); // si cancelamos con el botón, se pierde lo que quisiste guardar
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onEditClick(String transactionId, Ingreso ingreso) {
        showAddEditTransactionDialog(ingreso);
    }

    @Override
    public void onDeleteClick(String transactionId, Ingreso ingreso) { // aqui está la acción CRUD de eliminar
        if (currentUser == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        } // con eso lo registramos en la colección de la base de datos para borrarlo de ahí

        db.collection("ingresos").document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> { // el toast tambien indica que se elimino el ingreso
                    Toast.makeText(getContext(), "Ingreso eliminado con éxito",Toast.LENGTH_SHORT).show();
                    loadIngresos();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al eliminar ingreso",Toast.LENGTH_SHORT).show();
                });
    }
}