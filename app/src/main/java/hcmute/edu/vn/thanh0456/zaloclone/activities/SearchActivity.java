package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.R;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.UsersAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivitySearchBinding;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.UserListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

public class SearchActivity extends AppCompatActivity implements UserListener {

    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    ActivitySearchBinding binding;
    // Lưu hoặc lấy dữ liệu trong SharePref
    PreferenceManager preferenceManager;
    // instance của Firestore để tương tác với dữ liệu khi cần
    FirebaseFirestore database;
    // Danh sách các user
    ArrayList<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
        getUsers();
    }

    // Hàm khởi tạo
    private void init() {
        // Lưu hoặc lấy dữ liệu từ SharePrefs
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Tương tác với dữ liệu trên Firestore
        database = FirebaseFirestore.getInstance();
        // Lưu dữ liệu các user
        users = new ArrayList<>();
        // Hiển thị bàn phím để nhập input ngay khi chuyển đến trang này
        binding.inputSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    // Hàm thiết lập event
    private void setListeners() {
        // Trở về trang trước
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // Làm trống inputSearch
        binding.imageCancel.setOnClickListener(v -> binding.inputSearch.setText(""));
        // Thay đổi giao diện, ẩn/hiện dựa trên trạng thái của inputSearch
        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (binding.inputSearch.getText().length() != 0) {
                    binding.imageCancel.setVisibility(View.VISIBLE);
                } else {
                    binding.imageCancel.setVisibility(View.GONE);
                    binding.textErrorMessage.setVisibility(View.GONE);
                    binding.suggested.setVisibility(View.VISIBLE);
                    getUsers();
                }
            }
        });

        // Thực hiện tìm kiếm user theo tên dựa trên input do người dùng nhập vào inputSearch
        binding.inputSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    users.clear();
                    String text = binding.inputSearch.getText().toString().toLowerCase().trim();
                    database.collection(Constants.KEY_COLLECTION_USERS)
                            .get()
                            .addOnCompleteListener(task -> {
                                loading(false);
                                String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                                if (task.isSuccessful() && task.getResult() != null) {
                                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                        if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                            continue;
                                        }
                                        if (queryDocumentSnapshot.getString(Constants.KEY_NAME).toLowerCase().contains(text)) {
                                            User user = new User();
                                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                            user.id = queryDocumentSnapshot.getId();
                                            users.add(user);
                                        }
                                    }
                                    if (users.size() > 0) {

                                        UsersAdaptor usersAdaptor = new UsersAdaptor(users, SearchActivity.this::onUserClicked);
                                        binding.usersRecyclerView.setAdapter(usersAdaptor);
                                        binding.textErrorMessage.setVisibility(View.GONE);
                                        binding.usersRecyclerView.setVisibility(View.VISIBLE);
                                        usersAdaptor.notifyDataSetChanged();
                                    } else {
                                        binding.usersRecyclerView.setVisibility(View.GONE);
                                        showErrorMessage();
                                    }
                                    binding.suggested.setVisibility(View.GONE);
                                } else {
                                    showErrorMessage();
                                    binding.suggested.setVisibility(View.GONE);
                                }
                            });
                    return true;
                }
                return false;
            }
        });
    }

    // Hàm xử lý lấy tất cả user từ Firestore đẩy vào RecyclerView
    private void getUsers() {
        users.clear();
        loading(true);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
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
                            usersAdaptor.notifyDataSetChanged();
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