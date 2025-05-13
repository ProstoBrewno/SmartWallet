package com.example.diplomwork;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diplomwork.databinding.FragmentHomeBinding; // Импортируем ViewBinding для фрагмента
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class HomeFragment extends Fragment {

    // Переменные для ViewBinding
    private FragmentHomeBinding binding;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private LinearLayout operationsContainer;

    private String selectedDate;
    private Calendar calendar;

    // Форматы для даты
    private final SimpleDateFormat firebaseFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Используем ViewBinding вместо findViewById
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Инициализация Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        }

        // Инициализация календаря для работы с датами
        calendar = Calendar.getInstance();
        setCurrentDate();

        // Обработчики кнопок для доходов и расходов
        binding.incomeButton.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddIncomeActivity.class)));
        binding.expenseButton.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddExpensesActivity.class)));

        // Обработчик выбора даты
        binding.selectDateButton.setOnClickListener(v -> showDatePickerDialog());
        binding.todayButton.setOnClickListener(v -> setCurrentDate());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (firebaseAuth.getCurrentUser() != null) {
            loadBalanceData();
            if (selectedDate != null) {
                loadOperationsForDate(selectedDate);
            }
        }
    }

    // Показать диалог выбора даты
    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    Date selected = calendar.getTime();
                    selectedDate = firebaseFormat.format(selected);
                    binding.textView6.setText(displayFormat.format(selected));
                    loadOperationsForDate(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // Загрузка данных баланса пользователя
    private void loadBalanceData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer balance = snapshot.child("check").getValue(Integer.class);
                    if (balance != null) {
                        String formattedBalance = String.format(Locale.getDefault(), "%,d р.", balance);
                        binding.textView5.setText(formattedBalance);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.textView5.setText("Ошибка загрузки");
            }
        });
    }

    // Устанавливаем текущую дату
    private void setCurrentDate() {
        Date today = new Date();
        selectedDate = firebaseFormat.format(today);
        binding.textView6.setText("Сегодня, " + displayFormat.format(today));
        loadOperationsForDate(selectedDate);
    }

    // Загружаем операции для выбранной даты
    private void loadOperationsForDate(String date) {
        if (date == null || date.isEmpty()) return;

        operationsContainer = binding.operationsContainer; // Используем binding для получения контейнера

        operationsContainer.removeAllViews();

        if (firebaseAuth.getCurrentUser() == null) return;

        String userId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference incomesRef = FirebaseDatabase.getInstance().getReference("incomes");
        DatabaseReference expensesRef = FirebaseDatabase.getInstance().getReference("expenses");

        // Загрузка доходов
        incomesRef.orderByChild("date").equalTo(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot incomeSnapshot : snapshot.getChildren()) {
                    HashMap<String, Object> income = (HashMap<String, Object>) incomeSnapshot.getValue();
                    if (income != null && userId.equals(income.get("userId"))) {
                        addOperationView(income, true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Загрузка расходов
        expensesRef.orderByChild("date").equalTo(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    HashMap<String, Object> expense = (HashMap<String, Object>) expenseSnapshot.getValue();
                    if (expense != null && userId.equals(expense.get("userId"))) {
                        addOperationView(expense, false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Добавляем операцию в контейнер
    private void addOperationView(HashMap<String, Object> operation, boolean isIncome) {
        View operationView = LayoutInflater.from(getContext()).inflate(R.layout.item_operation, operationsContainer, false);

        TextView descriptionView = operationView.findViewById(R.id.operationDescription);
        TextView amountView = operationView.findViewById(R.id.operationAmount);

        String description = (String) operation.get("description");
        Number amountNum = (Number) operation.get("amount");
        long amount = amountNum != null ? amountNum.longValue() : 0;

        descriptionView.setText(description);
        if (isIncome) {
            amountView.setText("+" + amount + " р.");
            amountView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            amountView.setText("-" + amount + " р.");
            amountView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        operationsContainer.addView(operationView);
    }
}
