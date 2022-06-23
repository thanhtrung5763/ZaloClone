package hcmute.edu.vn.thanh0456.zaloclone.models;

import java.util.ArrayList;

// Tạo instance lưu dữ liệu để đẩy lên Firestore
// hoặc kéo dữ liệu từ Firestore và lưu vào instance để sử dụng
public class UserStory {

    // Tên người tạo story, ảnh người tạo story
    public String name, image;
    // Thời gian tạo story mới nhất
    public long lastUpdated;
    // Danh sách story của user đó
    public ArrayList<Story> stories;

    public UserStory(String name, String image, long lastUpdated, ArrayList<Story> stories) {
        this.name = name;
        this.image = image;
        this.lastUpdated = lastUpdated;
        this.stories = stories;
    }

    public UserStory() {

    }


}
