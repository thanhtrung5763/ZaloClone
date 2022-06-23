package hcmute.edu.vn.thanh0456.zaloclone.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

import hcmute.edu.vn.thanh0456.zaloclone.R;
import hcmute.edu.vn.thanh0456.zaloclone.activities.ChatActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.VideoCallingInComingActivity;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;


// Dịch vụ gửi tin nhắn, dữ liệu, thông báo đến thiết bị khác
public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
// TODO(developer): Handle FCM messages here.
//        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
//        Log.d("FCM", "From: " + message.getFrom());
//
//        // Check if message contains a data payload.
//        if (message.getData().size() > 0) {
//            Log.d("FCM", "Message data payload: " + message.getData());
//
//            if (/* Check if data needs to be processed by long running job */ true) {
//                // For long-running tasks (10 seconds or more) use WorkManager.
//                Log.d("FCM", "Check if data needs to be processed by long running job: " + message.getData());
//            } else {
//                // Handle message within 10 seconds
//                Log.d("FCM", "Handle message within 10 seconds" + message.getData());
//            }
//        }
//
//        // Check if message contains a notification payload.
//        if (message.getNotification() != null) {
//            Log.d("FCM", "Message Notification Body: " + message.getNotification().getBody());
//        }
        String type = message.getData().get(Constants.REMOTE_MSG_TYPE);
        // Nếu type khác null, gửi lời mời tham gia video call đến thiết bị khác kèm theo dữ liệu người mời để hiển thị lên UI
        if (type != null) {
            if(type.equals(Constants.REMOTE_MSG_INVITATION)) {
                Intent intent = new Intent(getApplicationContext(), VideoCallingInComingActivity.class);
                // Thiết lập dữ liệu người mời
                intent.putExtra(
                        Constants.REMOTE_MSG_MEETING_TYPE,
                        message.getData().get(Constants.REMOTE_MSG_MEETING_TYPE)
                );
                intent.putExtra(
                        Constants.KEY_USER_ID,
                        message.getData().get(Constants.KEY_USER_ID)
                );
                intent.putExtra(
                        Constants.KEY_NAME,
                        message.getData().get(Constants.KEY_NAME)
                );
                intent.putExtra(
                        Constants.REMOTE_MSG_INVITER_TOKEN,
                        message.getData().get(Constants.REMOTE_MSG_INVITER_TOKEN)
                );
                intent.putExtra(
                        Constants.REMOTE_MSG_MEETING_ROOM,
                        message.getData().get(Constants.REMOTE_MSG_MEETING_ROOM)
                );
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                // Người được mời sẽ dược chuyển đến activity VideoCallingInComingActivity
                // Người mời sẽ chuyển đến activity VideoCallingOutgoingActivity và chờ đợi phản hồi từ người được mời
                // hoặc bản thân người được mời có thể huỷ lời mời
                startActivity(intent);
            // Xử lí tín hiệu response từ người đươc mời(từ chối hoặc chấp nhận)
            // hoặc từ người mời(huỷ lời mời)
            } else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)) {
                Intent intent = new Intent(Constants.REMOTE_MSG_INVITATION_RESPONSE);
                intent.putExtra(
                        Constants.REMOTE_MSG_INVITATION_RESPONSE,
                        message.getData().get(Constants.REMOTE_MSG_INVITATION_RESPONSE)
                );
                // Gửi trả tín hiệu response đến intent tương ứng
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        }
        // Xử lí việc gửi thông báo có tin nhắn đến thiết bị khác, nếu người đó đang không hoạt động trong ứng dụng
        else {
            User user = new User();
            user.id = message.getData().get(Constants.KEY_USER_ID);
            user.name = message.getData().get(Constants.KEY_NAME);
            user.token = message.getData().get(Constants.KEY_FCM_TOKEN);

            int notificationId = new Random().nextInt(200) + 100;
            String channelId = "chat_message";

            Intent intent = new Intent(this, ChatActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra(Constants.KEY_USER, user);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId);
            builder.setSmallIcon(R.drawable.ic_baseline_notifications_24);
            builder.setContentTitle(user.name);
            builder.setContentText(message.getData().get(Constants.KEY_MESSAGE));
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(
                    message.getData().get(Constants.KEY_MESSAGE)
            ));
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            builder.setContentIntent(pendingIntent);
            builder.setAutoCancel(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence channelName = "Chat Message";
                String channelDescription = "This notification channel is used for chat message notifications";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
                channel.setDescription(channelDescription);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
                notificationManagerCompat.notify(notificationId, builder.build());
            } else {
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(notificationId, builder.build());
            }
        }



        super.onMessageReceived(message);
    }
}
