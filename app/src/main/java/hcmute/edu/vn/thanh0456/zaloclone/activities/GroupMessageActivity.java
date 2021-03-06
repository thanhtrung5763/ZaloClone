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
    // Binding thi???t k??? layout v?? hi???n th???(????? ph???i t???o c??c bi???n ????? l???y c??c thu???c t??nh trong layout v?? s??? d???ng)
    private ActivityGroupMessageBinding binding;
    // instance c???a Firestore ????? t????ng t??c v???i d??? li???u khi c???n
    private FirebaseFirestore database;
    // L??u ho???c l???y d??? li???u trong SharePref
    private PreferenceManager preferenceManager;
    // instance l??u d??? li???u li??n quan ?????n group
    private Group group;
    // instance l??u d??? li???u li??n quan ?????n tin nh???n m???i nh???t trong group
    GroupLastMessageModel lastMessageModel;
    // L??u l???ch s??? tin nh???n trong group
    private ArrayList<GroupMessage> groupMessages;
    // Adaptor
    private GroupChatAdaptor groupChatAdaptor;
    // id c???a ??o???n tin nh???n g???i ??i trong group
    private String groupMessageId = null;
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
    // L??u danh s??ch th??nh vi??n v?? ???nh ?????i di???n trong group
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

    // H??m kh???i t???i
    private void init() {
        // L??u ho???c l???y d??? li???u t??? SharePrefs
        preferenceManager = new PreferenceManager(getApplicationContext());
        // L??u tin nh???n c???a group
        groupMessages = new ArrayList<>();
        // Thi???t l???p adaptor
        groupChatAdaptor = new GroupChatAdaptor(
                groupMessages,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        // G???n adaptor v??o RecyclerView
        binding.chatRecyclerView.setAdapter(groupChatAdaptor);
        // T????ng t??c v???i d??? li???u tr??n Firestore
        database = FirebaseFirestore.getInstance();
        // Thi???t l???p recordView cho n??t record
        binding.recordButton.setRecordView(binding.recordView);
        binding.recordButton.setListenForRecord(false);
        // L??u d??? li???u v??? tin nh???n m???i nh???t c???a group
        lastMessageModel = new GroupLastMessageModel();
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
        // G???i h??m x??? l?? chuy???n ?????n trang xem th??ng tin group
        binding.imageInfo.setOnClickListener(v -> showInfoGroup());
        // C???p quy???n record cho ???ng d???ng
        binding.recordButton.setOnClickListener(v -> {
            if (Permissions.isRecordingok(GroupMessageActivity.this)) {
                binding.recordButton.setListenForRecord(true);
            } else {
                Permissions.requestRecording(GroupMessageActivity.this);
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

    // H??m x??? l?? chuy???n ?????n trang xem th??ng tin group
    private void showInfoGroup() {
        Intent intent = new Intent(getApplicationContext(), GroupInfoActivity.class);
        intent.putExtra(Constants.KEY_COLLECTION_GROUP, group);
        startActivity(intent);
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

        DocumentReference ref = database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE).document();
        groupMessageId = ref.getId();

        StorageReference audioPathOnFireBase = storageReference.child(groupMessageId + "." + "3gp");
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
                // Thi???t l???p d??? li???u c???n thi???t cho m???t document trong collection group
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

                // C???p nh???t tin nh???n m???i nh???t c???a group l??n Firestore trong subcollection lastMessage trong collection group
                database.collection(Constants.KEY_COLLECTION_GROUP)
                        .document(group.id)
                        .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                        .document(group.id)
                        .update(message);

                message.put(Constants.KEY_GROUP_ID, group.id);
                // T???o m???t document trong collection groupMessage v???i id l?? group.id, ?????y d??? li???u v??o trong document
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

            DocumentReference ref = database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE).document();
            groupMessageId = ref.getId();

            StorageReference imagePath = storageReference.child(groupMessageId + "." + "jpg");
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

                    lastMessageModel.dateObject = new Date();
                    lastMessageModel.message = imageURL;
                    lastMessageModel.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
                    lastMessageModel.type = "image";

                    // Thi???t l???p d??? li???u cho 1 document trong subcollection lastMessage trong collection group
                    HashMap<String, Object> message = new HashMap<>();
                    message.put(Constants.KEY_SENDER_ID, lastMessageModel.senderId);
                    message.put(Constants.KEY_MESSAGE, lastMessageModel.message);
                    message.put(Constants.KEY_TYPE, lastMessageModel.type);
                    message.put(Constants.KEY_TIMESTAMP, lastMessageModel.dateObject);

                    // C???p nh???t d??? li???u ???????c thi???t l???p l??n Firestore trong subcollection lastMessage trong collection group
                    // d??? li???u li??n quan ?????n tin nh???n m???i nh???t c???a group
                    database.collection(Constants.KEY_COLLECTION_GROUP)
                            .document(group.id)
                            .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                            .document(group.id)
                            .update(message);

                    message.put(Constants.KEY_GROUP_ID, group.id);
                    // T???o m???t document trong collection groupMessage v???i id l?? group.id, ?????y d??? li???u v??o trong document
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

    // H??m chuy???n ???nh d???ng String sang Bitmap
    private Bitmap getBitmapFromEncodedImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    // T???i th??ng tin c?? b???n c???a group nh?? ???nh, t??n, danh s??ch th??nh vi??n
    // v?? hi???n th??? l??n giao di???n
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

    // H??m x??? l?? g???i tin nh???n d???ng text
    private void sendMessage() {
        lastMessageModel.dateObject = new Date();
        lastMessageModel.message = binding.inputMessage.getText().toString();
        lastMessageModel.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
        lastMessageModel.type = "text";

        // Thi???t l???p d??? li???u cho m???t document trong subcollection lastMessage trong collection group
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, lastMessageModel.senderId);
        message.put(Constants.KEY_MESSAGE, lastMessageModel.message);
        message.put(Constants.KEY_TYPE, lastMessageModel.type);
        message.put(Constants.KEY_TIMESTAMP, lastMessageModel.dateObject);

        // C???p nh???t d??? li???u ???????c thi???t l???p l??n Firestore trong subcollection lastMessage trong collection group
        // d??? li???u li??n quan ?????n tin nh???n m???i nh???t c???a group
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                .document(group.id)
                .update(message);

        message.put(Constants.KEY_GROUP_ID, group.id);
        // T???o m???t document trong collection groupMessage v???i id l?? group.id, ?????y d??? li???u v??o trong document
        database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE)
                .add(message);


        group.lastMessageModel = lastMessageModel;
        // C???p nh???t RecyclerView
        RecentGroupAdaptor.updateLastMessage(group);
        binding.inputMessage.setText(null);
    }

    // H??m x??? l?? theo d??i s??? thay ?????i v??? tin nh???n trong group
    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_GROUP_MESSAGE)
                .whereEqualTo(Constants.KEY_GROUP_ID, group.id)
                .addSnapshotListener(eventListener);
    }

    // C???p nh???t v?? hi???n th??? tin nh???n theo real-time
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

                    // Chuy???n ?????i ???nh ?????i di???n c???a th??nh vi??n trong group d???ng String sang Bitmap
                    // L??u tr??? trong thu???c t??nh imageBitmap
                    // ???????c s??? d???ng ????? hi???n th??? k??m theo ??o???n tin nh???n th??nh vi??n ???? g???i
                    for (HashMap<String, Object> hashMap : memberAndImageHashMapList) {
                        if (hashMap.get(Constants.KEY_USER_ID).equals(groupMessage.senderId)) {
                            groupMessage.imageBitmap = (Bitmap) hashMap.get(Constants.KEY_IMAGE);
                            break;
                        }
                    }
                    // Thi???t l???p gi?? tr??? cho c??c thu???c t??nh c???a instance groupMessage
                    groupMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    groupMessage.type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                    groupMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    groupMessage.dateTime = getReadableDateTime(groupMessage.dateObject);
                    // Th??m instance v??o danh s??ch tin nh???n
                    groupMessages.add(groupMessage);
                }
            }
            // S???p x???p c??c tin nh???n theo th??? t??? th???i gian
            Collections.sort(groupMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                // C???p nh???t RecyclerView
                groupChatAdaptor.notifyDataSetChanged();
            } else {
                // C???p nh???t RecyclerView
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

    // Chuy???n th???i gian d???ng Date sang String
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM, dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    // H??m x??? l?? hi???n th??? ??o???n th??ng b??o
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // H??m nh???n d??? li???u n???u group c?? s??? thay ?????i nh?? t??n, ???nh, s??? l?????ng th??nh vi??n
    // c???p nh???t tr???ng th??i group l??n version m???i nh???t
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

    // Khi b???t ?????u activity, ????ng k?? h??m s??? nh???n d??? li???u truy???n v??? t??? m???t activity kh??c
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.KEY_COLLECTION_GROUP)
        );
    }


    // Hu??? ????ng k?? h??m nh???n d??? li???u khi activity b??? d???ng
    @Override
    protected void onDestroy() {
        // Unregister when activity finished
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
        super.onDestroy();
    }

}