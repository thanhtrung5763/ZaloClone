package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;

import hcmute.edu.vn.thanh0456.zaloclone.activities.ChatActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.GroupMessageActivity;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerRecentGroupBinding;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.ConversationListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.Group;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;

// Adaptor xử lí các thay đổi liên quan đến RecentGroup RecyclerView (các group có user của thiết bị là thành viên)
// cập nhật và đính dữ liệu vào các viewHolder, hiển thị lên cho người dùng
public class RecentGroupAdaptor extends RecyclerView.Adapter<RecentGroupAdaptor.GroupViewHolder>{

    // Danh sách các nhóm có chứa user là thành viên
    private static ArrayList<Group> groups = new ArrayList<>();
    // Constructor nhận vào danh sách các group có user của thiết bị là thành viên
    public RecentGroupAdaptor(ArrayList<Group> groups) {
        RecentGroupAdaptor.groups = groups;
    }

    // Tạo ViewHolder
    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupViewHolder(
                ItemContainerRecentGroupBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        // Đính dữ liệu vào ViewHolder
        holder.setData(groups.get(position));
        // Chuyển đến trang nhắn tin group khi user click vào một trong các ViewHolder(group) tương ứng
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), GroupMessageActivity.class);
            intent.putExtra(Constants.KEY_COLLECTION_GROUP, groups.get(position));
            v.getContext().startActivity(intent);
        });
    }

    // Trả về số lượng group
    @Override
    public int getItemCount() {
        return groups.size();
    }

    // Cập nhật trạng thái mới nhất của group
    public static void updateLastMessage(Group updatedGroup) {
        for (Group group : groups) {
            if (group.id.equals(updatedGroup.id)) {
                groups.remove(group);
                groups.add(updatedGroup);
                break;
            }
        }
        Collections.sort(groups, (obj1, obj2) -> obj2.lastMessageModel.dateObject.compareTo(obj1.lastMessageModel.dateObject));
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentGroupBinding binding;
        // binding thiết kế Viewholder
        GroupViewHolder(ItemContainerRecentGroupBinding itemContainerRecentGroupBinding) {
            super(itemContainerRecentGroupBinding.getRoot());
            binding = itemContainerRecentGroupBinding;
        }

        // Đính dữ liệu vào Viewholder
        void setData(Group group) {
            binding.imageProfile.setImageBitmap(getConversationImage(group.image));
            binding.textName.setText(group.name);
            if (group.lastMessageModel != null) {
                binding.textRecentMessage.setText(group.lastMessageModel.message);
            } else {
                binding.textRecentMessage.setText(String.format("Last Message"));
            }
//            if (group.lastMessageModel.senderId.equals(senderId)) {
//                if (chatMessage.type.equals("text")) {
//                    binding.textRecentMessage.setText(String.format("You: %s", gr.message));
//                }
//                else if (chatMessage.type.equals("image")) {
//                    binding.textRecentMessage.setText(String.format("You %s", "just sent an image"));
//                } else if (chatMessage.type.equals("audio")){
//                    binding.textRecentMessage.setText(String.format("You %s", "just send an audio"));
//                }
////                database.collection(Constants.KEY_COLLECTION_USERS)
////                        .document(chatMessage.receiverId)
////                        .addSnapshotListener((Activity) mcontext, ((value, error) -> {
////                            if (error != null) {
////                                return;
////                            }
////                            if (value != null) {
////                                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
////                                    int availability = Objects.requireNonNull(
////                                            value.getLong(Constants.KEY_AVAILABILITY)
////                                    ).intValue();
////                                    isReceiverAvailaible = availability == 1;
////                                }
////                            }
////                            if (isReceiverAvailaible) {
////                                binding.userAvailability.setVisibility(View.VISIBLE);
////                            } else {
////                                binding.userAvailability.setVisibility(View.GONE);
////                            }
////                        }));
//            }
//            else {
//                if (chatMessage.type.equals("text")) {
//                    binding.textRecentMessage.setText(chatMessage.message);
//                } else if (chatMessage.type.equals("image")) {
//                    binding.textRecentMessage.setText(String.format("%s %s", binding.textName.getText().toString(), "sent you an image"));
//                } else if (chatMessage.type.equals("audio")){
//                    binding.textRecentMessage.setText(String.format("%s %s", binding.textName.getText().toString(), "sent you an audio"));
//                }
////                database.collection(Constants.KEY_COLLECTION_USERS)
////                        .document(chatMessage.senderId)
////                        .addSnapshotListener((Activity) mcontext, ((value, error) -> {
////                            if (error != null) {
////                                return;
////                            }
////                            if (value != null) {
////                                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
////                                    int availability = Objects.requireNonNull(
////                                            value.getLong(Constants.KEY_AVAILABILITY)
////                                    ).intValue();
////                                    isReceiverAvailaible = availability == 1;
////                                }
////                            }
////                            if (isReceiverAvailaible) {
////                                binding.userAvailability.setVisibility(View.VISIBLE);
////                            } else {
////                                binding.userAvailability.setVisibility(View.GONE);
////                            }
////                        }));
//            }
//            binding.getRoot().setOnClickListener(v -> {
//                User user = new User();
//                user.id = chatMessage.conversationId;
//                user.name = chatMessage.conversationName;
//                user.image = chatMessage.conversationImage;
//                conversationListener.onConversationClicked(user);
//            });
        }
    }

    // Chuyển đổi ảnh dạng String sang Bitmap để đính vào thiết kế Viewholder
    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

}
