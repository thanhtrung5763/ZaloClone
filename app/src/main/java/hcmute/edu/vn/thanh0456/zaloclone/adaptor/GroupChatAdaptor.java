package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerGroupReceivedMessageBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerGroupSendMessageBinding;
import hcmute.edu.vn.thanh0456.zaloclone.models.GroupMessage;

// Adaptor xử lí các thay đổi liên quan đến Group Chat RecyclerView(lịch sử tin nhắn trong group)
// cập nhật và đính dữ liệu vào các viewHolder, hiển thị lên cho người dùng
public class GroupChatAdaptor extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    // Danh sách tin nhắn trong group
    private final ArrayList<GroupMessage> groupMessages;
    // Ảnh của người nhận tin nhắn
    private Bitmap receiverProfileImage;
    // Id người gửi tin nhắn
    private final String senderId;

    // Do Group Chat RecyclerView được xem từ 2 phía nên sẽ có 2 kiểu thiết kế viewHolder để hiển thị tin nhắn
    // 1 cho người gửi, 1 cho người nhận
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    private Bitmap getBitmapFromEncodedImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    // Constructor nhận vào danh sách tin nhắn của group, id người gửi tin nhắn
    public GroupChatAdaptor(ArrayList<GroupMessage> groupMessages, String senderId) {
        this.groupMessages = groupMessages;
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
                    ItemContainerGroupSendMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemContainerGroupReceivedMessageBinding.inflate(
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
            ((SentMessageViewHolder) holder).setData(groupMessages.get(position));
        } else {
            ((ReceivedMessageViewHolder) holder).setData(groupMessages.get(position), receiverProfileImage);
        }
    }

    // Trả về số lượng/kích thước tin nhắn của group
    @Override
    public int getItemCount() {
        return groupMessages.size();
    }

    // Xác định và trả về kiểu VIEW_TYPE tương ứng, sử dụng để xác định viewHolder được sử dụng
    @Override
    public int getItemViewType(int position) {
        if (groupMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            receiverProfileImage = groupMessages.get(position).imageBitmap;
            return VIEW_TYPE_RECEIVED;
        }
    }

    // Class ViewHolder của người gửi
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerGroupSendMessageBinding binding;
        // binding thiết kế ViewHolder
        SentMessageViewHolder(ItemContainerGroupSendMessageBinding itemContainerGroupSendMessageBinding) {
            super(itemContainerGroupSendMessageBinding.getRoot());
            binding = itemContainerGroupSendMessageBinding;
        }

        // Đính dữ liệu vào viewHolder tuỳ theo trường hợp tin nhắn dạng text, image hay audio
        void setData(GroupMessage groupMessage) {
            if (groupMessage.type.equals("text")) {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.textMessage.setText(groupMessage.message);
            } else if (groupMessage.type.equals("image")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.VISIBLE);
                Picasso.get().load(groupMessage.message).into(binding.imageMessage);
            } else if (groupMessage.type.equals("audio")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.VISIBLE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.audioMessage.setAudio(groupMessage.message);
            }
            binding.textDateTime.setText(groupMessage.dateTime);
        }
    }

    // Class ViewHolder của người nhận
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerGroupReceivedMessageBinding binding;

        // binding thiết kế ViewHolder
        ReceivedMessageViewHolder(ItemContainerGroupReceivedMessageBinding itemContainerGroupReceivedMessageBinding) {
            super(itemContainerGroupReceivedMessageBinding.getRoot());
            binding = itemContainerGroupReceivedMessageBinding;
        }

        // Đính dữ liệu vào viewHolder tuỳ theo trường hợp tin nhắn dạng text, image hay audio
        void setData(GroupMessage groupMessage, Bitmap receiverProfileImage) {
            if (groupMessage.type.equals("text")) {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.textMessage.setText(groupMessage.message);
            } else if (groupMessage.type.equals("image")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.VISIBLE);
                Picasso.get().load(groupMessage.message).into(binding.imageMessage);
            } else if (groupMessage.type.equals("audio")) {
                binding.textMessage.setVisibility(View.GONE);
                binding.audioMessage.setVisibility(View.VISIBLE);
                binding.imageMessage.setVisibility(View.GONE);
                binding.audioMessage.setAudio(groupMessage.message);
            }
            binding.textDateTime.setText(groupMessage.dateTime);
            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }

}
