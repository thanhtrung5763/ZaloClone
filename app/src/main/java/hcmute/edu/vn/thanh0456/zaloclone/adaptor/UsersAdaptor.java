package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.activities.GroupActivity;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerUserBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerUserGroupBinding;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.UserListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;

// Adaptor xử lí các thay đổi liên quan đến Users RecyclerView (danh sách các user)
// người dùng có thể click vào một user bất kì để bắt đầu chat với nhau
public class UsersAdaptor extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    // Danh sách user
    private final ArrayList<User> users;
    // Xử lí sự kiện khi user click vào một user khác, sẽ tạo conversation và chuyển đến trang chatting
    private final UserListener userListener;

    // Do có tái sử dụng lại UsersAdaptor trong việc chọn user để thêm vào group
    // nên sẽ chia 2 trường hợp tương ứng với 2 kiểu thiết kế ViewHolder
    // một cho hiển thị danh sách user để bắt đầu chat nếu click vào
    // một cho hiển thị danh sách user để lựa chọn và thêm vào group
    public static final int VIEW_TYPE_FRAGMENT_USER = 1;
    public static final int VIEW_TYPE_FRAGMENT_GROUP = 2;
    private final int VIEW_TYPE;

    // Constructor nhận vào danh sách các user, xác định VIEW_TYPE để hiển thị kiểu thiết kế ViewHolder tương ứng
    public UsersAdaptor(ArrayList<User> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
        VIEW_TYPE = getViewType(userListener);
    }

    // Tạo ViewHolder dựa theo VIEW_TYPE đã xác định
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (this.VIEW_TYPE == VIEW_TYPE_FRAGMENT_USER) {
            ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new UserViewHolder(itemContainerUserBinding);
        } else{
            ItemContainerUserGroupBinding itemContainerUserGroupBinding = ItemContainerUserGroupBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new UserGroupViewHolder(itemContainerUserGroupBinding);
        }
    }

    // Đính dữ liệu vào ViewHolder dựa theo VIEW_TYPE đã xác định
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (this.VIEW_TYPE == VIEW_TYPE_FRAGMENT_USER) {
            ((UserViewHolder)holder).setUserData(users.get(position));
        } else {
            ((UserGroupViewHolder)holder).setUserData(users.get(position));
        }
    }

    // Trả về số lượng user
    @Override
    public int getItemCount() {
        return users.size();
    }

    // Xác định VIEW_TYPE dựa trên việc có sử dụng userListener hay không
    public int getViewType(UserListener userListener) {
        if (userListener != null) {
            return VIEW_TYPE_FRAGMENT_USER;
        }
        return VIEW_TYPE_FRAGMENT_GROUP;
    }

    // Class ViewHolder sử dụng cho UsersActivity
    class UserViewHolder extends RecyclerView.ViewHolder {
        ItemContainerUserBinding binding;
        // binding thiết kế ViewHolder
        UserViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }
        // Đính dữ liệu vào ViewHolder
        void setUserData(User user) {
            binding.textName.setText(user.name);
            binding.textEmail.setText(user.email);
            binding.imageProfile.setImageBitmap(getUserImage(user.image));
            binding.getRoot().setOnClickListener(v -> userListener.onUserClicked(v, user));
        }
    }

    // Class ViewHolder sử dụng cho GroupActivity
    class UserGroupViewHolder extends RecyclerView.ViewHolder {
        ItemContainerUserGroupBinding binding;
        // binding thiết kế ViewHolder
        UserGroupViewHolder(ItemContainerUserGroupBinding itemContainerUserGroupBinding) {
            super(itemContainerUserGroupBinding.getRoot());
            binding = itemContainerUserGroupBinding;
        }
        // Đính dữ liệu vào ViewHolder
        void setUserData(User user) {
            binding.textName.setText(user.name);
            binding.textEmail.setText(user.email);
            binding.imageProfile.setImageBitmap(getUserImage(user.image));
            binding.cbSelectUser.setOnCheckedChangeListener((compoundButton, b) -> {
                GroupActivity.onCheckedChangeListener(user, compoundButton.isChecked());
            });
        }
    }

    // Chuyển ảnh dạng String sang Bitmap
    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

}
