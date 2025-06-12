package com.example.moneymanager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
public class TransactionAdapter<T> extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<T> transactionList;
    private Context context;
    private boolean isIngreso; // este boolean sirve para determinar si es un ingresos, gresos
    private OnTransactionActionListener<T> listener; // aqui si usa IA porque esta variable servirá para acciones de editar y eliminar
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public TransactionAdapter(Context context, List<T> transactionList, boolean isIngreso, OnTransactionActionListener<T> listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.isIngreso = isIngreso;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    public void setTransactionList(List<T> transactionList) {
        this.transactionList = transactionList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ingreso_egreso, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        T transaction = transactionList.get(position);

        String titulo;
        double monto;
        String descripcion;
        java.util.Date fecha;
        String transactionId;

        if (isIngreso) { // con el mismo booleano recolectamos los datos de cada uno , lo mismo con egreso
            Ingreso ingreso = (Ingreso) transaction; // el titulo, monto numérico, la descripcion, fecha y el id
            titulo = ingreso.getTitulo();
            monto = ingreso.getMonto();
            descripcion = ingreso.getDescripcion();
            fecha = ingreso.getFecha();
            transactionId = ingreso.getId();
        } else {
            Egreso egreso = (Egreso) transaction;
            titulo = egreso.getTitulo();
            monto = egreso.getMonto();
            descripcion = egreso.getDescripcion();
            fecha = egreso.getFecha();
            transactionId = egreso.getId();
        }

        holder.textViewTitulo.setText(titulo);


        String montoFormateado = String.format(Locale.getDefault(), "S/. %.2f", monto);
        if (isIngreso) { // aqui formateo los ingresos con el booleano
            holder.textViewMonto.setText("+ " + montoFormateado);
            holder.textViewMonto.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else { // aqui formateo los egresos
            holder.textViewMonto.setText("- " + montoFormateado);
            holder.textViewMonto.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        }

        if (fecha != null) { // este es el caso de la fecha que será día, mes y año
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.textViewFecha.setText(sdf.format(fecha));
        } else {
            holder.textViewFecha.setText("");
        }

        holder.textViewDescripcion.setText(descripcion);

        holder.buttonEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(transactionId, transaction);
            }
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) {
                showDeleteConfirmationDialog(transactionId, transaction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    private void showDeleteConfirmationDialog(String transactionId, T transaction) {
        new AlertDialog.Builder(context) // la accion de eliminar tambien contiene
                .setTitle("Confirmar Eliminación") // la alerta que confirme si eliminamos ingreso o egreso
                .setMessage("¿Estás seguro de que quieres eliminar este elemento?")
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onDeleteClick(transactionId, transaction);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitulo, textViewMonto, textViewFecha, textViewDescripcion;
        ImageButton buttonEdit, buttonDelete;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitulo = itemView.findViewById(R.id.textViewTitulo);
            textViewMonto = itemView.findViewById(R.id.textViewMonto);
            textViewFecha = itemView.findViewById(R.id.textViewFecha);
            textViewDescripcion = itemView.findViewById(R.id.textViewDescripcion);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
    public interface OnTransactionActionListener<T> { // aqui realizamos CRUDs de editar y eliminar
        void onEditClick(String transactionId, T transaction);
        void onDeleteClick(String transactionId, T transaction);
    }
}