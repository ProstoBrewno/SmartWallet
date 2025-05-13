package com.example.diplomwork;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diplomwork.databinding.FragmentStockMarketBinding;  // Импортируем сгенерированный класс Binding

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Фрагмент для отображения данных фондового рынка.
 * Использует API Twelve Data для получения котировок акций.
 */
public class StockMarketFragment extends Fragment {

    // Ключ API для доступа к данным Twelve Data
    private static final String API_KEY = "76fa9816a85e4506845d4206578a6423";
    // URL API для запроса котировок нескольких акций
    private static final String API_URL = "https://api.twelvedata.com/quote?symbol=AAPL,MSFT,GOOGL,TSLA,NVDA&apikey=" + API_KEY;
    // Имя SharedPreferences для кэширования данных
    private static final String PREFS_NAME = "StocksCache";
    // Интервал обновления данных (2 минуты)
    private static final long UPDATE_INTERVAL = 120_000;

    // View Binding для фрагмента
    private FragmentStockMarketBinding binding;
    // Символы акций для отслеживания
    private final String[] symbols = {"AAPL", "MSFT", "GOOGL", "TSLA", "NVDA"};

    // Поток для выполнения сетевых запросов
    private ExecutorService executorService;
    // Handler для обновления UI из фонового потока
    private Handler handler;
    // Handler для периодического обновления данных
    private Handler updateHandler;
    // Runnable для периодического обновления
    private Runnable updateRunnable;
    // Флаг видимости фрагмента пользователю
    private boolean isVisibleToUser = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Создаем однопоточный Executor для сетевых запросов
        executorService = Executors.newSingleThreadExecutor();
        // Handler для основного потока (UI)
        handler = new Handler(Looper.getMainLooper());
        // Handler для периодических обновлений
        updateHandler = new Handler();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Инициализация View Binding
        binding = FragmentStockMarketBinding.inflate(inflater, container, false);
        return binding.getRoot();  // Возвращаем корневой View
    }

    @Override
    public void onResume() {
        super.onResume();
        isVisibleToUser = true;

        // Загружаем кэшированные данные
        String cachedData = loadCachedData();
        if (cachedData != null) {
            // Отображаем кэшированные данные (без очистки существующих карточек)
            parseAndDisplayData(cachedData, false);
        }

        // Запускаем автоматическое обновление
        startAutoUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        isVisibleToUser = false;
        // Останавливаем автоматическое обновление
        stopAutoUpdates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Завершаем работу ExecutorService
        executorService.shutdown();
        // Удаляем все callback'и Handler'а
        updateHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Запускает периодическое обновление данных.
     */
    private void startAutoUpdates() {
        if (!isVisibleToUser) return;

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isVisibleToUser) {
                    // Получаем новые данные
                    fetchStockData();
                    // Планируем следующее обновление через UPDATE_INTERVAL
                    updateHandler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        };
        // Запускаем первое обновление
        updateHandler.post(updateRunnable);
    }

    /**
     * Останавливает периодическое обновление данных.
     */
    private void stopAutoUpdates() {
        updateHandler.removeCallbacks(updateRunnable);
    }

    /**
     * Получает данные об акциях с API.
     */
    private void fetchStockData() {
        executorService.execute(() -> {
            try {
                // Создаем HTTP-соединение
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Читаем ответ
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (response.length() > 0) {
                    // Сохраняем данные в кэш
                    saveDataToCache(response.toString());

                    // Обновляем UI только если фрагмент видим
                    if (isVisibleToUser && isAdded()) {
                        handler.post(() -> {
                            // Парсим и отображаем новые данные (с очисткой старых)
                            parseAndDisplayData(response.toString(), true);
                            binding.progressBar.setVisibility(View.GONE);  // Скрываем прогресс-бар
                        });
                    }
                }

            } catch (Exception e) {
                Log.e("StockMarketFragment", "Error fetching stock data", e);
            }
        });
    }

    /**
     * Парсит JSON-ответ и отображает данные.
     */
    private void parseAndDisplayData(String jsonResponse, boolean clearBefore) {
        try {
            if (clearBefore) {
                binding.stocksListLayout.removeAllViews();  // Очищаем список
            }

            if ("empty".equals(jsonResponse)) {
                // Если данные пустые, показываем прочерки
                for (String symbol : symbols) {
                    createStockCard(symbol, "—", "—");
                }
            } else {
                // Парсим JSON
                JSONObject responseObj = new JSONObject(jsonResponse);
                for (String symbol : symbols) {
                    if (responseObj.has(symbol)) {
                        JSONObject stock = responseObj.getJSONObject(symbol);
                        // Получаем данные об акции
                        String name = stock.optString("name", symbol);
                        String price = stock.optString("close", "—");
                        String change = stock.optString("percent_change", "—");

                        // Создаем карточку акции
                        createStockCard(name, price, change);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("StockMarketFragment", "Error parsing stock data", e);
            showError("Ошибка обработки данных");
        } finally {
            handler.post(() -> binding.progressBar.setVisibility(View.GONE));  // Скрываем прогресс-бар
        }
    }

    /**
     * Создает и добавляет карточку акции в список.
     */
    private void createStockCard(String name, String price, String change) {
        // Надуваем макет карточки
        View stockCard = LayoutInflater.from(getContext()).inflate(R.layout.stock_card, binding.stocksListLayout, false);
        TextView stockName = stockCard.findViewById(R.id.stockName);
        TextView stockPrice = stockCard.findViewById(R.id.stockPrice);
        TextView stockChange = stockCard.findViewById(R.id.stockChange);

        // Устанавливаем данные
        stockName.setText(name);
        stockPrice.setText("Цена: $" + price);
        stockChange.setText("Изменение: " + change + "%");

        // Устанавливаем цвет в зависимости от изменения цены
        if (!change.equals("—")) {
            try {
                float changeValue = Float.parseFloat(change);
                if (changeValue > 0) {
                    stockChange.setTextColor(getResources().getColor(R.color.green));
                } else if (changeValue < 0) {
                    stockChange.setTextColor(getResources().getColor(R.color.red));
                } else {
                    stockChange.setTextColor(getResources().getColor(R.color.gray));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Добавляем карточку в список
        binding.stocksListLayout.addView(stockCard);
    }

    /**
     * Сохраняет данные в SharedPreferences.
     * @param data данные для сохранения
     */
    private void saveDataToCache(String data) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString("stocks_data", data)
                .putLong("last_update", System.currentTimeMillis())
                .apply();
    }

    /**
     * Загружает кэшированные данные из SharedPreferences.
     * @return кэшированные данные или null
     */
    private String loadCachedData() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("stocks_data", null);
    }

    /**
     * Показывает Toast с сообщением об ошибке.
     * @param message текст сообщения
     */
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}