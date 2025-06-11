package com.example.moneymanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

        // Cargar el fragmento de ingresos por defecto al iniciar
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new IngresosFragment()).commit();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    int itemId = item.getItemId(); // Obtener el ID del item seleccionado

                    if (itemId == R.id.nav_ingresos) {
                        selectedFragment = new IngresosFragment();
                    } else if (itemId == R.id.nav_egresos) {
                        selectedFragment = new EgresosFragment();
                    } else if (itemId == R.id.nav_resumen) {
                        selectedFragment = new ResumenFragment();
                    } else if (itemId == R.id.nav_logout) {
                        // Cerrar sesión
                        mAuth.signOut();
                        Toast.makeText(DashboardActivity.this, "Sesión cerrada.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish(); // Cerrar DashboardActivity
                        return true; // No cargar un fragmento, solo cerrar sesión
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                                selectedFragment).commit();
                    }
                    return true;
                }
            };

    @Override
    public void onBackPressed() {
        // Deshabilitar el botón de retroceso para evitar que el usuario regrese a la pantalla de login sin cerrar sesión
        Toast.makeText(this, "Por favor, utiliza la opción 'Cerrar Sesión'.", Toast.LENGTH_SHORT).show();
        // Si realmente quieres permitir el comportamiento normal, puedes quitar esta línea:
        // super.onBackPressed();
    }
}