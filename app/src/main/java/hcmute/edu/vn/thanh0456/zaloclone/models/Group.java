package hcmute.edu.vn.thanh0456.zaloclone.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;

// Tạo instance lưu dữ liệu để đẩy lên Firestore
// hoặc kéo dữ liệu từ Firestore và lưu vào instance để sử dụng
public class Group implements Parcelable {
    // id nhóm, id người tạo, tên người tạo, ảnh nhóm, tên nhóm, thời gian tạo
    public String id, ownerId, ownerName, image, name, dateTime;
    // thời gian tạo
    public Date dateObject;
    // Danh sách thành viên
    public ArrayList<User> members;
    // Một instance lưu dữ liệu liên quan đến tin nhắn mới nhất trong group
    public GroupLastMessageModel lastMessageModel;

    public Group() {

    }

    protected Group(Parcel in) {
        id = in.readString();
        ownerId = in.readString();
        ownerName = in.readString();
        image = in.readString();
        name = in.readString();
        dateTime = in.readString();
        dateObject = new Date(in.readLong());
        members = in.createTypedArrayList(User.CREATOR);
        lastMessageModel = in.readParcelable(GroupLastMessageModel.class.getClassLoader());
    }

    public static final Creator<Group> CREATOR = new Creator<Group>() {
        @Override
        public Group createFromParcel(Parcel in) {
            return new Group(in);
        }

        @Override
        public Group[] newArray(int size) {
            return new Group[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(ownerId);
        parcel.writeString(ownerName);
        parcel.writeString(image);
        parcel.writeString(name);
        parcel.writeString(dateTime);
        parcel.writeLong(dateObject.getTime());
        parcel.writeTypedList(members);
        parcel.writeParcelable(lastMessageModel, i);
    }
}
