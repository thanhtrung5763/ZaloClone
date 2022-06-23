package hcmute.edu.vn.thanh0456.zaloclone.models;

import android.graphics.Bitmap;

import java.util.Date;

// Tạo instance lưu dữ liệu để đẩy lên Firestore
// hoặc kéo dữ liệu từ Firestore và lưu vào instance để sử dụng
public class GroupMessage {
    // Kiểu tin nhắn gửi, tin nhắn gửi, thời gian gửi, id người gửi, ảnh đại diện của người gửi
    public String type, message, dateTime, senderId, image;
    // Ảnh đại diện của người gửi
    public Bitmap imageBitmap;
    // Thời gian gửi
    public Date dateObject;

    public GroupMessage() {

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
