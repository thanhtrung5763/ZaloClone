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


// X??? l?? giao di???n chat 1-1
// extends BaseActivity ????? theo d??i v?? c???p nh???t t??nh tr???ng on/off c???a ng?????i d??ng
public class ChatActivity extends BaseActivity {

    // Binding thi???t k??? layout v?? hi???n th???(????? ph???i t???o c??c bi???n ????? l???y c??c thu???c t??nh trong layout v?? s??? d???ng)
    private ActivityChatBinding binding;
    // L??u d??? li???u ng?????i nh???n
    private User receivedUser;
    // L??u ho???c l???y d??? li???u trong SharePrefs
    private PreferenceManager preferenceManager;
    // L???ch s??? tin nh???n
    private ArrayList<ChatMessage> chatMessages;
    // Adaptor
    private ChatAdaptor chatAdaptor;
    // T????ng t??c v???i d??? li???u tr??n Firestore
    private FirebaseFirestore database;
    // id cu???c tr?? chuy???n gi???a 2 user
    private String conversationId = null;
    // t??nh tr???ng on/off c???a user
    private Boolean isReceiverAvailaible = false;
    // id c???a ??o???n tin nh???n
    private String chatId = null;
    // ???????ng d???n ?????n ???nh, audio l??u tr??n Storage c???a Firestore
    private String imageURL = null, audioURL = null;
    // ?????y ???nh, audio l??n Storage
    private StorageTask uploadTask;
    // Uri t??? ???????ng d???n ?????n ???nh, audio l??u tr??n thi???t b???
    private Uri imageUri, audioUri;
    // T???o recording
    private MediaRecorder mediaRecorder;
    // ???????ng d???n ?????n file recording
    private String audioPath;
    // T???m d???ng hi???n th??? UI bottom chat, ?????i animation cancel recording ho??n th??nh
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

