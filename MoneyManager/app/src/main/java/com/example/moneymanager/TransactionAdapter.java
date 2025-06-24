package com.example.moneymanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter<T> extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final Context context;
    private List<T> transactionList;
    private final boolean isIngreso;
    private final OnTransactionActionListener<T> listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnTransactionActionListener<T> {
        void onEditClick(String transactionId, T transaction);
        void onDeleteClick(String transactionId, T transaction);
        // CAMBIO CRUCIAL AQUÍ: El primer parámetro DEBE ser la URL
        void onDownloadClick(String comprobanteUrl, T transaction);
    }

    public TransactionAdapter(Context context, List<T> transactionList, boolean isIngreso, OnTransactionActionListener<T> listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.isIngreso = isIngreso;
        this.listener = listener;
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

        String id;
        String titulo;
        double monto;
        String descripcion;
        String fecha;
        String comprobanteUrl = null; // Correcto: se inicializa a null, se le asigna un valor más abajo

        if (isIngreso) {
            Ingreso ingreso = (Ingreso) transaction;
            id = ingreso.getId();
            titulo = ingreso.getTitulo();
            monto = ingreso.getMonto();
            descripcion = ingreso.getDescripcion();
            fecha = (ingreso.getFecha() != null) ? dateFormat.format(ingreso.getFecha()) : "N/A";
            comprobanteUrl = ingreso.getComprobanteUrl();
        } else {
            Egreso egreso = (Egreso) transaction;
            id = egreso.getId();
            titulo = egreso.getTitulo();
            monto = egreso.getMonto();
            descripcion = egreso.getDescripcion();
            fecha = (egreso.getFecha() != null) ? dateFormat.format(egreso.getFecha()) : "N/A";
            comprobanteUrl = egreso.getComprobanteUrl();
        }

        holder.tituloTextView.setText(titulo);
        holder.montoTextView.setText(String.format(Locale.getDefault(), "S/.%.2f", monto));
        holder.descripcionTextView.setText(descripcion);
        holder.fechaTextView.setText(fecha);

        if (isIngreso) {
            holder.montoTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
        } else {
            holder.montoTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        }

        // Asegúrate de que comprobanteUrl se maneja correctamente aquí
        if (comprobanteUrl != null && !comprobanteUrl.isEmpty()) {
            holder.imageViewComprobanteItem.setVisibility(View.VISIBLE);
            holder.buttonDownload.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(comprobanteUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(holder.imageViewComprobanteItem);
        } else {
            holder.imageViewComprobanteItem.setVisibility(View.GONE);
            holder.imageViewComprobanteItem.setImageDrawable(null);
            holder.buttonDownload.setVisibility(View.GONE);
        }

        // Asegúrate de que 'id' es final o efectivamente final si se usa en el listener de borrado o edición
        final String finalId = id;

        // CAMBIO AQUÍ: Pasa la comprobanteUrl real al listener de descarga
        final String finalComprobanteUrl = comprobanteUrl; // Hazla final para usarla en el lambda
        holder.editButton.setOnClickListener(v -> listener.onEditClick(finalId, transaction));
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(finalId, transaction));
        holder.buttonDownload.setOnClickListener(v -> listener.onDownloadClick(finalComprobanteUrl, transaction)); // <-- ¡CORREGIDO!
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tituloTextView;
        TextView montoTextView;
        TextView descripcionTextView;
        TextView fechaTextView;
        ImageView imageViewComprobanteItem;
        Button editButton;
        Button deleteButton;
        Button buttonDownload;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tituloTextView = itemView.findViewById(R.id.textViewTitulo);
            montoTextView = itemView.findViewById(R.id.textViewMonto);
            descripcionTextView = itemView.findViewById(R.id.textViewDescripcion);
            fechaTextView = itemView.findViewById(R.id.textViewFecha);
            imageViewComprobanteItem = itemView.findViewById(R.id.imageViewComprobanteItem);
            editButton = itemView.findViewById(R.id.buttonEdit);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
            buttonDownload = itemView.findViewById(R.id.buttonDownload);
        }
    }
}