package com.example.diplomwork;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.diplomwork.databinding.ActivityAddIncomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddIncomeActivity extends AppCompatActivity {

    // Создаем объект binding для доступа к элементам UI
    private ActivityAddIncomeBinding binding;
    private final Calendar calendar = Calendar.getInstance();
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация binding и установка контента
        binding = ActivityAddIncomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Инициализация Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Устанавливаем текущую дату
        updateDateInView();

        // Устанавливаем обработчики для кнопок и поля даты
        binding.dateEditText.setOnClickListener(v -> showDatePickerDialog());
        binding.backButton.setOnClickListener(v -> finish());
        binding.saveButton.setOnClickListener(v -> saveIncome());
    }

    // Метод для обновления поля даты
    private void updateDateInView() {
        String myFormat = "dd.MM.yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        binding.dateEditText.setText(sdf.format(calendar.getTime()));
    }

    // Метод для отображения диалога выбора даты
    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            updateDateInView();
        };

        new DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // Метод для сохранения дохода в Firebase
    private void saveIncome() {
        String amountStr = binding.amountEditText.getText().toString().trim();
        String description = binding.descriptionEditText.getText().toString().trim();
        String date = binding.dateEditText.getText().toString().trim();

        // Проверка на пустые поля
        if (amountStr.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String incomeId = mDatabase.child("incomes").push().getKey();

        // Сбор данных о доходе
        Map<String, Object> incomeData = new HashMap<>();
        incomeData.put("amount", amount);
        incomeData.put("description", description);
        incomeData.put("date", date);
        incomeData.put("userId", userId);

        // Сохранение данных о доходе в Firebase
        mDatabase.child("incomes").child(incomeId).setValue(incomeData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateUserBalance(amount); // Обновляем баланс пользователя
                    } else {
                        Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Метод для обновления баланса пользователя в Firebase
    private void updateUserBalance(double amount) {
        mDatabase.child("Users").child(userId).child("check")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Получаем текущий баланс
                        Double currentBalance = task.getResult().getValue(Double.class);
                        if (currentBalance == null) currentBalance = 0.0;

                        // Новый баланс после добавления дохода
                        double newBalance = currentBalance + amount;

                        // Обновляем баланс пользователя
                        mDatabase.child("Users").child(userId).child("check")
                                .setValue(newBalance)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(this, "Доход добавлен!", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Ошибка обновления баланса", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Ошибка получения баланса", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
