package hcmute.edu.vn.thanh0456.zaloclone.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.adaptor.UsersAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityUsersBinding;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.UserListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

// extends BaseActivity để theo dõi và cập nhật tình trạng on/off của người dùng
public class UsersActivity extends BaseActivity implements UserListener {

    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    private ActivityUsersBinding binding;
    // Lưu hoặc lấy dữ liệu trong SharePref
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
        getUsers();
    }

    // Hàm khởi tạo
    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
    }

    // Hàm thiết lập event
    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }

    // Hàm lấy dữ liệu tất cả user từ Firestore, đưa vào trong RecyclerView
    private void getUsers() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        ArrayList<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }
                        if (users.size() > 0) {
                            UsersAdaptor usersAdaptor = new UsersAdaptor(users, this);
                            binding.usersRecyclerView.setAdapter(usersAdaptor);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }

    // Hàm hiển thị thông báo lỗi
    private void showErrorMessage() {
        binding.textErrorMessage.setText(String.format("%s", "No user available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    // Hiệu ứng loading trong khi chờ tải dữ liệu
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    // Khi click vào một thành viên nào đó, chuyển đến trang chatting với thành viên đó
    @Override
    public void onUserClicked(View v, User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }

}