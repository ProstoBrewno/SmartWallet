package com.example.diplomwork;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;

import com.example.diplomwork.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // Объявление переменных для View Binding, BottomNavigationView и текущего фрагмента
    private ActivityMainBinding binding;
    private BottomNavigationView bottomNavigationView;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Инициализация View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Получаем ссылку на BottomNavigationView через View Binding
        bottomNavigationView = binding.bottomNavigationView;

        // Устанавливаем слушатель для изменения выбранного элемента навигации
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

        // Загружаем начальный фрагмент (главную страницу)
        loadHomeFragment();
    }

    // Слушатель для изменения выбранного элемента в BottomNavigationView
    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    int itemId = item.getItemId();

                    // В зависимости от выбранного элемента BottomNavigationView загружаем нужный фрагмент
                    if (itemId == R.id.navigation_home) {
                        selectedFragment = new HomeFragment();
                    } else if (itemId == R.id.navigation_calculator) {
                        selectedFragment = new CalculatorFragment();
                    } else if (itemId == R.id.navigation_statistics) {
                        selectedFragment = new StockMarketFragment();
                    } else if (itemId == R.id.navigation_profile) {
                        selectedFragment = new ProfileFragment();
                    }

                    // Загружаем выбранный фрагмент
                    loadFragment(selectedFragment);
                    return true;
                }
            };

    // Метод для замены текущего фрагмента на новый
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment); // Заменяем текущий фрагмент
        transaction.addToBackStack(null); // Добавляем фрагмент в стек для возможности возврата
        bottomNavigationView.getMenu().getItem(0).setChecked(true); // Устанавливаем первый пункт меню в активное состояние
        transaction.commit(); // Выполняем транзакцию
    }

    // Метод для загрузки начального фрагмента (главной страницы)
    private void loadHomeFragment() {
        currentFragment = new HomeFragment(); // Создаем новый объект фрагмента
        loadFragment(currentFragment); // Загружаем этот фрагмент
    }
}
