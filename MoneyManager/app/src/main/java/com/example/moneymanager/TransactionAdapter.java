package com.example.moneymanager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
    private boolean isIngreso; // true si es para Ingresos, false para Egresos
    private OnTransactionActionListener<T> listener; // Interfaz para acciones de editar/eliminar

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

        if (isIngreso) {
            Ingreso ingreso = (Ingreso) transaction;
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

        // Formatear el monto con signo y color
        String montoFormateado = String.format(Locale.getDefault(), "S/. %.2f", monto);
        if (isIngreso) {
            holder.textViewMonto.setText("+ " + montoFormateado);
            holder.textViewMonto.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.textViewMonto.setText("- " + montoFormateado);
            holder.textViewMonto.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        }

        // Formatear la fecha
        if (fecha != null) {
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
        new AlertDialog.Builder(context)
                .setTitle("Confirmar Eliminación")
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

    // Interfaz para que el Fragment pueda manejar las acciones de editar/eliminar
    public interface OnTransactionActionListener<T> {
        void onEditClick(String transactionId, T transaction);
        void onDeleteClick(String transactionId, T transaction);
    }
}