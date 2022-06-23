package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityVideoCallingInComingBinding;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIClient;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIService;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Activity bên phía người được mời
public class VideoCallingInComingActivity extends AppCompatActivity {

    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    ActivityVideoCallingInComingBinding binding;
    // instance của Firestore để tương tác với dữ liệu khi cần
    FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallingInComingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        loadSenderDetails();
        setListeners();
    }

    // Hàm khởi tạo
    private void init() {
        String meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        database = FirebaseFirestore.getInstance();
    }

    // Hàm thiết lập event
    private void setListeners() {
        // Chọn chấp nhận hoặc từ chối tham gia cuộc gọi, gửi trả response về cho người mời
        binding.fabAccept.setOnClickListener(v -> {
            sendInvitationResponse(
                    Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                    getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
            );
        });
        binding.fabDecline.setOnClickListener(v -> {
            sendInvitationResponse(
                    Constants.REMOTE_MSG_INVITATION_REJECTED,
                    getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
            );
        });
    }

    // Hàm xử lí gửi trả response về cho người mời
    private void sendInvitationResponse(String type, String receiverToken) {
        try {
            // Thiết lập dữ liệu
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            // Gửi trả dữ liệu về cho người mời
            sendRemoteMessage(body.toString(), type);

        } catch (Exception e) {
            showToast(e.getMessage());
            finish();
        }
    }

    // Hàm xử lí gửi dữ liệu lại cho người mời
    private void sendRemoteMessage(String remoteMessageBody, String type) {
        APIClient.getClient().create(APIService.class).sendMessage(
                Constants.getRemoteMsgHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    // Nếu chấp nhận, tạo cuộc gọi video call giữa 2 người
                    if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                        showToast("Invitation Accepted");

                        try {
                            // Thiết lập dữ liệu để tạo lập cuộc gọi video call
                            // Kết thúc activity hiện tại, chuyển tới giao diện cuộc gọi
                            URL serverURL = new URL("https://meet.jit.si");
                            JitsiMeetConferenceOptions conferenceOptions = new JitsiMeetConferenceOptions.Builder()
                                    .setServerURL(serverURL)
                                    .setWelcomePageEnabled(false)
                                    .setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM))
                                    .build();
                            JitsiMeetActivity.launch(VideoCallingInComingActivity.this, conferenceOptions);
                            finish();
                        } catch (Exception e) {
                            // Nếu xảy ra lỗi trong quá trình khởi tạo cuộc gọi, hiển thị và kết thúc activity
                            // trở về trang trước
                            showToast(e.getMessage());
                            finish();
                        }

                    } else {
                        // Nếu từ chối,hiển thị và kết thúc activity
                        // trở về trang trước
                        showToast("Invitation Rejected");
                        finish();
                    }
                } else {
                    // Nếu response trả về có lỗi, hiển thị và kết thúc activity
                    // trở về trang trước
                    showToast(response.message());
                    finish();
                }
            }
            // Xử lí quá trình gửi lời mời thất bại
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Hiển thị lỗi, kết thúc và quay về trang trước
                showToast(t.getMessage());
                finish();
            }
        });
    }

    // Nhận tín hiệu phản hồi lời mời của người gửi
    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)) {
                showToast("Invitation Cancelled");
                finish();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // Đăng kí hàm sẽ nhận dữ liệu là tín hiệu phản hồi lời mời từ người gửi
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    // Huỷ đăng kí hàm nhận dữ liệu khi activity bị dừng
    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }

    // Tải dữ liệu người gửi và hiển thị lên giao diện
    private void loadSenderDetails() {
        binding.textName.setText(getIntent().getStringExtra(Constants.KEY_NAME));
        String senderId = getIntent().getStringExtra(Constants.KEY_USER_ID);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(senderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(documentSnapshot.getString(Constants.KEY_IMAGE)));
                    }
                });
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
        Toast.makeText(VideoCallingInComingActivity.this, message, Toast.LENGTH_SHORT).show();
    }

}