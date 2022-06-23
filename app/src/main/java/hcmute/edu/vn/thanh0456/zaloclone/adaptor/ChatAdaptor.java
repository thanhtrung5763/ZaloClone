package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerReceivedMessageBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerSendMessageBinding;
import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;

// Adaptor xử lí các thay đổi liên quan đến Chat RecyclerView(lịch sử tin nhắn giữa 2 user)
// cập nhật và đính dữ liệu vào các viewHolder, hiển thị lên cho người dùng
public class ChatAdaptor extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    // Danh sách tin nhắn giữa 2 user
    private final ArrayList<ChatMessage> chatMessages;
    // Ảnh người được nhận tin nhắn
    private Bitmap receiverProfileImage;
    // Id người gửi tin nhắn
    private final String senderId;

    // Do Chat RecyclerView được xem từ 2 phía nên sẽ có 2 kiểu thiết kế viewHolder để hiển thị tin nhắn
    // 1 cho người gửi, 1 cho người nhận
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public void setReceiverProfileImage(Bitmap bitmap) {
        receiverProfileImage = bitmap;
    }

    // Constructor nhận vào danh sách tin nhắn giữa 2 user, hình ảnh người nhận, id người gửi
    public ChatAdaptor(ArrayList<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    // Tạo viewHolder
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nếu là người gửi, trả về thiết kế viewHolder của người gửi
        // Nếu là người nhận, trả về thiết kế viewHolder của người nhận
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    ItemContainerSendMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    // Đính dữ liệu vào viewHolder, sẽ gọi hàm đính dữ liệu tuỳ theo trường hợp
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        } else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position), receiverProfileImage);
        }
    }

    // Trả về số lượng/kích thước tin nhắn giữa 2 user
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    // Xác định và trả về kiểu VIEW_TYPE tương ứng, sử dụng để xác định viewHolder được sử dụng
    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    // Class ViewHolder của người gửi
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSendMessageBinding binding;
        // binding thiết kế ViewHolder
        SentMessageViewHolder(ItemContainerSendMessageBinding itemContainerSendMessageBinding) {
            super(itemContainerSendMessageBinding.getRoot());
            binding = itemContainerSendMessageBinding;
        }

        // Đính dữ liệu vào viewHolder tuỳ theo trường hợp tin nhắn dạng text, image hay audio
        void setData(ChatMessage chatMessage) {
            if (chatMessage.type.equals("text")) {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.textMessage.setText(chatMessage.message);
            } else if (chatMessage.type.equals("image")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.VISIBLE);
                Picasso.get().load(chatMessage.message).into(binding.imageMessage);
            } else if (chatMessage.type.equals("audio")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.VISIBLE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.audioMessage.setAudio(chatMessage.message);
            }
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }

    // Class ViewHolder của người nhận
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;
        // binding thiết kế ViewHolder
        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        // Đính dữ liệu vào viewHolder tuỳ theo trường hợp tin nhắn dạng text, image hay audio
        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            if (chatMessage.type.equals("text")) {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.textMessage.setText(chatMessage.message);
            } else if (chatMessage.type.equals("image")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.VISIBLE);
                Picasso.get().load(chatMessage.message).into(binding.imageMessage);
            } else if (chatMessage.type.equals("audio")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.VISIBLE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.audioMessage.setAudio(chatMessage.message);
            }
            binding.textDateTime.setText(chatMessage.dateTime);
            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }
}
