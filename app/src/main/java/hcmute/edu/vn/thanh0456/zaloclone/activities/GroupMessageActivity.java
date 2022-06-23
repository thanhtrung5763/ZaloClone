package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.devlomi.record_view.OnRecordListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.thanh0456.zaloclone.R;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.ChatAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.GroupChatAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.RecentGroupAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityGroupMessageBinding;
import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.Group;
import hcmute.edu.vn.thanh0456.zaloclone.models.GroupLastMessageModel;
import hcmute.edu.vn.thanh0456.zaloclone.models.GroupMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Permissions;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

public class GroupMessageActivity extends AppCompatActivity {
    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    private ActivityGroupMessageBinding binding;
    // instance của Firestore để tương tác với dữ liệu khi cần
    private FirebaseFirestore database;
    // Lưu hoặc lấy dữ liệu trong SharePref
    private PreferenceManager preferenceManager;
    // instance lưu dữ liệu liên quan đến group
    private Group group;
    // instance lưu dữ liệu liên quan đến tin nhắn mới nhất trong group
    GroupLastMessageModel lastMessageModel;
    // Lưu lịch sử tin nhắn trong group
    private ArrayList<GroupMessage> groupMessages;
    // Adaptor
    private GroupChatAdaptor groupChatAdaptor;
    // id của đoạn tin nhắn gửi đi trong group
    private String groupMessageId = null;
    // Đường dẫn đến ảnh, audio lưu trên Storage của Firestore
    private String imageURL = null, audioURL = null;
    // Đẩy ảnh, audio lên Storage
    private StorageTask uploadTask;
    // Uri từ đường dẫn đến ảnh, audio lưu trên thiết bị
    private Uri imageUri, audioUri;
    // Tạo recording
    private MediaRecorder mediaRecorder;
    // Đường dẫn đến file recording
    private String audioPath;
    // Tạm dừng hiển thị UI bottom chat, đợi animation cancel recording hoàn thành
    Handler handlerUI = new Handler();
    // Lưu danh sách thành viên và ảnh đại diện trong group
    private ArrayList<HashMap<String, Object>> memberAndImageHashMapList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadGroupDetails();
        setListeners();
        init();
        listenMessages();
    }

    // Hàm khởi tại
    private void init() {
        // Lưu hoặc lấy dữ liệu từ SharePrefs
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Lưu tin nhắn của group
        groupMessages = new ArrayList<>();
        // Thiết lập adaptor
        groupChatAdaptor = new GroupChatAdaptor(
                groupMessages,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        // Gắn adaptor vào RecyclerView
        binding.chatRecyclerView.setAdapter(groupChatAdaptor);
        // Tương tác với dữ liệu trên Firestore
        database = FirebaseFirestore.getInstance();
        // Thiết lập recordView cho nút record
        binding.recordButton.setRecordView(binding.recordView);
        binding.recordButton.setListenForRecord(false);
        // Lưu dữ liệu về tin nhắn mới nhất của group
        lastMessageModel = new GroupLastMessageModel();
    }

    // Hàm thiết lập event
    private void setListeners() {
        // Xử lý ẩn/hiện giao diện bottom chat dựa trên tin nhắn user nhập
        binding.inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (binding.inputMessage.getText().length() != 0) {
                    binding.imageCamera.setVisibility(View.GONE);
                    binding.imagePhoto.setVisibility(View.GONE);
                    binding.imageLike.setVisibility(View.GONE);
                    binding.imageShrink.setVisibility(View.VISIBLE);
                    binding.imageSend.setVisibility(View.VISIBLE);
                } else {
                    binding.imageCamera.setVisibility(View.VISIBLE);
                    binding.imagePhoto.setVisibility(View.VISIBLE);
                    binding.imageLike.setVisibility(View.VISIBLE);
                    binding.imageShrink.setVisibility(View.GONE);
                    binding.imageSend.setVisibility(View.GONE);
                }
            }
        });
        binding.imageShrink.setOnClickListener(view -> {
            binding.imageCamera.setVisibility(View.VISIBLE);
            binding.imagePhoto.setVisibility(View.VISIBLE);
            binding.imageShrink.setVisibility(View.GONE);
        });
        // Trở về trang trước
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // Gọi hàm xử lí gửi tin nhắn text
        binding.imageSend.setOnClickListener(v -> sendMessage());
        // Gọi hàm xử lí gửi tin nhắn ảnh
        binding.imagePhoto.setOnClickListener(v -> sendImage());
        // Gọi hàm xử lí chuyển đến trang xem thông tin group
        binding.imageInfo.setOnClickListener(v -> showInfoGroup());
        // Cấp quyền record cho ứng dụng
        binding.recordButton.setOnClickListener(v -> {
            if (Permissions.isRecordingok(GroupMessageActivity.this)) {
                binding.recordButton.setListenForRecord(true);
            } else {
                Permissions.requestRecording(GroupMessageActivity.this);
            }
        });
        // Ấn giữ nút record
        binding.recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                // Bắt đầu record
                Log.d("RecordView", "onStart");
                setUpRecording();
                binding.layoutBottomLeft.setVisibility(View.GONE);
                binding.inputMessage.setVisibility(View.GONE);
                binding.layoutLikeAndSend.setVisibility(View.GONE);
                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Hàm xử lí kéo từ phải sang trái để huỷ record
            @Override
            public void onCancel() {
                //On Swipe To Cancel
                Log.d("RecordView", "onCancel");

                mediaRecorder.reset();
                mediaRecorder.release();
                File file = new File(audioPath);
                if (file.exists()) {
                    file.delete();
                }
                handlerUI.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        binding.layoutBottomLeft.setVisibility(View.VISIBLE);
                        binding.inputMessage.setVisibility(View.VISIBLE);
                        binding.layoutLikeAndSend.setVisibility(View.VISIBLE);                    }
                }, 1150);
            }

            // Hàm xử lý thả nút record để kết thúc record
            @Override
            public void onFinish(long recordTime) {
                Log.d("RecordView", "onFinish");
                mediaRecorder.stop();
                mediaRecorder.release();
                binding.layoutBottomLeft.setVisibility(View.VISIBLE);
                binding.inputMessage.setVisibility(View.VISIBLE);
                binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
                sendRecordingMessage();
            }

            // Hàm xử lý nếu thời lượng record nhỏ hơn 1 giây thì huỷ file, không gửi
            @Override
            public void onLessThanSecond() {
                //When the record time is less than One Second
                Log.d("RecordView", "onLessThanSecond");

                mediaRecorder.reset();
                mediaRecorder.release();

                File file = new File(audioPath);
                if (file.exists()) {
                    file.delete();
                }

                binding.layoutBottomLeft.setVisibility(View.VISIBLE);
                binding.inputMessage.setVisibility(View.VISIBLE);
                binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
            }
        });
        // Thiết lập animation cho nút record
        binding.recordView.setOnBasketAnimationEndListener(() -> {
            Log.d("RecordView", "Basket Animation Finished");
            binding.layoutBottomLeft.setVisibility(View.VISIBLE);
            binding.inputMessage.setVisibility(View.VISIBLE);
            binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
        });
    }

    // Hàm xử lí chuyển đến trang xem thông tin group
    private void showInfoGroup() {
        Intent intent = new Intent(getApplicationContext(), GroupInfoActivity.class);
        intent.putExtra(Constants.KEY_COLLECTION_GROUP, group);
        startActivity(intent);
    }

    // Thiết lập source, format, encode cho file record, audioPath là đường dẫn đến file record
    private void setUpRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        File file = new File(Environment.getExternalStorageDirectory(). getAbsolutePath(), "Recordings");
