package hcmute.edu.vn.thanh0456.zaloclone;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

import hcmute.edu.vn.thanh0456.zaloclone.activities.BaseActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.SignInActivity;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityMainBinding;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

// extends BaseActivity để theo dõi và cập nhật tình trạng on/off của người dùng
public class MainActivity extends BaseActivity {

    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    private ActivityMainBinding binding;
    // Lưu hoặc lấy dữ liệu trong SharePrefs
    private  PreferenceManager preferenceManager;
    // Biến static ánh xạ đến context của application
    public static Context contextOfApplication;
    public static Context getContextOfApplication()
    {
        return contextOfApplication;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        loadHeader();
        setListeners();
        getToken();
    }

    // Hàm khởi tạo
    private void init() {
        contextOfApplication = getApplicationContext();
        // Lưu hoặc lấy dữ liệu từ SharePreferences
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Mặc định khi vào trang chính sẽ load giao diện ChatFragment
        // Khi người dùng xoá group chat, trở về trang chính và load giao diện GroupFragment
        if (getIntent().getStringExtra("deleteGroup") == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new ChatFragment()).commit();
        } else {
            binding.bottomNav.getMenu().getItem(1).setChecked(true);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new GroupFragment()).commit();
        }
    }

    // Load giao diện header từ thông tin của user
    private void loadHeader() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    // Tạo FCM_TOKEN cho user, lưu vào SharePrefs, cập nhật lên Firestore
    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }
    private void updateToken(String token) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnSuccessListener(unused -> showToast("Token updated successfully"))
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    // Hàm nhận chuỗi string truyền vào và hiển thị
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Hàm thiết lập event
    private void setListeners() {
        binding.imageSignOut.setOnClickListener(v -> signOut());
        binding.bottomNav.setOnItemSelectedListener(navListener);
    }

    // Chuyển đổi giữa các fragment theo lựa chọn từ user
    private NavigationBarView.OnItemSelectedListener navListener = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.nav_chat:
                    selectedFragment = new ChatFragment();
                    break;
                case R.id.nav_group:
                    selectedFragment = new GroupFragment();
                    break;
                case R.id.nav_person:
                    selectedFragment = new PersonFragment();
                    break;
            }
            if (selectedFragment instanceof PersonFragment) {
                binding.imageProfile.setVisibility(View.GONE);
                binding.textName.setVisibility(View.GONE);
                binding.imageSignOut.setVisibility(View.GONE);
            } else {
                binding.imageProfile.setVisibility(View.VISIBLE);
                binding.textName.setVisibility(View.VISIBLE);
                binding.imageSignOut.setVisibility(View.VISIBLE);
            }
            loadHeader();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
            return true;
        }
    };

    // Hàm đăng xuất, khi được gọi sẽ xoá FCM_TOKEN trong document của user trên Firestore, xoá dữ liệu user trong SharePrefs
    // và chuyển tới trang đăng nhập
    private void signOut() {
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    FirebaseAuth.getInstance().signOut();
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}