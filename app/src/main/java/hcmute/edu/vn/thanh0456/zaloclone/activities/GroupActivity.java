package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import hcmute.edu.vn.thanh0456.zaloclone.adaptor.UsersAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityGroupBinding;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.UserListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

public class GroupActivity extends AppCompatActivity implements UserListener{

    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    private static ActivityGroupBinding binding;
    // Ảnh được mã hoá thành kiểu String
    private String encodeImage;
    // Lưu hoặc lấy dữ liệu trong SharePref
    private PreferenceManager preferenceManager;
    // Id của group chat
    private String groupId = null;
    // Danh sách các user được chọn để thêm vào nhóm
    private static ArrayList<User> selectedUser;
    // Tương tác với dữ liệu trên Firestore
    private FirebaseFirestore database;
    // Tham chiếu đến collection group
    private CollectionReference memberRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(binding.getRoot());
        setListeners();
        getUsers();
    }

    // Hàm khởi tạo
    private void init() {
        binding = ActivityGroupBinding.inflate(getLayoutInflater());
        // Lưu hoặc lấy dữ liệu trong SharePref
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Chứa các user sẽ được thêm vào group
        selectedUser = new ArrayList<>();
        // instance của Firestore để tương tác với dữ liệu khi cần
        database = FirebaseFirestore.getInstance();
        // Tham chiếu đến collection group
        memberRef = database.collection(Constants.KEY_COLLECTION_GROUP);
    }

    // Hàm tạo group chat
    private void createGroup() {
        // Thiết lập dữ liệu
        HashMap<String, Object> group = new HashMap<>();
        group.put(Constants.KEY_OWNER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        group.put(Constants.KEY_OWNER_NAME, preferenceManager.getString(Constants.KEY_NAME));
        group.put(Constants.KEY_GROUP_IMAGE, encodeImage);
        group.put(Constants.KEY_GROUP_NAME, binding.inputGroupName.getText().toString());
        group.put(Constants.KEY_TIMESTAMP, new Date());
        DocumentReference ref = database.collection(Constants.KEY_COLLECTION_GROUP).document();
        groupId = ref.getId();
        // Tạo mới một document trong collection group
        addGroup(group);
        // Thêm dữ liệu user vào document vừa tạo
        addMemberToGroup();
        // Trở về GroupFragment
        onBackPressed();
    }

    // Hàm xử lí tạo mới một document trong collection group
    private void addGroup(HashMap<String, Object> group) {
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(groupId)
                .set(group);
    }

    // Hàm xử lý thêm user vào trong document vừa tạo
    private void addMemberToGroup() {
        // Thêm user là owner của group
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        user.put(Constants.KEY_ROLE, "owner");
        memberRef.document(groupId).collection(Constants.KEY_COLLECTIONS_MEMBERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .set(user);

        // Thêm user là member của group
        for(User user1 : selectedUser) {
            HashMap<String, Object> user2 = new HashMap<>();
            user2.put(Constants.KEY_USER_ID, user1.id);
            user2.put(Constants.KEY_ROLE, "member");
            memberRef.document(groupId).collection(Constants.KEY_COLLECTIONS_MEMBERS)
                    .document(user1.id)
                    .set(user2);
        }
        showToast("Group Created");
    }

    // Hàm thiết lập event
    private void setListeners() {
        // Chọn ảnh trong thiết bị
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        // Trở về trang trước
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // Tạo group
        binding.buttonCreate.setOnClickListener(v -> {
            if (!selectedUser.isEmpty()) {
                createGroup();
            }
        });
    }

    // Hàm kéo dữ liệu user từ Firestore, đẩy vào RecyclerView
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
                            UsersAdaptor usersAdaptor = new UsersAdaptor(users, null);
                            binding.usersRecyclerView.setAdapter(usersAdaptor);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        // Nếu task không thành công, hiển thị lỗi
                        showErrorMessage();
                    }
                });
    }

    // Hàm hiển thị lỗi (cho debug)
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

    // Chuyển ảnh dạng bitmap sang String
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // Sau khi chọn ảnh đại diện cho group, hiển thị lên giao diện
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
                        // gán ảnh đại diện đã encode sang String vào biến
                        // đợi xử lí đẩy lên lưu trong Firestore
                        encodeImage = encodeImage(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    // Hàm xử lí thay đổi trên list selectedUser
    public static void onCheckedChangeListener(User user, Boolean isSelect) {
        if (isSelect) {
            selectedUser.add(user);
        } else {
            selectedUser.remove(user);
        }
        if (selectedUser.isEmpty()) {
            loadingButton(false);
        } else {
            loadingButton(true);
        }
    }

    // Hiệu ứng loading trong khi chờ tải dữ liệu
    public static void loadingButton(Boolean isLoading) {
        if (isLoading) {
            binding.buttonCreate.setVisibility(View.VISIBLE);
        } else {
            binding.buttonCreate.setVisibility(View.INVISIBLE);
        }
    }

    // Hàm nhận chuỗi String truyền vào và hiển thị
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Hàm trống do tái sử dụng UsersAdaptor nhưng không cần click vào user để đến trang chatting
    @Override
    public void onUserClicked(View v, User user) {
        // do nothing
    }

}