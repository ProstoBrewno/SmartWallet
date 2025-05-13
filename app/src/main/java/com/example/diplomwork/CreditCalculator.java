package com.example.diplomwork;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import com.example.diplomwork.databinding.ActivityCreditCalculatorBinding;

public class CreditCalculator extends AppCompatActivity {
    private ActivityCreditCalculatorBinding binding; // View Binding для связывания UI элементов

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация View Binding
        binding = ActivityCreditCalculatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Устанавливаем обработчики для кнопок
        setUpListeners();
    }

    private void setUpListeners() {
        // Обработчик нажатия на кнопку расчета
        binding.buttonCalculate.setOnClickListener(v -> calculateLoanPayment());

        // Обработчик нажатия на кнопку "Назад", для закрытия активности
        binding.Back.setOnClickListener(v -> finish());
    }

    // Метод для расчета ежемесячного платежа по кредиту
    private void calculateLoanPayment() {
        // Получение введенных данных с проверкой на пустые значения
        String loanAmountString = binding.editTextLoanAmount.getText().toString();
        String interestRateString = binding.editTextInterestRate.getText().toString();
        String loanTermString = binding.editTextLoanTerm.getText().toString();

        // Проверка, чтобы все поля были заполнены
        if (loanAmountString.isEmpty() || interestRateString.isEmpty() || loanTermString.isEmpty()) {
            Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Преобразуем строки в числовые значения
            double loanAmount = Double.parseDouble(loanAmountString);
            double interestRate = Double.parseDouble(interestRateString);
            int loanTerm = Integer.parseInt(loanTermString);

            // Проверка на положительные значения
            if (loanAmount <= 0 || interestRate <= 0 || loanTerm <= 0) {
                Toast.makeText(this, "Все значения должны быть положительными", Toast.LENGTH_SHORT).show();
                return;
            }

            // Формула для расчета ежемесячного платежа
            double monthlyInterestRate = interestRate / 100 / 12; // ежемесячная ставка
            double numberOfPayments = loanTerm * 12; // общее количество платежей
            double monthlyPayment = (loanAmount * monthlyInterestRate) / (1 - Math.pow(1 + monthlyInterestRate, -numberOfPayments));

            // Отображение результата на экране
            binding.textViewResult.setText(String.format("Ежемесячный платеж: %.2f", monthlyPayment));

        } catch (NumberFormatException e) {
            // В случае ошибок при преобразовании данных
            Toast.makeText(this, "Введите корректные данные", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Освобождение ресурсов View Binding
    }
}
