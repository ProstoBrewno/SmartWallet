package com.example.diplomwork;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.example.diplomwork.databinding.ActivityDebitCalculatorBinding;

public class DebitCalculator extends AppCompatActivity {
    private ActivityDebitCalculatorBinding binding; // View Binding для связи с UI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация View Binding
        binding = ActivityDebitCalculatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Устанавливаем обработчики для кнопок
        setUpListeners();
    }

    private void setUpListeners() {
        // Обработчик нажатия на кнопку расчета вклада
        binding.buttonCalculate.setOnClickListener(v -> calculateDepositPayment());

        // Обработчик нажатия на кнопку "Назад", для закрытия активности
        binding.Back2.setOnClickListener(v -> finish());
    }

    // Метод для расчета итоговой суммы вклада и дохода
    private void calculateDepositPayment() {
        // Получаем данные из полей ввода
        String depositAmountString = binding.editTextInitialAmount.getText().toString();
        String interestRateString = binding.editTextInterestRate.getText().toString();
        String depositTermString = binding.editTextDuration.getText().toString();

        // Проверяем, чтобы все поля были заполнены
        if (depositAmountString.isEmpty() || interestRateString.isEmpty() || depositTermString.isEmpty()) {
            binding.textViewResult.setText("Все поля должны быть заполнены");
            return;
        }

        try {
            // Преобразуем строки в числовые значения
            double depositAmount = Double.parseDouble(depositAmountString);
            double interestRate = Double.parseDouble(interestRateString);
            int depositTerm = Integer.parseInt(depositTermString);

            // Проверка на положительные значения
            if (depositAmount <= 0 || interestRate <= 0 || depositTerm <= 0) {
                binding.textViewResult.setText("Все значения должны быть положительными");
                return;
            }

            // Формула для расчета итоговой суммы вклада
            double monthlyInterestRate = interestRate / 100; // ежемесячная ставка
            double totalAmount = depositAmount * Math.pow(1 + monthlyInterestRate, depositTerm); // общая сумма с учетом процентов
            double totalInterest = totalAmount - depositAmount; // общий доход от вклада

            // Отображаем результат на экране
            binding.textViewResult.setText(String.format("Общая сумма вклада: %.2f\nОбщий доход: %.2f", totalAmount, totalInterest));

        } catch (NumberFormatException e) {
            // В случае ошибок при преобразовании данных
            binding.textViewResult.setText("Введите корректные данные");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Освобождение ресурсов View Binding
    }
}
