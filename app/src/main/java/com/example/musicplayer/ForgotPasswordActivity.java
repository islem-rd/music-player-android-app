package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etResetEmail;
    private Button btnReset;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvLogin = findViewById(R.id.tvLogin);
        etResetEmail = findViewById(R.id.etResetEmail);
        btnReset = findViewById(R.id.btnReset);
        mAuth = FirebaseAuth.getInstance();
        btnBack.setOnClickListener(v -> {
            onBackPressed(); // Retour à l'activité précédente
        });
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Optionnel : ferme l'activité actuelle
        });
        btnReset.setOnClickListener(v -> {
            String email = etResetEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etResetEmail.setError("Email requis");
                etResetEmail.requestFocus();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Lien de réinitialisation envoyé à votre email.", Toast.LENGTH_LONG).show();
                            finish(); // Return to login
                        } else {
                            Toast.makeText(this, "Erreur : " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
