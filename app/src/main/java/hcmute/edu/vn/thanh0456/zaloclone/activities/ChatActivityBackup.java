package hcmute.edu.vn.thanh0456.zaloclone.activities;

import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.devlomi.record_view.OnRecordListener;
import com.google.android.gms.tasks.OnCompleteListener;
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

public class ChatActivityBackup extends BaseActivity {

    private ActivityChatBinding binding;
    private User receivedUser;
    private PreferenceManager preferenceManager;
    private ArrayList<ChatMessage> chatMessages;
    private ChatAdaptor chatAdaptor;
    private FirebaseFirestore database;
    private String conversationId = null;
    private Boolean isReceiverAvailaible = false;
    private String chatId = null;
    private String imageURL = null, audioURL = null;
    private StorageTask uploadTask;
    private Uri imageUri, audioUri;
    private MediaRecorder mediaRecorder;
    private String audioPath;
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

    private void setListeners() {
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
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.imageSend.setOnClickListener(v -> sendMessage());
        binding.imagePhoto.setOnClickListener(v -> sendImage());
        binding.recordButton.setOnClickListener(v -> {
            if (Permissions.isRecordingok(ChatActivityBackup.this)) {
                binding.recordButton.setListenForRecord(true);
            } else {
                Permissions.requestRecording(ChatActivityBackup.this);
            }
        });
        binding.recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                //Start Recording..
                Log.d("RecordView", "onStart");
                setUpRecording();

                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                binding.inputMessage.setVisibility(View.GONE);
                binding.layoutLikeAndSend.setVisibility(View.GONE);
                binding.recordView.setVisibility(View.VISIBLE);
            }

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

                binding.inputMessage.setVisibility(View.VISIBLE);
                binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
                binding.recordView.setVisibility(View.GONE);

            }

            @Override
            public void onFinish(long recordTime) {
                Log.d("RecordView", "onFinish");
                mediaRecorder.stop();
                mediaRecorder.release();
                binding.inputMessage.setVisibility(View.VISIBLE);
                binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
                binding.recordView.setVisibility(View.GONE);
                sendRecordingMessage();
            }

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

                mediaRecorder.reset();
                mediaRecorder.release();

                binding.inputMessage.setVisibility(View.VISIBLE);
                binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
                binding.recordView.setVisibility(View.GONE);
            }
        });
    }

    private void init() {
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(receivedUser.image));

        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdaptor = new ChatAdaptor(
                chatMessages,
                getBitmapFromEncodedImage(receivedUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdaptor);
        database = FirebaseFirestore.getInstance();
        binding.recordButton.setRecordView(binding.recordView);
        binding.recordButton.setListenForRecord(false);
    }

    private void setUpRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat. THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        File file = new File(Environment.getExternalStorageDirectory(). getAbsolutePath(), "Recordings");
//        if (!file.exists()) {
//            file.mkdirs();
//        }
        audioPath = getFilePath();

        mediaRecorder.setOutputFile(audioPath);
    }
    private String getFilePath() {
        ContextWrapper contextwrapper = new ContextWrapper(getApplicationContext());
        File recordPath = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            recordPath = contextwrapper.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS);
        }
        File file  = new File(recordPath, System.currentTimeMillis() + ".3gp");
        return file.getPath();
    }
    private void sendRecordingMessage() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Audio Files");
        audioUri = Uri.fromFile(new File(audioPath));

        DocumentReference ref = database.collection("chats").document();
        chatId = ref.getId();

        StorageReference audioPathOnFireBase = storageReference.child(chatId + "." + "3gp");
        uploadTask = audioPathOnFireBase.putFile(audioUri);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                showToast(task.getException().getMessage());
                throw task.getException();
            }
            return audioPathOnFireBase.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadURL = (Uri) task.getResult();
                audioURL = downloadURL.toString();

                HashMap<String, Object> message = new HashMap<>();
                message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
                message.put(Constants.KEY_MESSAGE, audioURL);
                message.put(Constants.KEY_TYPE, "audio");
                message.put(Constants.KEY_TIMESTAMP, new Date());
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
                binding.inputMessage.setText(null);
            }
        });
    }

    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .add(message);
        if (conversationId != null) {
            updateConversation(binding.inputMessage.getText().toString());
        } else {
            HashMap<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversation.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
            conversation.put(Constants.KEY_RECEIVER_NAME, receivedUser.name);
            conversation.put(Constants.KEY_RECEIVER_IMAGE, receivedUser.image);
            conversation.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversation.put(Constants.KEY_TIMESTAMP, new Date());
            addConversation(conversation);
        }
        if (!isReceiverAvailaible) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receivedUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                sendNotification(body.toString());
            } catch (Exception e) {
                showToast(e.getMessage());
            }
        }
        binding.inputMessage.setText(null);
    }

    private void sendImage() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent.createChooser(intent, "Select Image"), 438);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");

            DocumentReference ref = database.collection("chats").document();
            chatId = ref.getId();

            StorageReference imagePath = storageReference.child(chatId + "." + "jpg");
            uploadTask = imagePath.putFile(imageUri);
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    showToast(task.getException().getMessage());
                    throw task.getException();
                }
                return imagePath.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadURL = (Uri) task.getResult();
                    imageURL = downloadURL.toString();

                    HashMap<String, Object> message = new HashMap<>();
                    message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
                    message.put(Constants.KEY_MESSAGE, imageURL);
                    message.put(Constants.KEY_TYPE, "image");
                    message.put(Constants.KEY_TIMESTAMP, new Date());
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
                    binding.inputMessage.setText(null);

                }
            });
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody) {
        APIClient.getClient().create(APIService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
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
                    showToast("Notification sent successfully");
                } else {
                    showToast("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(receivedUser.id)
                .addSnapshotListener(ChatActivityBackup.this, ((value, error) -> {
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
                    if (isReceiverAvailaible) {
                        binding.userAvailability.setVisibility(View.VISIBLE);
                    } else {
                        binding.userAvailability.setVisibility(View.GONE);
                    }
                }));
    }

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
            checkForConversation();
        }
    };

    private Bitmap getBitmapFromEncodedImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    private void loadReceiverDetails() {
        receivedUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receivedUser.name);
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM, dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversation(HashMap<String, Object> conversation) {
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    private void updateConversation(String message) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID),
                Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME),
                Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE),
                Constants.KEY_RECEIVER_ID, receivedUser.id,
                Constants.KEY_RECEIVER_NAME, receivedUser.name,
                Constants.KEY_RECEIVER_IMAGE, receivedUser.image,
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

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
    private void checkForConversationRemoteLy(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}