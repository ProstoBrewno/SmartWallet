package com.example.diplomwork;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import com.example.diplomwork.databinding.ActivityPersonalDataBinding;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class PersonalData extends AppCompatActivity {

    // ViewBinding для доступа к элементам интерфейса
    private ActivityPersonalDataBinding binding;

    // Переменные для хранения данных пользователя
    private String name;
    private String email;
    private String password;

    // Флаг видимости пароля
    private boolean isPasswordVisible = false;

    // Firebase компоненты
    private FirebaseAuth firebaseAuth;
    private FirebaseStorage storage;
    private DatabaseReference ref;
    private StorageReference storageReference;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация ViewBinding
        binding = ActivityPersonalDataBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Инициализация Firebase компонентов
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Проверка авторизации пользователя
        if (currentUser == null) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Получение ссылки на данные пользователя в базе данных
        ref = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());

        // Загрузка данных пользователя
        loadUserData();

        // Настройка кнопок редактирования данных
        binding.button6.setOnClickListener(v -> showEditField(binding.nameLayout, name)); // Редактирование имени
        binding.button7.setOnClickListener(v -> showEditField(binding.emailLayout, email)); // Редактирование email
        binding.button8.setOnClickListener(v -> showEditField(binding.passwordLayout, password)); // Редактирование пароля

        // Переключение видимости пароля
        binding.passwordVisibilityButton.setOnClickListener(v -> togglePasswordVisibility(binding.passwordLayout));

        // Обработчик кнопки "Назад"
        binding.Back3.setOnClickListener(v -> handleBackPressed());
    }

    // Загружает данные пользователя из Firebase Database
    private void loadUserData() {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Получение данных из базы данных
                    name = snapshot.child("name").getValue(String.class);
                    email = snapshot.child("email").getValue(String.class);
                    password = snapshot.child("password").getValue(String.class); // Заполнитель для пароля

                    // Установка значений в текстовые поля
                    binding.nameLayout.getEditText().setText(name);
                    binding.emailLayout.getEditText().setText(email);
                    binding.passwordLayout.getEditText().setText(password);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PersonalData.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Показывает поле для редактирования выбранного значения
    private void showEditField(TextInputLayout inputLayout, String currentValue) {
        // Показываем затемнение и контейнер для редактирования
        binding.dimView.setVisibility(View.VISIBLE);
        binding.editFieldContainer.setVisibility(View.VISIBLE);
        binding.editText.setText(currentValue);

        // Устанавливаем соответствующий тип ввода
        if (inputLayout == binding.passwordLayout) {
            binding.editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else if (inputLayout == binding.emailLayout) {
            binding.editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        } else {
            binding.editText.setInputType(InputType.TYPE_CLASS_TEXT);
        }

        // Отключаем кнопки во время редактирования
        disableButtons();

        // Обработчик сохранения изменений
        binding.saveButton.setOnClickListener(v -> {
            String newValue = binding.editText.getText().toString();
            if (isInputValid(inputLayout, newValue)) {
                updateFieldValue(inputLayout, newValue);
                binding.editFieldContainer.setVisibility(View.GONE);
                binding.dimView.setVisibility(View.GONE);
                enableButtons();
            }
        });
    }

    // Проверяет введенные данные на валидность
    private boolean isInputValid(TextInputLayout inputLayout, String newValue) {
        if (inputLayout == binding.nameLayout && newValue.isEmpty()) {
            Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show();
            return false;
        } else if (inputLayout == binding.emailLayout && !Patterns.EMAIL_ADDRESS.matcher(newValue).matches()) {
            Toast.makeText(this, "Неверный формат email", Toast.LENGTH_SHORT).show();
            return false;
        } else if (inputLayout == binding.passwordLayout && newValue.length() < 4) {
            Toast.makeText(this, "Пароль должен содержать не менее 4 символов", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Обновляет значение в базе данных и интерфейсе
    private void updateFieldValue(TextInputLayout inputLayout, String newValue) {
        if (inputLayout == binding.nameLayout) {
            if (newValue.equals(name)) {
                Toast.makeText(this, "Новый имя совпадает с текущим", Toast.LENGTH_SHORT).show();
                return;
            }
            ref.child("name").setValue(newValue)
                    .addOnSuccessListener(aVoid -> {
                        name = newValue;
                        binding.nameLayout.getEditText().setText(newValue);
                        Toast.makeText(this, "Имя успешно обновлено", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка обновления имени", Toast.LENGTH_SHORT).show());
        } else if (inputLayout == binding.emailLayout) {
            if (newValue.equals(email)) {
                Toast.makeText(this, "Новый email совпадает с текущим", Toast.LENGTH_SHORT).show();
                return;
            }
            currentUser.updateEmail(newValue)
                    .addOnSuccessListener(aVoid -> ref.child("email").setValue(newValue)
                            .addOnSuccessListener(aVoid1 -> {
                                email = newValue;
                                binding.emailLayout.getEditText().setText(newValue);
                                Toast.makeText(this, "Email успешно обновлен", Toast.LENGTH_SHORT).show();
                            }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка обновления email", Toast.LENGTH_SHORT).show());
        } else if (inputLayout == binding.passwordLayout) {
            if (newValue.equals(password)) {
                Toast.makeText(this, "Новый пароль совпадает с текущим", Toast.LENGTH_SHORT).show();
                return;
            }
            currentUser.updatePassword(newValue)
                    .addOnSuccessListener(aVoid -> {
                        ref.child("password").setValue(newValue);
                        password = newValue;
                        binding.passwordLayout.getEditText().setText(password);
                        Toast.makeText(this, "Пароль успешно обновлен", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка обновления пароля", Toast.LENGTH_SHORT).show());
        }
    }

    // Переключает видимость пароля
    private void togglePasswordVisibility(TextInputLayout passwordLayout) {
        if (isPasswordVisible) {
            passwordLayout.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
            binding.passwordVisibilityButton.setImageResource(android.R.drawable.ic_menu_view);
            binding.passwordVisibilityButton.setContentDescription("Показать пароль");
        } else {
            passwordLayout.getEditText().setTransformationMethod(null);
            binding.passwordVisibilityButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            binding.passwordVisibilityButton.setContentDescription("Скрыть пароль");
        }
        isPasswordVisible = !isPasswordVisible;
    }

    // Отключает все кнопки (во время редактирования)
    private void disableButtons() {
        binding.button6.setEnabled(false);
        binding.button7.setEnabled(false);
        binding.button8.setEnabled(false);
        binding.passwordVisibilityButton.setEnabled(false);
        binding.Back3.setEnabled(false);
    }

    // Включает все кнопки (после завершения редактирования)
    private void enableButtons() {
        binding.button6.setEnabled(true);
        binding.button7.setEnabled(true);
        binding.button8.setEnabled(true);
        binding.passwordVisibilityButton.setEnabled(true);
        binding.Back3.setEnabled(true);
    }

    // Обрабатывает нажатие кнопки "Назад"
    private void handleBackPressed() {
        if (binding.editFieldContainer.getVisibility() == View.VISIBLE) {
            binding.editFieldContainer.setVisibility(View.GONE);
            binding.dimView.setVisibility(View.GONE);
            enableButtons();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Очистка binding для предотвращения утечек памяти
        binding = null;
    }
}
