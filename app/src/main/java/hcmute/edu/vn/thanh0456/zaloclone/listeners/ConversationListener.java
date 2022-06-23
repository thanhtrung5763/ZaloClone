package hcmute.edu.vn.thanh0456.zaloclone.listeners;

import hcmute.edu.vn.thanh0456.zaloclone.models.User;

// Xử lí sự kiện khi user click vào một conversation bất kì
public interface ConversationListener {
    void onConversationClicked(User user);
}
