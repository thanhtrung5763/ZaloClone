package hcmute.edu.vn.thanh0456.zaloclone.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

// Tạo instance lưu dữ liệu để đẩy lên Firestore
// hoặc kéo dữ liệu từ Firestore và lưu vào instance để sử dụng
public class User implements Parcelable {
    // Tên, ảnh, email, fcm_token, id, role của user
    public String name, image, email, token, id, role;

    protected User(Parcel in) {
        name = in.readString();
        image = in.readString();
        email = in.readString();
        token = in.readString();
        id = in.readString();
        role = in.readString();
    }

    public User() {

    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(image);
        parcel.writeString(email);
        parcel.writeString(token);
        parcel.writeString(id);
        parcel.writeString(role);
    }
}
