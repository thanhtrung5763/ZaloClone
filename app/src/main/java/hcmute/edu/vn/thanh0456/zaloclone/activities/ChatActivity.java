package hcmute.edu.vn.thanh0456.zaloclone.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContextWrapper;
import android.content.Intent;
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

import com.devlomi.record_view.OnBasketAnimationEnd;
import com.devlomi.record_view.OnRecordListener;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import hcmute.edu.vn.thanh0456.zaloclone.R;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.ChatAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ActivityChatBinding;
import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIClient;
import hcmute.edu.vn.thanh0456.zaloclone.network.APIService;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Permissions;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


// Xử lí giao diện chat 1-1
// extends BaseActivity để theo dõi và cập nhật tình trạng on/off của người dùng
public class ChatActivity extends BaseActivity {

    // Binding thiết kế layout và hiển thị(đỡ phải tạo các biến để lấy các thuộc tính trong layout và sử dụng)
    private ActivityChatBinding binding;
    // Lưu dữ liệu người nhận
    private User receivedUser;
    // Lưu hoặc lấy dữ liệu trong SharePrefs
    private PreferenceManager preferenceManager;
    // Lịch sử tin nhắn
    private ArrayList<ChatMessage> chatMessages;
    // Adaptor
    private ChatAdaptor chatAdaptor;
    // Tương tác với dữ liệu trên Firestore
    private FirebaseFirestore database;
    // id cuộc trò chuyện giữa 2 user
    private String conversationId = null;
    // tình trạng on/off của user
    private Boolean isReceiverAvailaible = false;
    // id của đoạn tin nhắn
    private String chatId = null;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadReceiverDetails();
        setListeners();
        init();
        listenMessages();
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
        // Gửi lời mời cuộc gọi thoại đến user, hiển thị giao diện chờ phản hồi từ người được mời
        binding.imageVideo.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, VideoCallingOutgoingActivity.class);
            intent.putExtra(Constants.KEY_USER, receivedUser);
            intent.putExtra(Constants.REMOTE_MSG_MEETING_TYPE, "video");
            startActivity(intent);
        });
        // Cấp quyền record cho ứng dụng
        binding.recordButton.setOnClickListener(v -> {
            if (Permissions.isRecordingok(ChatActivity.this)) {
                binding.recordButton.setListenForRecord(true);
            } else {
                Permissions.requestRecording(ChatActivity.this);
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

    // Hàm khởi tạo
    private void init() {
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(receivedUser.image));
        preferenceManager = new PreferenceManager(getApplicationContext());
        // Lưu tin nhắn giữa 2 user
        chatMessages = new ArrayList<>();
        // Thiết lập adaptor
        chatAdaptor = new ChatAdaptor(
                chatMessages,
                getBitmapFromEncodedImage(receivedUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        // Gắn adaptor vào RecyclerView
        binding.chatRecyclerView.setAdapter(chatAdaptor);
        // instance của Firestore để tương tác với dữ liệu khi cần
        database = FirebaseFirestore.getInstance();
        // Thiết lập RecordView cho nút record
        binding.recordButton.setRecordView(binding.recordView);
        binding.recordButton.setListenForRecord(false);
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

        DocumentReference ref = database.collection(Constants.KEY_COLLECTION_CHAT).document();
        chatId = ref.getId();

        StorageReference audioPathOnFireBase = storageReference.child(chatId + "." + "3gp");
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

                // Thiết lập dữ liệu cần thiết cho một document trong collection chat
                Uri downloadURL = (Uri) task.getResult();
                audioURL = downloadURL.toString();

                HashMap<String, Object> message = new HashMap<>();
                message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
                message.put(Constants.KEY_MESSAGE, audioURL);
                message.put(Constants.KEY_TYPE, "audio");
                message.put(Constants.KEY_TIMESTAMP, new Date());

                // Tạo một document trong collection chat với id là chatId, đẩy dữ liệu vào trong document
                database.collection(Constants.KEY_COLLECTION_CHAT)
                        .document(chatId)
                        .set(message)
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                showToast("Audio sent successfully");
                            } else {
                                showToast(task1.getException().getMessage());
                            }
                        });
                // Hàm cập nhật lastMessage
                updateLastMessage(message);
                binding.inputMessage.setText(null);
            }
        });
    }

    // Hàm gửi tin nhắn text
    private void sendMessage() {

        // Thiết lập dữ liệu cho một document trong collection chat
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TYPE, "text");
        message.put(Constants.KEY_TIMESTAMP, new Date());

        // Thêm dữ liệu vào collection chat
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .add(message);
        // Hàm cập nhật lastMessage
        updateLastMessage(message);
        binding.inputMessage.setText(null);
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

            DocumentReference ref = database.collection(Constants.KEY_COLLECTION_CHAT).document();
            chatId = ref.getId();

            StorageReference imagePath = storageReference.child(chatId + "." + "jpg");
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

                    // Thiết lập dữ liệu cho 1 document trong collection chat
                    HashMap<String, Object> message = new HashMap<>();
                    message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
                    message.put(Constants.KEY_MESSAGE, imageURL);
                    message.put(Constants.KEY_TYPE, "image");
                    message.put(Constants.KEY_TIMESTAMP, new Date());
                    // Lưu dữ liệu lên collection chat, trong document có id là chatId
                    database.collection(Constants.KEY_COLLECTION_CHAT)
                            .document(chatId)
                            .set(message)
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    showToast("Image sent successfully");
                                } else {
                                    showToast(task1.getException().getMessage());
                                }
                            });
                    // Cập nhật lastMessage
                    updateLastMessage(message);
                    binding.inputMessage.setText(null);

                }
            });
        }
    }

    // Hàm xử lí cập nhật lastMessage cho conversation giữa 2 user
    private void updateLastMessage(HashMap<String, Object> message) {
        // Nếu 2 user đã có conversation với nhau, chỉ cập nhật lastMessage và thời gian
        // ngược lại, tạo và thêm mới một conversation trong collection conversation
        if (conversationId != null) {
            updateConversation(message);
        } else {
            // Thiết lập dữ liệu cho một document trong collection conversation
            HashMap<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversation.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
            conversation.put(Constants.KEY_RECEIVER_NAME, receivedUser.name);
            conversation.put(Constants.KEY_RECEIVER_IMAGE, receivedUser.image);
            conversation.put(Constants.KEY_LAST_MESSAGE, message.get(Constants.KEY_MESSAGE));
            conversation.put(Constants.KEY_TYPE, message.get(Constants.KEY_TYPE));
            conversation.put(Constants.KEY_TIMESTAMP, new Date());
            // Thêm mới một document trong collection
            addConversation(conversation);
        }
        // Nếu user offline, gửi thông báo đến thiết bị
        if (!isReceiverAvailaible) {
            try {
                // Thiết lập dữ liệu cho thông báo
                JSONArray tokens = new JSONArray();
                tokens.put(receivedUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                if (message.get(Constants.KEY_TYPE).equals( "text")) {
                    data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
                } else if (message.get(Constants.KEY_TYPE).equals("image")) {
                    data.put(Constants.KEY_MESSAGE, String.format("%s %s", preferenceManager.getString(Constants.KEY_NAME), "sent you an image"));
                } else if (message.get(Constants.KEY_TYPE).equals("audio")) {
                    data.put(Constants.KEY_MESSAGE, String.format("%s %s", preferenceManager.getString(Constants.KEY_NAME), "sent you an audio"));
                }
                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                // Hàm xử lí gửi thông báo
                sendNotification(body.toString());
            } catch (Exception e) {
                showToast(e.getMessage());
            }
        }
    }

    // Hàm nhận vào chuỗi string và hiển thị
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Hàm xử lí gửi thông báo
    private void sendNotification(String messageBody) {
        // Tạo một API, gửi thông báo đến thiết bị khác
        APIClient.getClient().create(APIService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            // Xử lí response trả về
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJSON = new JSONObject(response.body());
                            JSONArray results = responseJSON.getJSONArray("results");
                            if (responseJSON.getInt("failure") == 1) {
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // Nếu thông báo được gửi đi thành công, hiển thị thông báo
                    showToast("Notification sent successfully");
                } else {
                    // Nếu thất bại, hiển thị lỗi
                    showToast("Error: " + response.code());
                }
            }
            // Nếu quá trình gửi thông báo đi không thành công, hiển thị lỗi
            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    // Hàm kiểm tra tình trạng hoạt động(offline/online) của user theo real-time
    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(receivedUser.id)
                .addSnapshotListener(ChatActivity.this, ((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
                        if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                            int availability = Objects.requireNonNull(
                                    value.getLong(Constants.KEY_AVAILABILITY)
                            ).intValue();
                            isReceiverAvailaible = availability == 1;
                        }
                        receivedUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                        if (receivedUser.image == null) {
                            receivedUser.image = value.getString(Constants.KEY_IMAGE);
                            chatAdaptor.setReceiverProfileImage(getBitmapFromEncodedImage(receivedUser.image));
                            chatAdaptor.notifyItemRangeChanged(0, chatMessages.size());
                        }
                    }
                    // Nếu user online sẽ hiển thị 1 ô tròn màu xanh kế bên avatar để nhận biết
                    // ngược lại, ẩn đi
                    if (isReceiverAvailaible) {
                        binding.userAvailability.setVisibility(View.VISIBLE);
                    } else {
                        binding.userAvailability.setVisibility(View.GONE);
                    }
                }));
    }

    // Hàm xử lí tin nhắn giữa 2 user theo real-time
    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receivedUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receivedUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    // Một listener kéo dữ liệu tin nhắn giữa 2 user đẩy vào RecyclerView theo real-time
    // Nếu có tin nhắn mới, sẽ lập tức hiển thị
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.dateTime = getReadableDateTime(chatMessage.dateObject);
                    chatMessages.add(chatMessage);
                }
            }
            // Sắp xếp tin nhắn theo thứ tự thời gian
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                chatAdaptor.notifyDataSetChanged();
            } else {
                chatAdaptor.notifyItemRangeChanged(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversationId == null) {
            // Hàm kiểm tra conversation giữa 2 user
            checkForConversation();
        }
    };

    // Hàm chuyển ảnh dạng String sang Bitmap
    private Bitmap getBitmapFromEncodedImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    // Hàm xử lý lấy dữ liệu người nhận và gán vào giao diện
    private void loadReceiverDetails() {
        receivedUser = getIntent().getParcelableExtra(Constants.KEY_USER);
        binding.textName.setText(receivedUser.name);
    }

    // Chuyển dữ liệu thời gian dạng Date sang String
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM, dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    // Thêm mới một document vào collection conversation
    private void addConversation(HashMap<String, Object> conversation) {
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    // Cập nhật document trong collection conversation
    private void updateConversation(HashMap<String, Object> message) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID),
                Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME),
                Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE),
                Constants.KEY_RECEIVER_ID, receivedUser.id,
                Constants.KEY_RECEIVER_NAME, receivedUser.name,
                Constants.KEY_RECEIVER_IMAGE, receivedUser.image,
                Constants.KEY_LAST_MESSAGE, message.get(Constants.KEY_MESSAGE),
                Constants.KEY_TYPE, message.get(Constants.KEY_TYPE),
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    // Hàm kiểm tra giữa 2 user đã có conversation chưa
    private void checkForConversation() {
        checkForConversationRemoteLy(
                preferenceManager.getString(Constants.KEY_USER_ID),
                receivedUser.id
        );
        checkForConversationRemoteLy(
                receivedUser.id,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
    }
    // Tạo task lấy ra document trong collection conversation
    // có KEY_SENDER_ID = senderId
    // có KEY_RECEIVER_ID = receiverId
    private void checkForConversationRemoteLy(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    // Nếu task thực hiện thành công, lấy và gán id của document vào biến "conversationId"
    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
    };

    // Nếu người dùng mở lại ứng dụng, sẽ gọi hàm cập nhật tình trạng của user
    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}