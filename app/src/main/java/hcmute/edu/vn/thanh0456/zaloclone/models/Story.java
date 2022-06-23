package hcmute.edu.vn.thanh0456.zaloclone.models;

import java.util.Date;

// Tạo instance lưu dữ liệu để đẩy lên Firestore
// hoặc kéo dữ liệu từ Firestore và lưu vào instance để sử dụng
public class Story {

    // URL đến ảnh lưu trên Firestore
    public String imageURL;
    // Thời gian tạo story
    public long timestamp;

    public Story() {
    }

    public Story(String imageURL, long timestamp) {
        this.imageURL = imageURL;
        this.timestamp = timestamp;
    }

}