//        if (!file.exists()) {
//            file.mkdirs();
//        }
        audioPath = getFilePath();

        mediaRecorder.setOutputFile(audioPath);
    }

    // Hàm thiết lập filepath để lưu file
    private String getFilePath() {
        ContextWrapper contextwrapper = new ContextWrapper(getApplicationContext());
        File recordPath = null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            recordPath = contextwrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        }
        File file  = new File(recordPath, System.currentTimeMillis() + ".3gp");
        return file.getPath();
    }

    // Hàm xử lí gửi tin nhắn record
    private void sendRecordingMessage() {
        // Sau khi record, lấy uri từ audioPath, đẩy file record lên Storage của Firebase, trong thư mục Audio Files
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Audio Files");
        audioUri = Uri.fromFile(new File(audioPath));

        DocumentReference ref = database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE).document();
        groupMessageId = ref.getId();

        StorageReference audioPathOnFireBase = storageReference.child(groupMessageId + "." + "3gp");
        uploadTask = audioPathOnFireBase.putFile(audioUri);
        uploadTask.continueWithTask(task -> {
            // Nếu thất bại, hiển thị lỗi
            if (!task.isSuccessful()) {
                showToast(task.getException().getMessage());
                throw task.getException();
            }
            // Nếu thành công, trả về URL đến file record trên Storage
            return audioPathOnFireBase.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Thiết lập dữ liệu cần thiết cho một document trong collection group
                Uri downloadURL = (Uri) task.getResult();
                audioURL = downloadURL.toString();

                lastMessageModel.dateObject = new Date();
                lastMessageModel.message = audioURL;
                lastMessageModel.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
                lastMessageModel.type = "audio";

                HashMap<String, Object> message = new HashMap<>();
                message.put(Constants.KEY_SENDER_ID, lastMessageModel.senderId);
                message.put(Constants.KEY_MESSAGE, lastMessageModel.message);
                message.put(Constants.KEY_TYPE, lastMessageModel.type);
                message.put(Constants.KEY_TIMESTAMP, lastMessageModel.dateObject);

                // Cập nhật tin nhắn mới nhất của group lên Firestore trong subcollection lastMessage trong collection group
                database.collection(Constants.KEY_COLLECTION_GROUP)
                        .document(group.id)
                        .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                        .document(group.id)
                        .update(message);

                message.put(Constants.KEY_GROUP_ID, group.id);
                // Tạo một document trong collection groupMessage với id là group.id, đẩy dữ liệu vào trong document
                database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE)
                        .document(groupMessageId)
                        .set(message)
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                showToast("Audio sent successfully");
                            } else {
                                showToast(task1.getException().getMessage());
                            }
                        });

                binding.inputMessage.setText(null);
            }
        });
    }

    // Hàm xử lí gửi tin nhắn ảnh
    private void sendImage() {
        // Chọn ảnh
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        // Hàm xử lí sau khi chọn ảnh
        startActivityForResult(intent.createChooser(intent, "Select Image"), 438);
    }

    // Hàm xử lí sau khi chọn ảnh
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // Lấy uri từ ảnh, đẩy lên Storage của Firebase, lưu trong thư mục "Image Files"
            imageUri = data.getData();
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");

            DocumentReference ref = database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE).document();
            groupMessageId = ref.getId();

            StorageReference imagePath = storageReference.child(groupMessageId + "." + "jpg");
            uploadTask = imagePath.putFile(imageUri);
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    showToast(task.getException().getMessage());
                    throw task.getException();
                }
                // Nếu đẩy ảnh lên thành công, trả về URL đến ảnh trên Storage
                return imagePath.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Lấy URL trả về, lưu dưới dạng String
                    Uri downloadURL = (Uri) task.getResult();
                    imageURL = downloadURL.toString();

                    lastMessageModel.dateObject = new Date();
                    lastMessageModel.message = imageURL;
                    lastMessageModel.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
                    lastMessageModel.type = "image";

                    // Thiết lập dữ liệu cho 1 document trong subcollection lastMessage trong collection group
                    HashMap<String, Object> message = new HashMap<>();
                    message.put(Constants.KEY_SENDER_ID, lastMessageModel.senderId);
                    message.put(Constants.KEY_MESSAGE, lastMessageModel.message);
                    message.put(Constants.KEY_TYPE, lastMessageModel.type);
                    message.put(Constants.KEY_TIMESTAMP, lastMessageModel.dateObject);

                    // Cập nhật dữ liệu được thiết lập lên Firestore trong subcollection lastMessage trong collection group
                    // dữ liệu liên quan đến tin nhắn mới nhất của group
                    database.collection(Constants.KEY_COLLECTION_GROUP)
                            .document(group.id)
                            .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                            .document(group.id)
                            .update(message);

                    message.put(Constants.KEY_GROUP_ID, group.id);
                    // Tạo một document trong collection groupMessage với id là group.id, đẩy dữ liệu vào trong document
                    database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE)
                            .document(groupMessageId)
                            .set(message)
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    showToast("Image sent successfully");
                                } else {
                                    showToast(task1.getException().getMessage());
                                }
                            });

