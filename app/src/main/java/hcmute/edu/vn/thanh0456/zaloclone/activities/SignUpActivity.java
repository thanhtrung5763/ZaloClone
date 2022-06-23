package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

import hcmute.edu.vn.thanh0456.zaloclone.MainActivity;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivitySignUpBinding;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

// Đăng kí tài khoản
public class SignUpActivity extends AppCompatActivity {

    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    private ActivitySignUpBinding binding;
    // Lưu hoặc lấy dữ liệu trong SharePref
    private PreferenceManager preferenceManager;
    // Ảnh đã được mã hoá dạng String
    private String encodeImage;
    // Tạo tài khoản và xác thực đăng nhập bằng email sử dụng FirebaseAuth
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
    }
    // Hàm khởi tạo
    private void init() {
        // Lưu hoặc lấy dữ liệu từ SharePreferences
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Đăng kí tài khoản sử dụng FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
    }
    // Hàm thiết lập event
    private void setListeners() {
        binding.textSignIn.setOnClickListener(v -> onBackPressed());

        // Validate thông tin người dùng nhập, nếu thoả mãn sẽ gọi hàm thực hiện đăng kí
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signUp();
            }
        });
        // Chọn ảnh trong thiết bị
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    // Hàm nhận chuỗi string truyền vào và hiển thị
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Hàm đăng kí tài khoản
    private void signUp() {
        loading(true);
        createAccount();
    }
    // Tạo tài khoản
    private void createAccount() {
        // Tạo tài khoản bằng email và password sử dụng FirebaseAuth
        mAuth.createUserWithEmailAndPassword(binding.inputEmail.getText().toString(), binding.inputPassword.getText().toString())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Successfully Registered");
                        insertUser();
                    } else {
                        // Hiển thị thông báo nếu thất bại
                        loading(false);
                        showToast(task.getException().getMessage()
                        );
                    }
                });
    }
    // Thêm user vào Firestore
    private void insertUser() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        user.put(Constants.KEY_IMAGE, encodeImage);
        // Thêm user vào collection trong Firestore
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    // Lưu dữ liệu của user vào SharePreferences
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodeImage);
                    // Chuyển tới màn hình chính
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    // Hiển thị thông báo nếu thất bại
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    // Validate thông tin đăng kí do người dùng nhập
    private Boolean isValidSignUpDetails() {
        if (encodeImage == null) {
            showToast("Select profile image");
            return false;
        } else if (binding.inputName.getText().toString().trim().isEmpty()) {
            showToast("Enter name");
            return false;
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Confirm your password");
            return false;

        } else if (binding.inputPassword.getText().toString().length() < 6) {
            showToast("Password should be at least 6 characters");
            return false;
        }
        else if (!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())) {
            showToast("Password & confirm password must be same");
            return false;
        }
        return true;
    }

    // Encode ảnh từ Bitmap sang String
    // để lưu lên Firestore
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // Chọn ảnh trong thiết bị
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imageProfile.setImageBitmap(bitmap);
                        binding.textAddImage.setVisibility(View.GONE);
                        encodeImage = encodeImage(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
    );


    // Hiệu ứng loading trong khi chờ tải dữ liệu
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignUp.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}