    // H??m thi???t l???p event
    private void setListeners() {
        // X??? l?? ???n/hi???n giao di???n bottom chat d???a tr??n tin nh???n user nh???p
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
        // Tr??? v??? trang tr?????c
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // G???i h??m x??? l?? g???i tin nh???n text
        binding.imageSend.setOnClickListener(v -> sendMessage());
        // G???i h??m x??? l?? g???i tin nh???n ???nh
        binding.imagePhoto.setOnClickListener(v -> sendImage());
        // G???i l???i m???i cu???c g???i tho???i ?????n user, hi???n th??? giao di???n ch??? ph???n h???i t??? ng?????i ???????c m???i
        binding.imageVideo.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, VideoCallingOutgoingActivity.class);
            intent.putExtra(Constants.KEY_USER, receivedUser);
            intent.putExtra(Constants.REMOTE_MSG_MEETING_TYPE, "video");
            startActivity(intent);
        });
        // C???p quy???n record cho ???ng d???ng
        binding.recordButton.setOnClickListener(v -> {
            if (Permissions.isRecordingok(ChatActivity.this)) {
                binding.recordButton.setListenForRecord(true);
            } else {
                Permissions.requestRecording(ChatActivity.this);
            }
        });
        // ???n gi??? n??t record
        binding.recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                // B???t ?????u record
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

            // H??m x??? l?? k??o t??? ph???i sang tr??i ????? hu??? record
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

            // H??m x??? l?? th??? n??t record ????? k???t th??c record
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

            // H??m x??? l?? n???u th???i l?????ng record nh??? h??n 1 gi??y th?? hu??? file, kh??ng g???i
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
        // Thi???t l???p animation cho n??t record
        binding.recordView.setOnBasketAnimationEndListener(() -> {
            Log.d("RecordView", "Basket Animation Finished");
            binding.layoutBottomLeft.setVisibility(View.VISIBLE);
            binding.inputMessage.setVisibility(View.VISIBLE);
            binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
        });
    }

    // H??m kh???i t???o
    private void init() {
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(receivedUser.image));
        preferenceManager = new PreferenceManager(getApplicationContext());
        // L??u tin nh???n gi???a 2 user
        chatMessages = new ArrayList<>();
        // Thi???t l???p adaptor
        chatAdaptor = new ChatAdaptor(
                chatMessages,
                getBitmapFromEncodedImage(receivedUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        // G???n adaptor v??o RecyclerView
        binding.chatRecyclerView.setAdapter(chatAdaptor);
        // instance c???a Firestore ????? t????ng t??c v???i d??? li???u khi c???n
        database = FirebaseFirestore.getInstance();
        // Thi???t l???p RecordView cho n??t record
        binding.recordButton.setRecordView(binding.recordView);
        binding.recordButton.setListenForRecord(false);
    }

    // Thi???t l???p source, format, encode cho file record, audioPath l?? ???????ng d???n ?????n file record
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

    // H??m thi???t l???p filepath ????? l??u file
    private String getFilePath() {
        ContextWrapper contextwrapper = new ContextWrapper(getApplicationContext());
        File recordPath = null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            recordPath = contextwrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        }
        File file  = new File(recordPath, System.currentTimeMillis() + ".3gp");
        return file.getPath();
    }

    // H??m x??? l?? g???i tin nh???n record
    private void sendRecordingMessage() {
        // Sau khi record, l???y uri t??? audioPath, ?????y file record l??n Storage c???a Firebase, trong th?? m???c Audio Files
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Audio Files");
        audioUri = Uri.fromFile(new File(audioPath));

        DocumentReference ref = database.collection(Constants.KEY_COLLECTION_CHAT).document();
        chatId = ref.getId();

        StorageReference audioPathOnFireBase = storageReference.child(chatId + "." + "3gp");
        uploadTask = audioPathOnFireBase.putFile(audioUri);
        uploadTask.continueWithTask(task -> {
            // N???u th???t b???i, hi???n th??? l???i
            if (!task.isSuccessful()) {
                showToast(task.getException().getMessage());
                throw task.getException();
            }
            // N???u th??nh c??ng, tr??? v??? URL ?????n file record tr??n Storage
            return audioPathOnFireBase.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                // Thi???t l???p d??? li???u c???n thi???t cho m???t document trong collection chat
                Uri downloadURL = (Uri) task.getResult();
                audioURL = downloadURL.toString();

                HashMap<String, Object> message = new HashMap<>();
                message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
                message.put(Constants.KEY_MESSAGE, audioURL);
                message.put(Constants.KEY_TYPE, "audio");
                message.put(Constants.KEY_TIMESTAMP, new Date());

                // T???o m???t document trong collection chat v???i id l?? chatId, ?????y d??? li???u v??o trong document
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
                // H??m c???p nh???t lastMessage
                updateLastMessage(message);
                binding.inputMessage.setText(null);
            }
        });
    }

    // H??m g???i tin nh???n text
    private void sendMessage() {

        // Thi???t l???p d??? li???u cho m???t document trong collection chat
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TYPE, "text");
        message.put(Constants.KEY_TIMESTAMP, new Date());

        // Th??m d??? li???u v??o collection chat
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .add(message);
        // H??m c???p nh???t lastMessage
        updateLastMessage(message);
        binding.inputMessage.setText(null);
    }

    // H??m x??? l?? g???i tin nh???n ???nh
    private void sendImage() {
        // Ch???n ???nh
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        // H??m x??? l?? sau khi ch???n ???nh
        startActivityForResult(intent.createChooser(intent, "Select Image"), 438);
    }

    // H??m x??? l?? sau khi ch???n ???nh
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // L???y uri t??? ???nh, ?????y l??n Storage c???a Firebase, l??u trong th?? m???c "Image Files"
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
                // N???u ?????y ???nh l??n th??nh c??ng, tr??? v??? URL ?????n ???nh tr??n Storage
                return imagePath.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // L???y URL tr??? v???, l??u d?????i d???ng String
                    Uri downloadURL = (Uri) task.getResult();
                    imageURL = downloadURL.toString();

                    // Thi???t l???p d??? li???u cho 1 document trong collection chat
                    HashMap<String, Object> message = new HashMap<>();
                    message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
                    message.put(Constants.KEY_MESSAGE, imageURL);
                    message.put(Constants.KEY_TYPE, "image");
                    message.put(Constants.KEY_TIMESTAMP, new Date());
                    // L??u d??? li???u l??n collection chat, trong document c?? id l?? chatId
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
                    // C???p nh???t lastMessage
                    updateLastMessage(message);
                    binding.inputMessage.setText(null);

                }
            });
        }
    }

    // H??m x??? l?? c???p nh???t lastMessage cho conversation gi???a 2 user
    private void updateLastMessage(HashMap<String, Object> message) {
        // N???u 2 user ???? c?? conversation v???i nhau, ch??? c???p nh???t lastMessage v?? th???i gian
        // ng?????c l???i, t???o v?? th??m m???i m???t conversation trong collection conversation
        if (conversationId != null) {
            updateConversation(message);
        } else {
            // Thi???t l???p d??? li???u cho m???t document trong collection conversation
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
            // Th??m m???i m???t document trong collection
            addConversation(conversation);
        }
        // N???u user offline, g???i th??ng b??o ?????n thi???t b???
        if (!isReceiverAvailaible) {
            try {
                // Thi???t l???p d??? li???u cho th??ng b??o
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

                // H??m x??? l?? g???i th??ng b??o
                sendNotification(body.toString());
            } catch (Exception e) {
                showToast(e.getMessage());
            }
        }
    }

    // H??m nh???n v??o chu???i string v?? hi???n th???
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // H??m x??? l?? g???i th??ng b??o
    private void sendNotification(String messageBody) {
        // T???o m???t API, g???i th??ng b??o ?????n thi???t b??? kh??c
        APIClient.getClient().create(APIService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            // X??? l?? response tr??? v???
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
                    // N???u th??ng b??o ???????c g???i ??i th??nh c??ng, hi???n th??? th??ng b??o
                    showToast("Notification sent successfully");
                } else {
                    // N???u th???t b???i, hi???n th??? l???i
                    showToast("Error: " + response.code());
                }
            }
            // N???u qu?? tr??nh g???i th??ng b??o ??i kh??ng th??nh c??ng, hi???n th??? l???i
            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    // H??m ki???m tra t??nh tr???ng ho???t ?????ng(offline/online) c???a user theo real-time
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
                    // N???u user online s??? hi???n th??? 1 ?? tr??n m??u xanh k??? b??n avatar ????? nh???n bi???t
                    // ng?????c l???i, ???n ??i
                    if (isReceiverAvailaible) {
                        binding.userAvailability.setVisibility(View.VISIBLE);
                    } else {
                        binding.userAvailability.setVisibility(View.GONE);
                    }
                }));
    }

    // H??m x??? l?? tin nh???n gi???a 2 user theo real-time
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
    // M???t listener k??o d??? li???u tin nh???n gi???a 2 user ?????y v??o RecyclerView theo real-time
    // N???u c?? tin nh???n m???i, s??? l???p t???c hi???n th???
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
            // S???p x???p tin nh???n theo th??? t??? th???i gian
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
            // H??m ki???m tra conversation gi???a 2 user
            checkForConversation();
        }
    };

    // H??m chuy???n ???nh d???ng String sang Bitmap
    private Bitmap getBitmapFromEncodedImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    // H??m x??? l?? l???y d??? li???u ng?????i nh???n v?? g??n v??o giao di???n
    private void loadReceiverDetails() {
        receivedUser = getIntent().getParcelableExtra(Constants.KEY_USER);
        binding.textName.setText(receivedUser.name);
    }

    // Chuy???n d??? li???u th???i gian d???ng Date sang String
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM, dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    // Th??m m???i m???t document v??o collection conversation
    private void addConversation(HashMap<String, Object> conversation) {
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    // C???p nh???t document trong collection conversation
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

    // H??m ki???m tra gi???a 2 user ???? c?? conversation ch??a
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
    // T???o task l???y ra document trong collection conversation
    // c?? KEY_SENDER_ID = senderId
    // c?? KEY_RECEIVER_ID = receiverId
    private void checkForConversationRemoteLy(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    // N???u task th???c hi???n th??nh c??ng, l???y v?? g??n id c???a document v??o bi???n "conversationId"
    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
    };

    // N???u ng?????i d??ng m??? l???i ???ng d???ng, s??? g???i h??m c???p nh???t t??nh tr???ng c???a user
    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}