package com.example.diplomwork;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diplomwork.databinding.FragmentCalculatorBinding;

public class CalculatorFragment extends Fragment {
    private FragmentCalculatorBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCalculatorBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Обработчик нажатия на кнопку "Кредитный калькулятор"
        binding.creditCalculatorButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), CreditCalculator.class);
            startActivity(intent);
        });

        // Обработчик нажатия на кнопку "Дебетовый калькулятор"
        binding.debitCalculatorButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), DebitCalculator.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очистка привязки при уничтожении представления
        binding = null;
    }
}
