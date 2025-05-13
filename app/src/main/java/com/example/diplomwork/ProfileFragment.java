package com.example.diplomwork;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diplomwork.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

import java.io.*;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding; // View Binding для фрагмента
    private FirebaseAuth firebaseAuth; // Авторизация Firebase
    private FirebaseStorage storage; // Хранилище Firebase
    private DatabaseReference ref; // Ссылка на данные в Realtime Database
    private StorageReference storageReference; // Ссылка на хранилище
    private ValueEventListener profileListener; // Слушатель изменений данных
    private Uri filepath; // Путь к выбранному изображению
    private String email = "", name = "";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final long MAX_IMAGE_SIZE = 1024 * 1024; // 1MB

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Инициализация View Binding
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Инициализация Firebase
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        // Проверка авторизации пользователя
        if (currentUser == null) {
            Toast.makeText(getActivity(), "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Получаем ссылку на данные текущего пользователя в базе
        ref = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());

        // Показ прогрессбара при загрузке данных
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.textView2.setVisibility(View.INVISIBLE);
        binding.textView4.setVisibility(View.INVISIBLE);

        // Обработчики кнопок
        binding.btnPersonalInfo.setOnClickListener(v -> goToPersanalInfo());
        binding.btnLogout.setOnClickListener(v -> logOut());
        binding.button.setOnClickListener(v -> deleteProfileData());
        binding.imageView2.setOnClickListener(v -> selectImage());

        // Загрузка данных из локального кэша
        loadUserInfoFromCache();
        Bitmap cachedBitmap = loadProfileImageFromCache();
        if (cachedBitmap != null) {
            binding.imageView2.setImageBitmap(getCircularBitmap(cachedBitmap));
            binding.progressBar.setVisibility(View.GONE);
        }

        // Слушатель изменений профиля пользователя в Realtime Database
        profileListener = ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    name = snapshot.child("name").getValue(String.class);
                    email = snapshot.child("email").getValue(String.class);

                    // Обновление UI
                    if (email != null) {
                        binding.textView4.setText(email);
                        binding.textView4.setVisibility(View.VISIBLE);
                    }
                    if (name != null) {
                        binding.textView2.setText(name);
                        binding.textView2.setVisibility(View.VISIBLE);
                    }

                    // Сохраняем данные в SharedPreferences
                    saveUserInfoToCache(name, email);

                    // Загрузка изображения профиля
                    loadProfileImage(currentUser.getUid(), new BitmapResultCallback() {
                        @Override
                        public void onBitmapReady(Bitmap bitmap) {
                            if (bitmap != null) {
                                binding.imageView2.setImageBitmap(getCircularBitmap(bitmap));
                                saveProfileImageToCache(bitmap);
                            }
                            binding.progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("ProfileFragment", "Error loading profile image", e);
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), "Ошибка: данные пользователя отсутствуют", Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Ошибка загрузки данных: " + error.getMessage(), Toast.LENGTH_LONG).show();
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        return view;
    }

    // Интерфейс для обработки результатов загрузки изображения
    public interface BitmapResultCallback {
        void onBitmapReady(Bitmap bitmap);
        void onError(Exception e);
    }

    // Загрузка изображения профиля из Firebase Storage
    private void loadProfileImage(String userId, BitmapResultCallback callback) {
        StorageReference profileImageRef = storageReference.child("profile_images/" + userId + ".jpg");

        profileImageRef.getBytes(MAX_IMAGE_SIZE)
                .addOnSuccessListener(bytes -> {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    callback.onBitmapReady(bitmap);
                    saveProfileImageToCache(bitmap);
                })
                .addOnFailureListener(e -> {
                    Bitmap cachedBitmap = loadProfileImageFromCache();
                    if (cachedBitmap != null) {
                        callback.onBitmapReady(cachedBitmap);
                    } else {
                        Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_profile);
                        callback.onBitmapReady(defaultBitmap);
                    }
                });
    }

    // Преобразование изображения в круглое
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int min = Math.min(width, height);
        Bitmap output = Bitmap.createBitmap(min, min, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setAntiAlias(true);
        canvas.drawCircle(min / 2f, min / 2f, min / 2f, paint);
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, (min - width) / 2f, (min - height) / 2f, paint);
        return output;
    }

    // Удаление данных профиля из базы, хранилища и учетной записи
    private void deleteProfileData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getActivity(), "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setTitle("Удаление аккаунта...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        StorageReference imageRef = FirebaseStorage.getInstance().getReference("profile_images/" + userId + ".jpg");

        imageRef.delete().addOnCompleteListener(task1 -> {
            userRef.removeValue().addOnCompleteListener(task2 -> {
                currentUser.delete().addOnCompleteListener(task3 -> {
                    progressDialog.dismiss();
                    if (task3.isSuccessful()) {
                        Toast.makeText(getActivity(), "Аккаунт успешно удален", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                        requireActivity().finish();
                    } else {
                        Toast.makeText(getActivity(), "Ошибка удаления аккаунта: " + task3.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getActivity(), "Ошибка: повторная авторизация перед удалением", Toast.LENGTH_LONG).show();
                });
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(getActivity(), "Ошибка удаления данных: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(getActivity(), "Ошибка удаления изображения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    // Выбор изображения из галереи
    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    // Выход из аккаунта
    private void logOut() {
        clearUserCache();
        firebaseAuth.signOut();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        requireActivity().finish();
    }

    // Очистка кэшированных данных пользователя
    private void clearUserCache() {
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("user_cache", 0);
            prefs.edit().clear().apply();
        }
    }

    // Переход на экран личной информации
    private void goToPersanalInfo() {
        startActivity(new Intent(getActivity(), PersonalData.class));
    }

    // Обработка результата выбора изображения
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            filepath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), filepath);
                binding.imageView2.setImageBitmap(getCircularBitmap(bitmap));
                saveProfileImageToCache(bitmap);
                uploadImage();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Ошибка при загрузке изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Загрузка изображения в Firebase Storage
    private void uploadImage() {
        if (filepath != null) {
            ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setTitle("Загрузка...");
            progressDialog.show();

            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser == null) {
                progressDialog.dismiss();
                return;
            }

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), filepath);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageData = baos.toByteArray();

                StorageReference imageRef = storageReference.child("profile_images/" + currentUser.getUid() + ".jpg");
                imageRef.putBytes(imageData)
                        .addOnSuccessListener(taskSnapshot -> {
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), "Изображение загружено", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } catch (IOException e) {
                progressDialog.dismiss();
                e.printStackTrace();
                Toast.makeText(getActivity(), "Ошибка обработки изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Сохранение данных пользователя в SharedPreferences
    private void saveUserInfoToCache(String name, String email) {
        if (getContext() == null) return;
        getContext().getSharedPreferences("user_cache", 0)
                .edit()
                .putString("name", name)
                .putString("email", email)
                .apply();
    }

    // Загрузка сохраненных данных из SharedPreferences
    private void loadUserInfoFromCache() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("user_cache", 0);
        String cachedName = prefs.getString("name", null);
        String cachedEmail = prefs.getString("email", null);

        if (cachedName != null) {
            binding.textView2.setText(cachedName);
            binding.textView2.setVisibility(View.VISIBLE);
        }
        if (cachedEmail != null) {
            binding.textView4.setText(cachedEmail);
            binding.textView4.setVisibility(View.VISIBLE);
        }
    }

    // Сохранение изображения в локальный кэш
    private void saveProfileImageToCache(Bitmap bitmap) {
        try {
            File file = new File(getContext().getCacheDir(), "profile.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Загрузка изображения из кэша
    private Bitmap loadProfileImageFromCache() {
        try {
            File file = new File(getContext().getCacheDir(), "profile.jpg");
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Очистка ресурсов при уничтожении фрагмента
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) binding = null;
        if (ref != null && profileListener != null) ref.removeEventListener(profileListener);
    }
}
