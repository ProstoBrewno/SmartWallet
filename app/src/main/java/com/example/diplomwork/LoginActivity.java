package com.example.diplomwork;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.diplomwork.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    // Объявляем переменные для View Binding и FirebaseAuth
    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;

    /**
     * Метод для проверки доступности интернета.
     * @param context контекст приложения
     * @return true, если интернет доступен, иначе false
     */
    public static boolean isInternetAvailable(Context context) {
        // Получаем доступ к системному сервису для работы с сетевыми подключениями
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected(); // Проверяем подключение к сети
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Инициализация View Binding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Инициализация Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Обработчик для кнопки "Войти"
        binding.buttonLogin.setOnClickListener(v -> {
            // Получаем введенные пользователем данные
            String email = binding.editTextUsername.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString().trim();

            // Проверка, что поля не пустые
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверка наличия интернета перед выполнением входа
            if (isInternetAvailable(this)) {
                // Выполнение аутентификации через Firebase
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Если аутентификация прошла успешно, переходим на главную страницу
                        if (task.isSuccessful()) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish(); // Закрываем текущую активность
                        } else {
                            // В случае ошибки, показываем сообщение об ошибке
                            Toast.makeText(LoginActivity.this, "Ошибка входа, зарегестрируйтесь!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                // Если интернета нет, показываем сообщение
                Toast.makeText(LoginActivity.this, "Отсутствует подключение к интернету", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчик для кнопки "Зарегистрироваться"
        binding.buttonRegister.setOnClickListener(v -> {
            // Переходим на экран регистрации
            startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
            finish(); // Закрываем текущую активность
        });
    }
}
