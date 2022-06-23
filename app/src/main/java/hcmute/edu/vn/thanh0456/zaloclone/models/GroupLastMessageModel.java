package hcmute.edu.vn.thanh0456.zaloclone.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

// Tạo instance lưu dữ liệu để đẩy lên Firestore
// hoặc kéo dữ liệu từ Firestore và lưu vào instance để sử dụng
public class GroupLastMessageModel implements Parcelable {
    // id người gửi, tin nhắn gửi, kiểu tin nhắn, thời gian gửi
    public String senderId, message, type, dateTime;
    // Thời gian gửi
    public Date dateObject;
    public GroupLastMessageModel() {
    }

    protected GroupLastMessageModel(Parcel in) {
        senderId = in.readString();
        message = in.readString();
        type = in.readString();
        dateTime = in.readString();
        dateObject = new Date(in.readLong());
    }

    public static final Creator<GroupLastMessageModel> CREATOR = new Creator<GroupLastMessageModel>() {
        @Override
        public GroupLastMessageModel createFromParcel(Parcel in) {
            return new GroupLastMessageModel(in);
        }

        @Override
        public GroupLastMessageModel[] newArray(int size) {
            return new GroupLastMessageModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(senderId);
        parcel.writeString(message);
        parcel.writeString(type);
        parcel.writeString(dateTime);
        parcel.writeLong(dateObject.getTime());
    }
}
