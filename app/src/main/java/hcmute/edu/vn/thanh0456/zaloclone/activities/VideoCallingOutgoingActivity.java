package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceIdReceiver;
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityVideoCallingOutgoingBinding;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIClient;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIService;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Activity bên phía người mời
public class VideoCallingOutgoingActivity extends AppCompatActivity {

    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    ActivityVideoCallingOutgoingBinding binding;
    // Lưu dữ liệu liên quan đến người được mời
    private User receivedUser;
    // Lưu hoặc lấy dữ liệu trong SharePref
    private PreferenceManager preferenceManager;
    // meetingRoom ID
    String meetingRoom = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallingOutgoingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        loadReceiverDetails();
        setListeners();
        String meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        if (meetingType != null && receivedUser != null) {
            initiateMeeting(meetingType, receivedUser.token);
        }
    }

    // Hàm khởi tạo
    private void init() {
        // Lưu hoặc lấy dữ liệu từ SharePrefs
        preferenceManager = new PreferenceManager(getApplicationContext());
    }

    // Hàm thiết lập event
    private void setListeners() {
        binding.fabDecline.setOnClickListener(v -> {
            if (receivedUser != null) {
                cancelInvitation(receivedUser.token);
            }
        });
    }

    // Hàm xử lí tạo và gửi lời mời tham gia video call tới người nhận
    private void initiateMeeting(String meetingType, String receiverToken) {
        try {
            // Thiết lập dữ liệu cần thiết
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));

            meetingRoom = preferenceManager.getString(Constants.KEY_USER_ID)
                    + '_'
                    + UUID.randomUUID().toString().substring(0, 5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            // Gửi lời mời tham gia video call
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);

        } catch (Exception e) {
            showToast(e.getMessage());
            finish();
        }
    }

    // Hàm xử lí gửi lời mời tham gia video call
    private void sendRemoteMessage(String remoteMessageBody, String type) {
        // Tạo một API, gửi thông báo đến thiết bị khác
        APIClient.getClient().create(APIService.class).sendMessage(
                Constants.getRemoteMsgHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            // Xử lí response trả về
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    if (type.equals(Constants.REMOTE_MSG_INVITATION)) {
                        // Nếu lời mời được gửi đi thành công, hiển thị thông báo
                        showToast("Invitation send successfully");
                    } else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)) {
                        // Nếu người nhận huỷ, không tham gia
                        // hiển thị thông báo, kết thúc và quay về trang chatting
                        showToast("Invitation cancelled");
                        finish();
                    }
                } else {
                    // Nếu response có lỗi, hiển thị và kết thúc, quay về trang chatting
                    showToast(response.message());
                    finish();
                }
            }

            // Xử lí quá trình gửi lời mời thất bại
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Hiển thị lỗi, kết thúc và quay về trang chatting
                showToast(t.getMessage());
                finish();
            }
        });
    }

    // Hàm xử lý huỷ lời mời tham gia
    private void cancelInvitation(String receiverToken) {
        try {
            // Thiết lập dữ liệu
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
            // Gửi lời mời tham gia video call
            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE);

        } catch (Exception e) {
            showToast(e.getMessage());
            finish();
        }
    }

    // Nhận tín hiệu phản hồi lời mời của người nhận
    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            // Nếu người nhận lời mời đồng ý tham gia, tạo một cuộc gọi video call
            // ngược lại, kết thúc và trở về trang chatting
            if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                showToast("Invitation Accepted");
                try {
                    // Thiết lập dữ liệu để tạo lập cuộc gọi video call
                    // Đóng activity hiện tại, chuyển tới giao diện cuộc gọi
                    URL serverURL = new URL("https://meet.jit.si");
                    JitsiMeetConferenceOptions conferenceOptions = new JitsiMeetConferenceOptions.Builder()
                            .setServerURL(serverURL)
                            .setWelcomePageEnabled(false)
                            .setRoom(meetingRoom)
                            .build();
                    JitsiMeetActivity.launch(VideoCallingOutgoingActivity.this, conferenceOptions);
                    finish();
                } catch (Exception e) {
                    // Nếu xảy ra lỗi trong quá trình khởi tạo cuộc gọi, hiển thị và kết thúc activity
                    // trở về trang chatting
                    showToast(e.getMessage());
                    finish();
                }
            } else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)) {
                // Nếu người nhạn lời mời từ chối,hiển thị và kết thúc activity
                // trở về trang chatting
                showToast("Invitation Rejected");
                finish();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // Đăng kí hàm sẽ nhận dữ liệu là tín hiệu phản hồi lời mời từ người nhận
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Loại bỏ hàm đăng kí khỏi Broadcast
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }

    // Tải dữ liệu người nhận và hiển thị lên giao diện
    private void loadReceiverDetails() {
        receivedUser = getIntent().getParcelableExtra(Constants.KEY_USER);
        binding.textName.setText(receivedUser.name);
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(receivedUser.image));
    }

    // Hàm chuyển ảnh dạng String sang Bitmap
    private Bitmap getBitmapFromEncodedImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    // Hàm nhận chuỗi String và hiển thị
    private void showToast(String message) {
        Toast.makeText(VideoCallingOutgoingActivity.this, message, Toast.LENGTH_SHORT).show();
    }

}