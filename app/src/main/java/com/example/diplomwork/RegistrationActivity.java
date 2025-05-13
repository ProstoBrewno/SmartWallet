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

import com.example.diplomwork.databinding.ActivityRegistrationBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {

    // Объявляем переменные для View Binding, FirebaseAuth и DatabaseReference
    private ActivityRegistrationBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference ref;

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
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Инициализация Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance().getReference("Users");

        // Обработчик нажатия кнопки "Зарегистрироваться"
        binding.registerButton.setOnClickListener(v -> {
            // Получаем введенные данные от пользователя
            String name = binding.nameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();
            int check = 0; // Изначально сумма на аккаунте равна 0

            // Проверка на пустые поля
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(RegistrationActivity.this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверка на совпадение паролей
            if (!password.equals(confirmPassword)) {
                Toast.makeText(RegistrationActivity.this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверка подключения к интернету
            if (isInternetAvailable(this)) {
                // Регистрация нового пользователя через Firebase
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        // Получаем уникальный идентификатор пользователя
                        String userId = firebaseAuth.getCurrentUser().getUid();

                        // Создаем структуру данных для сохранения в Firebase
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("password", password);
                        userData.put("check", check); // Изначальное значение счета

                        // Сохраняем данные в Firebase Realtime Database
                        ref.child(userId).setValue(userData).addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                // Если данные сохранены успешно, переходим в основное приложение
                                Toast.makeText(RegistrationActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                                finish(); // Закрываем текущую активность
                            } else {
                                // В случае ошибки сохранения данных
                                Toast.makeText(RegistrationActivity.this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        // В случае ошибки регистрации пользователя
                        Toast.makeText(RegistrationActivity.this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Если нет интернета, показываем сообщение об ошибке
                Toast.makeText(RegistrationActivity.this, "Отсутствует подключение к интернету", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчик нажатия кнопки "Назад"
        binding.backButton.setOnClickListener(v -> {
            // Переход к экрану входа
            startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
            finish(); // Закрываем текущую активность
        });
    }
}
