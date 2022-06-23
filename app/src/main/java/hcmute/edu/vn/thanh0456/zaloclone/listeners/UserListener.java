package hcmute.edu.vn.thanh0456.zaloclone.listeners;

import android.view.View;

import hcmute.edu.vn.thanh0456.zaloclone.models.User;

// Xử lí sự kiện khi user click vào một user bất kì
public interface UserListener {
    void onUserClicked(View v, User user);
}
