package com.example.diplomwork;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.diplomwork.databinding.ActivityAddExpensesBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class AddExpensesActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    // Объявление переменных для доступа к UI элементам через ViewBinding
    private ActivityAddExpensesBinding binding;
    private Uri imageUri;
    private Calendar calendar = Calendar.getInstance();
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация binding для использования в activity
        binding = ActivityAddExpensesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Получение текущего пользователя Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) userId = currentUser.getUid();

        // Инициализация базы данных и хранилища Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();

        // Устанавливаем дату по умолчанию
        updateDateInView();

        // Обработчик для открытия DatePicker
        binding.dateEditText.setOnClickListener(v -> showDatePickerDialog());

        // Обработчик для кнопки возврата
        binding.backButton.setOnClickListener(v -> finish());

        // Обработчик для кнопки загрузки фото
        binding.uploadPhotoButton.setOnClickListener(v -> openFileChooser());

        // Обработчик для кнопки сохранения расходов
        binding.saveButton.setOnClickListener(v -> saveExpense());
    }

    // Обновляем текстовое поле с датой
    private void updateDateInView() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        binding.dateEditText.setText(sdf.format(calendar.getTime()));
    }

    // Показываем диалог выбора даты
    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            updateDateInView();
        };

        new DatePickerDialog(this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // Открываем файловый менеджер для выбора фото
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Обработка выбора изображения из файлового менеджера
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            binding.receiptImageView.setImageURI(imageUri);
        }
    }

    // Сохранение расхода
    private void saveExpense() {
        String amountStr = binding.amountEditText.getText().toString().trim();
        String description = binding.descriptionEditText.getText().toString().trim();
        String date = binding.dateEditText.getText().toString().trim();

        if (amountStr.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String expenseId = mDatabase.child("expenses").push().getKey();

        if (imageUri != null) {
            StorageReference fileRef = mStorage.child("receipts").child(userId).child(expenseId + ".jpg");
            fileRef.putFile(imageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return fileRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(uri -> uploadExpense(expenseId, amount, description, date, uri.toString()))
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show());
        } else {
            uploadExpense(expenseId, amount, description, date, null);
        }
    }

    // Загрузка данных о расходах в Firebase
    private void uploadExpense(String expenseId, double amount, String description, String date, String photoUrl) {
        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("amount", amount);
        expenseData.put("description", description);
        expenseData.put("date", date);
        expenseData.put("userId", userId);
        if (photoUrl != null) expenseData.put("receiptUrl", photoUrl);

        mDatabase.child("expenses").child(expenseId).setValue(expenseData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateUserBalance(-amount); // вычитаем сумму из баланса
                    } else {
                        Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Обновление баланса пользователя
    private void updateUserBalance(double amount) {
        mDatabase.child("Users").child(userId).child("check")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Double currentBalance = task.getResult().getValue(Double.class);
                        if (currentBalance == null) currentBalance = 0.0;

                        double newBalance = currentBalance + amount;

                        mDatabase.child("Users").child(userId).child("check")
                                .setValue(newBalance)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(this, "Расход добавлен!", Toast.LENGTH_SHORT).show();
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
