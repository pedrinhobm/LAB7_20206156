package com.example.moneymanager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {
    private EditText editTextRegisterEmail;
    private EditText editTextRegisterPassword;
    private EditText editTextConfirmPassword;
    private Button buttonRegister;
    private TextView textViewBackToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) { // este es el registro de una cuenta
        super.onCreate(savedInstanceState); // yo lo hice con una cuenta alterna al personal
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance(); // iniciamos la autenticacion con el firebase
        editTextRegisterEmail = findViewById(R.id.editTextRegisterEmail);
        editTextRegisterPassword = findViewById(R.id.editTextRegisterPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewBackToLogin = findViewById(R.id.textViewBackToLogin);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // para el registro completamos 3 campos, email , contraseña y su confirmación
                String email = editTextRegisterEmail.getText().toString().trim();
                String password = editTextRegisterPassword.getText().toString().trim();
                String confirmPassword = editTextConfirmPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) { // en caso al menos uno esté incompleto
                    Toast.makeText(RegisterActivity.this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
                } else if (!password.equals(confirmPassword)) { // si no concidien
                    Toast.makeText(RegisterActivity.this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                } else if (password.length() < 6) { // otra nota a agregar es que el firebase requiere mínimo 6 caracteres, por eso lo agregué dicha selectiva
                    Toast.makeText(RegisterActivity.this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                }
                else {
                    mAuth.createUserWithEmailAndPassword(email, password) // realizamos la autenticacion
                            .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) { // cuando logra funcionar la autenticacion ...
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        Toast.makeText(RegisterActivity.this, "Registrado exitoso", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class); // el intent es cuando logro registrarme bien y me lleva a la vista del resumen del dashboard
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Registro fallido", Toast.LENGTH_LONG).show(); // en caso no funciona, indicamos con un toast que falló
                                    }
                                }
                            });
                }
            }
        });

        textViewBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override // esto es para retornar a la vista anterior que es del login con el que ya tienes cuenta y
            public void onClick(View v) { // deseas iniciar sesion
                finish();
            }
        });
    }
}
