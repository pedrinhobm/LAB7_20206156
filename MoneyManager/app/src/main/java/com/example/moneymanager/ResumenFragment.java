package com.example.moneymanager; // Asegúrate que el package sea el correcto

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

// Elimina estas importaciones de MPAndroidChart si no las usas
// import com.github.mikephil.charting.charts.BarChart;
// import com.github.mikephil.charting.charts.PieChart;
// import com.github.mikephil.charting.components.Legend;
// import com.github.mikephil.charting.components.XAxis;
// import com.github.mikephil.charting.data.BarData;
// import com.github.mikephil.charting.data.BarDataSet;
// import com.github.mikephil.charting.data.BarEntry;
// import com.github.mikephil.charting.data.PieData;
// import com.github.mikephil.charting.data.PieDataSet;
// import com.github.mikephil.charting.data.PieEntry;
// import com.github.mikephil.charting.formatter.PercentFormatter;
// import com.github.mikephil.charting.utils.ColorTemplate;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog; // Este sí lo mantenemos

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResumenFragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    private CustomPieChartView pieChart; // ¡CAMBIO!
    private CustomBarChartView barChart; // ¡CAMBIO!
    private TextView textViewSelectedMonth, textViewTotalIngresos, textViewTotalEgresos, textViewConsolidado;
    private ImageButton buttonPreviousMonth, buttonNextMonth;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private Calendar currentMonthCalendar;
    private static final String TAG = "ResumenFragment";

    public ResumenFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        currentMonthCalendar = Calendar.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resumen, container, false);

        pieChart = view.findViewById(R.id.pieChart); // ¡CAMBIO!
        barChart = view.findViewById(R.id.barChart); // ¡CAMBIO!
        textViewSelectedMonth = view.findViewById(R.id.textViewSelectedMonth);
        textViewTotalIngresos = view.findViewById(R.id.textViewTotalIngresos);
        textViewTotalEgresos = view.findViewById(R.id.textViewTotalEgresos);
        textViewConsolidado = view.findViewById(R.id.textViewConsolidado);
        buttonPreviousMonth = view.findViewById(R.id.buttonPreviousMonth);
        buttonNextMonth = view.findViewById(R.id.buttonNextMonth);

        setupMonthPicker();
        // setupCharts(); // ¡Ya no necesitamos esta configuración específica de MPAndroidChart!
        loadDataForCharts();

        return view;
    }

    private void setupMonthPicker() {
        updateMonthDisplay();

        textViewSelectedMonth.setOnClickListener(v -> {
            DatePickerDialog dpd = DatePickerDialog.newInstance(
                    this,
                    currentMonthCalendar.get(Calendar.YEAR),
                    currentMonthCalendar.get(Calendar.MONTH),
                    currentMonthCalendar.get(Calendar.DAY_OF_MONTH)
            );
            dpd.show(getParentFragmentManager(), "Datepickerdialog");
        });

        buttonPreviousMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadDataForCharts();
        });

        buttonNextMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadDataForCharts();
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()); // Corregido el formato para mostrar "yyyy"
        textViewSelectedMonth.setText(sdf.format(currentMonthCalendar.getTime()));
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        currentMonthCalendar.set(Calendar.YEAR, year);
        currentMonthCalendar.set(Calendar.MONTH, monthOfYear);
        currentMonthCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth); // El día no importa para el resumen mensual, pero lo configuramos
        updateMonthDisplay();
        loadDataForCharts();
    }

    // Ya no necesitamos setupCharts() tal como estaba para MPAndroidChart
    // private void setupCharts() { /* ... */ }

    private void loadDataForCharts() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar startMonth = (Calendar) currentMonthCalendar.clone();
        startMonth.set(Calendar.DAY_OF_MONTH, 1);
        startMonth.set(Calendar.HOUR_OF_DAY, 0);
        startMonth.set(Calendar.MINUTE, 0);
        startMonth.set(Calendar.SECOND, 0);
        startMonth.set(Calendar.MILLISECOND, 0);

        Calendar endMonth = (Calendar) currentMonthCalendar.clone();
        endMonth.set(Calendar.DAY_OF_MONTH, endMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endMonth.set(Calendar.HOUR_OF_DAY, 23);
        endMonth.set(Calendar.MINUTE, 59);
        endMonth.set(Calendar.SECOND, 59);
        endMonth.set(Calendar.MILLISECOND, 999);

        Date startDate = startMonth.getTime();
        Date endDate = endMonth.getTime();

        Task<QuerySnapshot> ingresosTask = db.collection("ingresos")
                .whereEqualTo("userId", currentUser.getUid())
                .whereGreaterThanOrEqualTo("fecha", startDate)
                .whereLessThanOrEqualTo("fecha", endDate)
                .get();

        Task<QuerySnapshot> egresosTask = db.collection("egresos")
                .whereEqualTo("userId", currentUser.getUid())
                .whereGreaterThanOrEqualTo("fecha", startDate)
                .whereLessThanOrEqualTo("fecha", endDate)
                .get();

        Tasks.whenAllSuccess(ingresosTask, egresosTask)
                .addOnSuccessListener(list -> {
                    double totalIngresos = 0;
                    for (QueryDocumentSnapshot document : (QuerySnapshot) list.get(0)) {
                        Ingreso ingreso = document.toObject(Ingreso.class);
                        totalIngresos += ingreso.getMonto();
                    }

                    double totalEgresos = 0;
                    for (QueryDocumentSnapshot document : (QuerySnapshot) list.get(1)) {
                        Egreso egreso = document.toObject(Egreso.class);
                        totalEgresos += egreso.getMonto();
                    }

                    updateUI(totalIngresos, totalEgresos);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading data for charts", e);
                    Toast.makeText(getContext(), "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(double totalIngresos, double totalEgresos) {
        DecimalFormat df = new DecimalFormat("'S/.' #,##0.00");

        textViewTotalIngresos.setText("Total Ingresos: " + df.format(totalIngresos));
        textViewTotalEgresos.setText("Total Egresos: " + df.format(totalEgresos));

        double consolidado = totalIngresos - totalEgresos;
        textViewConsolidado.setText("Consolidado: " + df.format(consolidado));
        if (consolidado >= 0) {
            textViewConsolidado.setTextColor(getContext().getResources().getColor(android.R.color.holo_green_dark));
        } else {
            textViewConsolidado.setTextColor(getContext().getResources().getColor(android.R.color.holo_red_dark));
        }


        // Actualizar Pie Chart (usando tu CustomPieChartView)
        pieChart.setData((float) totalIngresos, (float) totalEgresos);


        // Actualizar Bar Chart (usando tu CustomBarChartView)
        barChart.setData(totalIngresos, totalEgresos);
    }
}