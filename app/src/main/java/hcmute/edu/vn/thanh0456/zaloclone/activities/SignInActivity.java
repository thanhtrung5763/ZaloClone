package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

import hcmute.edu.vn.thanh0456.zaloclone.MainActivity;
import hcmute.edu.vn.thanh0456.zaloclone.R;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivitySignInBinding;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

public class SignInActivity extends AppCompatActivity {

    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    private ActivitySignInBinding binding;
    // Lưu hoặc lấy dữ liệu trong SharePref
    private PreferenceManager preferenceManager;
    // Tạo tài khoản và xác thực đăng nhập bằng email sử dụng FirebaseAuth
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        FirebaseUser user = mAuth.getCurrentUser();
//        if(user != null) {
//            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//            startActivity(intent);
//            finish();
//        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
    }

    // Hàm khởi tạo
    private void init() {
        // Lưu hoặc lấy dữ liệu từ SharePreferences
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Đăng nhập sử dụng FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
    }

    // Hàm thiết lập event
    private void setListeners() {
        // Chuyển đến trang đăng kí tài khoản
        binding.textCreateNewAccount.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
//        binding.buttonSignIn.setOnClickListener(v -> addDataToFirestore());
        // Validate dữ liệu đầu vào, nếu hợp lệ thì thực hiện gọi hàm xử lí đăng nhập signIn
        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signIn();
            }
        });
        // Không cần đăng nhập lại
        // Chuyển thằng đến trang chính nếu SharePref có lưu trữ thông tin user
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // Hàm nhận chuỗi string truyền vào và hiển thị
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    // Hàm xử lí đăng nhập
    private void signIn() {
        loading(true);
        // Đăng nhập với input của người dùng, sử dụng FirebaseAuth
        mAuth.signInWithEmailAndPassword(binding.inputEmail.getText().toString(), binding.inputPassword.getText().toString())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Successfully Logged In");
                        storeCurrentUserInPrefs();
                    }
                    else {
                        loading(false);
                        showToast(task.getException().getMessage());
                    }
                });
    }

    // Lấy dữ liệu user từ Firestore và lưu trong SharePrefs
    private void storeCurrentUserInPrefs() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null
                            && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                        // Chuyển đến trang chính
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        loading(false);
                        showToast(task.getException().getMessage());
                    }
                });
    }

    // Hiệu ứng loading trong khi chờ tải dữ liệu
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignIn.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    // Validate thông tin đăng kí do người dùng nhập
    private Boolean isValidSignUpDetails() {
        if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        }
        return true;
    }
//    private void addDataToFirestore() {
//        FirebaseFirestore database = FirebaseFirestore.getInstance();
//        HashMap<String, Object> data = new HashMap<>();
//        data.put("first_name", "Thanh");
//        data.put("last_name", "Ninh");
//        database.collection("users")
//                .add(data)
//                .addOnSuccessListener(documentReference -> {
//                    Toast.makeText(getApplicationContext(), "Data Inserted", Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(exception -> {
//                    Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
}