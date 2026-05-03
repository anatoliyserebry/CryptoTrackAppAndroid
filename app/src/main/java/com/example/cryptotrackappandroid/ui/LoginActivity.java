package com.example.cryptotrackappandroid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cryptotrackappandroid.BuildConfig;
import com.example.cryptotrackappandroid.R;
import com.example.cryptotrackappandroid.data.ApiClient;
import com.example.cryptotrackappandroid.data.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.loginProgress);
        TextView serverUrlText = findViewById(R.id.serverUrlText);
        serverUrlText.setText("FastAPI: " + BuildConfig.API_BASE_URL);

        loginButton.setOnClickListener(v -> login());
    }

    private void login() {
        String email = emailInput.getText() != null ? emailInput.getText().toString() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";
        setLoading(true);

        apiClient.login(email, password, new com.example.cryptotrackappandroid.data.ApiCallback<String>() {
            @Override
            public void onSuccess(String token) {
                setLoading(false);
                sessionManager.saveToken(token);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(Exception error) {
                setLoading(false);
                Snackbar.make(loginButton, "Enter an email and password", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
    }
}
