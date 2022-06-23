package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.MainActivity;
import hcmute.edu.vn.thanh0456.zaloclone.R;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.UsersAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityGroupInfoBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.DialogEditGroupnameBinding;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.UserListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.Group;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

public class GroupInfoActivity extends AppCompatActivity implements UserListener {

    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    ActivityGroupInfoBinding binding;
    // instance lưu dữ liệu liên quan đến group
    private Group group;
    // Ảnh được mã hoá thành kiểu String
    private String encodeImage;
    // instance của Firestore để tương tác với dữ liệu khi cần
    FirebaseFirestore database;

    // Kiểm tra xem có sự thay đổi liên quan đến group hay không
    // để gửi dữ liệu bị thay đổi vào trang trước, cập nhật theo real-time
    private Boolean isGroupChange;

    // Lưu hoặc lấy dữ liệu trong SharePref
    private PreferenceManager preferenceManager;
    // Lưu role của người dùng trong nhóm
    private String role;
    // Adaptor
    private UsersAdaptor usersAdaptor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
        loadGroupInfo();
        getAllUsersOfGroup();
        getRole();
    }

    // Hàm khởi tạo
    private void init() {
        // Lưu hoặc lấy dữ liệu vào SharePrefs
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Tương tác với dữ liệu lưu trên Firestore
        database = FirebaseFirestore.getInstance();
        // Các biến khởi tạo, giá trị thay đổi dựa trên sự thay đổi của group(ảnh, tên, số lượng thành viên)
        isGroupChange = false;
    }

    // Hàm thiết lập event
    private void setListeners() {
        // Trở về trang trước
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // Hiển thị một popupMenu có các lựa chọn thay đổi tên group, ảnh group, xoá group, rời group
        binding.imageMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(GroupInfoActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.popup_group, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.changeGroupPhoto) {
                    changeGroupPhoto();

                } else if(menuItem.getItemId() == R.id.changeName) {
                    changeGroupName();

                } else if (menuItem.getItemId() == R.id.deleteGroup) {
                    deleteGroup();

                } else if (menuItem.getItemId() == R.id.leaveGroup) {
//                    leaveGroup();
                }
                return true;
            });
            popupMenu.show();
        });
        // tablayout xem member hoặc admin trong group
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    getAllUsersOfGroup();
                }
                else {
                    getAdminOfGroup();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    // Lưu role của user hiện tại trong nhóm hiện tại vào biến
    private void getRole() {
        if (preferenceManager.getString(Constants.KEY_USER_ID).equals(group.ownerId)) {
            role = "owner";
            return;
        }
        for (User member : group.members) {
            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(member.id)) {
                role = member.role;
                return;
            }
        }
    }

    // Hàm xử lí thay ảnh group
    private void changeGroupPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImage.launch(intent);

    }

    // Hàm xử lí cập nhật ảnh lên Firestore
    private void updateGroupPhoto() {
        group.image = encodeImage;
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(encodeImage));
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .update(Constants.KEY_GROUP_IMAGE, encodeImage);
        isGroupChange = true;
    }

    // Hàm xử lí thay tên group
    private void changeGroupName() {
        // Hiển thị một dialog để thay đổi tên mới cho group
        // Có 3 option là remove, cancel, save
        DialogEditGroupnameBinding dialogEditGroupnameBinding = DialogEditGroupnameBinding.inflate(getLayoutInflater());
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogEditGroupnameBinding.getRoot());

        Window window = dialog.getWindow();

        if (window == null) {
            return;
        }

        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams windowAtrributes = window.getAttributes();
        windowAtrributes.gravity = Gravity.CENTER;
        window.setAttributes(windowAtrributes);

        dialogEditGroupnameBinding.edtGroupName.setText(binding.textName.getText().toString());
        dialogEditGroupnameBinding.edtGroupName.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        setDialogListeners(dialogEditGroupnameBinding, dialog);
        dialog.show();

    }

    // Hàm thiết lập event cho dialog
    private void setDialogListeners(DialogEditGroupnameBinding dialogEditGroupnameBinding, Dialog dialog) {
        // Làm trống nơi nhập tên group
        dialogEditGroupnameBinding.removeButton.setOnClickListener(v -> {
            dialogEditGroupnameBinding.edtGroupName.setText("");
        });
        // Đóng dialog
        dialogEditGroupnameBinding.cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });
        // Cập nhật tên mới cho group
        dialogEditGroupnameBinding.saveButton.setOnClickListener(v -> {
            if (!binding.textName.equals(dialogEditGroupnameBinding.edtGroupName)) {
                updateGroupName(dialogEditGroupnameBinding.edtGroupName.getText().toString());
            }
            dialog.dismiss();
        });
    }


    // Cập nhật tên mới của group lên Firestore
    private void updateGroupName(String newGroupName) {
        group.name = newGroupName;
        binding.textName.setText(newGroupName);
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .update(Constants.KEY_GROUP_NAME, newGroupName);
        showToast("Updated Group Name");
        isGroupChange = true;
    }

    // Hàm xử lí xoá group
    private void deleteGroup() {
        // Hiển thị một dialog với 2 option là yes, no
        // Chọn yes -> xoá group, chuyển về trang chính
        // Chọn no -> đóng dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Group")
                .setMessage("Are you sure to delete this group?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    database.collection(Constants.KEY_COLLECTION_GROUP)
                            .document(group.id)
                            .delete();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("deleteGroup", "deleteGroup");
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.cancel())
                .show();
    }

    // Sau khi chọn ảnh đại diện cho group, cập nhật lên giao diện, đẩy dữ liệu ảnh lên Firestore
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imageProfile.setImageBitmap(bitmap);
                        encodeImage = encodeImage(bitmap);
                        updateGroupPhoto();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    // Hàm chuyển ảnh dạng Bitmap sang String
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // Hàm xử lý kéo tất cả user của group, đẩy vào RecyclerView
    private void getAllUsersOfGroup() {
        loading(true);
        if (group.members.size() > 0) {
            usersAdaptor = new UsersAdaptor(group.members, this);
            binding.usersRecyclerView.setAdapter(usersAdaptor);
            binding.usersRecyclerView.setVisibility(View.VISIBLE);
        } else {
            showErrorMessage();
        }
        loading(false);
    }

    // Hàm xử lý kéo user có role owner hoặc admin, đẩy vào RecyclerView
    private void getAdminOfGroup() {
        loading(true);
        if (group.members.size() > 0) {
            ArrayList<User> adminList = new ArrayList<>();
            for (User user : group.members) {
                if (user.role.equals("owner") || user.role.equals("admin")) {
                    adminList.add(user);
                }
            }
            usersAdaptor = new UsersAdaptor(adminList, this);
            binding.usersRecyclerView.setAdapter(usersAdaptor);
            binding.usersRecyclerView.setVisibility(View.VISIBLE);
        } else {
            showErrorMessage();
        }
        loading(false);
    }

    // Hàm gắn thông tin cơ bản của group vào giao diện
    private void loadGroupInfo() {
        group = getIntent().getParcelableExtra(Constants.KEY_COLLECTION_GROUP);
        binding.textName.setText(group.name);
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(group.image));
    }

    // Hàm chuyển ảnh dạng String sang Bitmap
    private Bitmap getBitmapFromEncodedImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    // Hàm hiển thị lỗi
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

        if (user.id.equals(group.ownerId)) {
            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(user.id)) {
                return;
            }
            messageWithMember(user);
            return;
        }

        if (role.equals("owner") || role.equals("admin")) {
            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(user.id)) {
                return;
            }
            PopupMenu popupMenu = new PopupMenu(GroupInfoActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.popup_member_group_for_admin, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.message) {
                    messageWithMember(user);

                } else if (menuItem.getItemId() == R.id.setAdmin) {
                    setAdminToMember(user);

                } else if (menuItem.getItemId() == R.id.removeMember) {
                    removeMember(user);
                }
                return true;
            });
            popupMenu.show();
        } else if (role.equals("member")) {
            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(user.id)) {
                return;
            }
            messageWithMember(user);
        }

    }

    // Hàm xử lí xoá thành viên khỏi group
    private void removeMember(User user) {
        CollectionReference groupRef = database.collection(Constants.KEY_COLLECTION_GROUP);
        groupRef.document(group.id)
                .collection(Constants.KEY_COLLECTIONS_MEMBERS)
                .document(user.id)
                .delete();
        showToast("Deleted Member");
        for (User member : group.members) {
            if (member.id.equals(user.id)) {
                group.members.remove(member);
                break;
            }
        }
        usersAdaptor.notifyDataSetChanged();
        isGroupChange = true;

    }

    // Hàm xử lí chuyển đến trang chatting với user được chọn
    private void messageWithMember(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }

    // Hàm xử lí set quyền admin cho thành viên trong group
    private void setAdminToMember(User user) {
        CollectionReference groupRef = database.collection(Constants.KEY_COLLECTION_GROUP);
        groupRef.document(group.id)
                .collection(Constants.KEY_COLLECTIONS_MEMBERS)
                .document(user.id)
                .update(Constants.KEY_ROLE, "admin");

        for (User member : group.members) {
            if (member.id.equals(user.id)) {
                member.role = "admin";
                break;
            }
        }
        showToast("Set Admin Successfully");
    }

    // Hàm nhận chuỗi String truyền vào và hiển thị
    private void showToast(String message) {
        Toast.makeText(GroupInfoActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    // Hàm xử lý quay về trang trước
    // truyền theo dữ liệu đã thay đổi của group(ảnh, tên, số lượng thành viên)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Constants.KEY_COLLECTION_GROUP);
        if (isGroupChange) {
            intent.putExtra(
                    Constants.KEY_COLLECTION_GROUP,
                    group
            );
        }
        if (isGroupChange) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }
}