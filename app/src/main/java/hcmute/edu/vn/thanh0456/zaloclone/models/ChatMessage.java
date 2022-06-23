package hcmute.edu.vn.thanh0456.zaloclone.models;

import java.util.Date;

// Class lưu dữ liệu liên quan đến tin nhắn giữa 2 user
public class ChatMessage {
    // id người gửi, id người nhận, tin nhắn gửi đi, thời gian gửi, kiểu tin nhắn gửi(text, image, audio)
    public String senderId, receiverId, message, dateTime, type;
    // Thời gian gửi
    public Date dateObject;
    // id của cuộc trò chuyện giữa 2 user, tên cuộc trò chuyện, ảnh cuộc trò chuyển(tên và ảnh đều là của người nhận)
    public String conversationId, conversationName, conversationImage;
}