//                    updateLastMessage(message);
                    binding.inputMessage.setText(null);
                }
            });
        }
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

    // Tải thông tin cơ bản của group như ảnh, tên, danh sách thành viên
    // và hiển thị lên giao diện
    private void loadGroupDetails() {
        group = getIntent().getParcelableExtra(Constants.KEY_COLLECTION_GROUP);
        binding.textName.setText(group.name);
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(group.image));
        memberAndImageHashMapList = new ArrayList<>();
        for (User user : group.members) {
            HashMap<String, Object> memberAndImageHashMap = new HashMap<>();
            memberAndImageHashMap.put(Constants.KEY_USER_ID, user.id);
            memberAndImageHashMap.put(Constants.KEY_IMAGE, getBitmapFromEncodedImage(user.image));
            memberAndImageHashMapList.add(memberAndImageHashMap);
        }
    }

    // Hàm xử lí gửi tin nhắn dạng text
    private void sendMessage() {
        lastMessageModel.dateObject = new Date();
        lastMessageModel.message = binding.inputMessage.getText().toString();
        lastMessageModel.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
        lastMessageModel.type = "text";

        // Thiết lập dữ liệu cho một document trong subcollection lastMessage trong collection group
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, lastMessageModel.senderId);
        message.put(Constants.KEY_MESSAGE, lastMessageModel.message);
        message.put(Constants.KEY_TYPE, lastMessageModel.type);
        message.put(Constants.KEY_TIMESTAMP, lastMessageModel.dateObject);

        // Cập nhật dữ liệu được thiết lập lên Firestore trong subcollection lastMessage trong collection group
        // dữ liệu liên quan đến tin nhắn mới nhất của group
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                .document(group.id)
                .update(message);

        message.put(Constants.KEY_GROUP_ID, group.id);
        // Tạo một document trong collection groupMessage với id là group.id, đẩy dữ liệu vào trong document
        database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE)
                .add(message);


        group.lastMessageModel = lastMessageModel;
        // Cập nhật RecyclerView
        RecentGroupAdaptor.updateLastMessage(group);
        binding.inputMessage.setText(null);
    }

    // Hàm xử lí theo dõi sự thay đổi về tin nhắn trong group
    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE)
                .whereEqualTo(Constants.KEY_GROUP_ID, group.id)
                .addSnapshotListener(eventListener);
    }

    // Cập nhật và hiển thị tin nhắn theo real-time
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = groupMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    GroupMessage groupMessage = new GroupMessage();
                    groupMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);

                    // Chuyển đổi ảnh đại diện của thành viên trong group dạng String sang Bitmap
                    // Lưu trữ trong thuộc tính imageBitmap
                    // Được sử dụng để hiển thị kèm theo đoạn tin nhắn thành viên đó gửi
                    for (HashMap<String, Object> hashMap : memberAndImageHashMapList) {
                        if (hashMap.get(Constants.KEY_USER_ID).equals(groupMessage.senderId)) {
                            groupMessage.imageBitmap = (Bitmap) hashMap.get(Constants.KEY_IMAGE);
                            break;
                        }
                    }
                    // Thiết lập giá trị cho các thuộc tính của instance groupMessage
                    groupMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    groupMessage.type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                    groupMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    groupMessage.dateTime = getReadableDateTime(groupMessage.dateObject);
                    // Thêm instance vào danh sách tin nhắn
                    groupMessages.add(groupMessage);
                }
            }
            // Sắp xếp các tin nhắn theo thứ tự thời gian
            Collections.sort(groupMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                // Cập nhật RecyclerView
                groupChatAdaptor.notifyDataSetChanged();
            } else {
                // Cập nhật RecyclerView
                groupChatAdaptor.notifyItemRangeChanged(groupMessages.size(), groupMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(groupMessages.size() - 1);
                groupChatAdaptor.notifyDataSetChanged();
            }

            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
//        if (conversationId == null) {
//            checkForConversation();
//        }
    };

    // Chuyển thời gian dạng Date sang String
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM, dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    // Hàm xử lí hiển thị đoạn thông báo
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Hàm nhận dữ liệu nếu group có sự thay đổi như tên, ảnh, số lượng thành viên
    // cập nhật trạng thái group lên version mới nhất
    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Group changedGroup = intent.getParcelableExtra(Constants.KEY_COLLECTION_GROUP);
            if (changedGroup != null) {
                if (changedGroup.image != null && !changedGroup.image.equals(group.image)) {
                    binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(changedGroup.image));
                    showToast("Changed Image");
                }
                if (changedGroup.name != null && !changedGroup.name.equals(group.name)) {
                    binding.textName.setText(changedGroup.name);
                    showToast("Changed Name");
                }
                group = changedGroup;
                showToast("Changed Group");
            }
        }
    };

    // Khi bắt đầu activity, đăng kí hàm sẽ nhận dữ liệu truyền về từ một activity khác
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.KEY_COLLECTION_GROUP)
        );
    }


    // Huỷ đăng kí hàm nhận dữ liệu khi activity bị dừng
    @Override
    protected void onDestroy() {
        // Unregister when activity finished
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
        super.onDestroy();
    }

}