package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Objects;

import hcmute.edu.vn.thanh0456.zaloclone.activities.ChatActivity;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerRecentConversationBinding;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.ConversationListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

// Adaptor xử lí các thay đổi liên quan đến RecentConversation RecyclerView(các cuộc trò chuyện của user với user khác)
// cập nhật và đính dữ liệu vào các viewHolder, hiển thị lên cho người dùng
public class RecentConversationAdaptor extends RecyclerView.Adapter<RecentConversationAdaptor.ConversationViewHolder>{

    // Dữ liệu liên quan đến tin nhắn mới nhất trong conversation giữa 2 user
    private final ArrayList<ChatMessage> chatMessages;
    // id người gửi tin
    private final String senderId;
    // Xử lí sự kiện khi user click vào một conversation bất kì
    private final ConversationListener conversationListener;
    // Lưu context
    private final Context mcontext;
    // Tương tác với dữ liệu trên Firestore
    private final FirebaseFirestore database;
    // Kiểm tra tình trạng on/off của user khác
    private Boolean isReceiverAvailaible = false;

    // Constructor nhận vào danh sách dữ liệu tin nhắn mới nhất giữa các user, id người gửi tin nhắn
    // một instance listener để xử lí sự kiện khi người dùng click vào,
    public RecentConversationAdaptor(ArrayList<ChatMessage> chatMessages, String senderId, ConversationListener conversationListener, Context context) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;
        this.conversationListener = conversationListener;
        this.mcontext = context;
        this.database = FirebaseFirestore.getInstance();
    }

    // Tạo viewHolder
    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversationViewHolder(
                ItemContainerRecentConversationBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    // Đính dữ liệu vào viewHolder
    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    // Trả về số lượng/kích thước của danh sách chatMessages
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversationBinding binding;

        // binding thiết kế ViewHolder
        ConversationViewHolder(ItemContainerRecentConversationBinding itemContainerRecentConversationBinding) {
            super(itemContainerRecentConversationBinding.getRoot());
            binding = itemContainerRecentConversationBinding;
        }

        // Đính dữ liệu vào viewHolder tuỳ theo trường hợp tin nhắn dạng text, image hay audio
        // Đồng thời cập nhật tình trạng hoạt động của user được nhận tin nhắn
        void setData(ChatMessage chatMessage) {
            binding.imageProfile.setImageBitmap(getConversationImage(chatMessage.conversationImage));
            binding.textName.setText(chatMessage.conversationName);
            if (chatMessage.senderId.equals(senderId)) {
                if (chatMessage.type.equals("text")) {
                    binding.textRecentMessage.setText(String.format("You: %s", chatMessage.message));
                } else if (chatMessage.type.equals("image")) {
                    binding.textRecentMessage.setText(String.format("You %s", "just sent an image"));
                } else if (chatMessage.type.equals("audio")){
                    binding.textRecentMessage.setText(String.format("You %s", "just send an audio"));
                }
                database.collection(Constants.KEY_COLLECTION_USERS)
                        .document(chatMessage.receiverId)
                        .addSnapshotListener((Activity) mcontext, ((value, error) -> {
                            if (error != null) {
                                return;
                            }
                            if (value != null) {
                                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                                    int availability = Objects.requireNonNull(
                                            value.getLong(Constants.KEY_AVAILABILITY)
                                    ).intValue();
                                    isReceiverAvailaible = availability == 1;
                                }
                            }
                            if (isReceiverAvailaible) {
                                binding.userAvailability.setVisibility(View.VISIBLE);
                            } else {
                                binding.userAvailability.setVisibility(View.GONE);
                            }
                        }));
            }
            else {
                if (chatMessage.type.equals("text")) {
                    binding.textRecentMessage.setText(chatMessage.message);
                } else if (chatMessage.type.equals("image")) {
                    binding.textRecentMessage.setText(String.format("%s %s", binding.textName.getText().toString(), "sent you an image"));
                } else if (chatMessage.type.equals("audio")){
                    binding.textRecentMessage.setText(String.format("%s %s", binding.textName.getText().toString(), "sent you an audio"));
                }
                database.collection(Constants.KEY_COLLECTION_USERS)
                        .document(chatMessage.senderId)
                        .addSnapshotListener((Activity) mcontext, ((value, error) -> {
                            if (error != null) {
                                return;
                            }
                            if (value != null) {
                                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                                    int availability = Objects.requireNonNull(
                                            value.getLong(Constants.KEY_AVAILABILITY)
                                    ).intValue();
                                    isReceiverAvailaible = availability == 1;
                                }
                            }
                            if (isReceiverAvailaible) {
                                binding.userAvailability.setVisibility(View.VISIBLE);
                            } else {
                                binding.userAvailability.setVisibility(View.GONE);
                            }
                        }));
            }
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.id = chatMessage.conversationId;
                user.name = chatMessage.conversationName;
                user.image = chatMessage.conversationImage;
                // gọi đến một hàm xử lí sự kiện khi user click vào một Viewholder trong RecyclerView
                conversationListener.onConversationClicked(user);
            });
        }
    }

    // Chuyển đổi ảnh dạng String sang Bitmap để đính vào thiết kế của Viewholder
    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

}
