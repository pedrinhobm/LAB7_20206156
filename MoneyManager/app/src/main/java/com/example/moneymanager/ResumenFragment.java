package com.example.moneymanager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ResumenFragment extends Fragment implements DatePickerDialog.OnDateSetListener {
    private CustomPieChartView pieChart; // para el resumen si usa IA porque queria guiarme como
    private CustomBarChartView barChart; // podia hacer cada uno de los graficos solicitados que es una barras y pastel
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

        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        textViewSelectedMonth = view.findViewById(R.id.textViewSelectedMonth);
        textViewTotalIngresos = view.findViewById(R.id.textViewTotalIngresos);
        textViewTotalEgresos = view.findViewById(R.id.textViewTotalEgresos);
        textViewConsolidado = view.findViewById(R.id.textViewConsolidado); // es la acumulacion de ingresos y egresos
        buttonPreviousMonth = view.findViewById(R.id.buttonPreviousMonth); // estos son los botones para cambiar de mes
        buttonNextMonth = view.findViewById(R.id.buttonNextMonth);
        setupMonthPicker();
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
            updateMonthDisplay(); // el boton para retroceder meses y cambiar los charts
            loadDataForCharts();
        });

        buttonNextMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay(); // el boton para aumentar meses y cambiar los charts
            loadDataForCharts();
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        textViewSelectedMonth.setText(sdf.format(currentMonthCalendar.getTime()));
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        currentMonthCalendar.set(Calendar.YEAR, year);
        currentMonthCalendar.set(Calendar.MONTH, monthOfYear);
        currentMonthCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateMonthDisplay();
        loadDataForCharts();
    }

    private void loadDataForCharts() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        } // para cargar los datos de los graficos del dashboard
        // lo distribuimos desde desde el inicio a fin de mes
        Calendar startMonth = (Calendar) currentMonthCalendar.clone();
        startMonth.set(Calendar.DAY_OF_MONTH, 1);
        startMonth.set(Calendar.HOUR_OF_DAY, 0);
        startMonth.set(Calendar.MINUTE, 0);
        startMonth.set(Calendar.SECOND, 0);
        startMonth.set(Calendar.MILLISECOND, 0);

        Calendar endMonth = (Calendar) currentMonthCalendar.clone(); // para ello lo seleccionamos del mes escogido del calendario
        endMonth.set(Calendar.DAY_OF_MONTH, endMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endMonth.set(Calendar.HOUR_OF_DAY, 23);
        endMonth.set(Calendar.MINUTE, 59);
        endMonth.set(Calendar.SECOND, 59);
        endMonth.set(Calendar.MILLISECOND, 999);

        Date startDate = startMonth.getTime();
        Date endDate = endMonth.getTime();

        // aqui usa IA para obtener los datos que se obtienen de la base de datos que son ingresos y egresos
        // para cada uno obtenemos por el Id, las fechas de inicio y fin
        // en este caso lo realizmaos por un QuerySnapshot y Task que llevan a la bd
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

        Tasks.whenAllSuccess(ingresosTask, egresosTask) // lo mismo aqui use para la cantidad total de ingresos y egresos
                .addOnSuccessListener(list -> {
                    double totalIngresos = 0; // en cada uno realizamos una for que jalara los querys
                    for (QueryDocumentSnapshot document : (QuerySnapshot) list.get(0)) { // para que sume el monto
                        Ingreso ingreso = document.toObject(Ingreso.class); // desde la clase de Ingreso
                        totalIngresos += ingreso.getMonto();
                    }
                    double totalEgresos = 0; // de igual manera aqui pasa al caso de egresos
                    for (QueryDocumentSnapshot document : (QuerySnapshot) list.get(1)) { // lo sumamos con un for de iterativa
                        Egreso egreso = document.toObject(Egreso.class); // pero desde  la clase de Egreso
                        totalEgresos += egreso.getMonto();
                    }
                    updateUI(totalIngresos, totalEgresos);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(double totalIngresos, double totalEgresos) {
        DecimalFormat df = new DecimalFormat("'S/.' #,##0.00"); // establecemos el formato decimal total acumulado
        textViewTotalIngresos.setText("Total Ingresos: " + df.format(totalIngresos));
        textViewTotalEgresos.setText("Total Egresos: " + df.format(totalEgresos));
        double consolidado = totalIngresos - totalEgresos; // realizo la resta de ambos parámetros
        textViewConsolidado.setText("Consolidado: " + df.format(consolidado));
        if (consolidado >= 0) { // para el consolidado use colores para distinguir
            textViewConsolidado.setTextColor(getContext().getResources().getColor(android.R.color.holo_green_dark));
        } else { // si aun quedan utilidades de verde o perdidas de rojo
            textViewConsolidado.setTextColor(getContext().getResources().getColor(android.R.color.holo_red_dark));
        }


        // Actualizar Pie Chart (usando tu CustomPieChartView)
        pieChart.setData((float) totalIngresos, (float) totalEgresos);


        // Actualizar Bar Chart (usando tu CustomBarChartView)
        barChart.setData(totalIngresos, totalEgresos);
    }
}