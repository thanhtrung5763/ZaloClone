package hcmute.edu.vn.thanh0456.zaloclone;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import hcmute.edu.vn.thanh0456.zaloclone.activities.ChatActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.SearchActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.SignInActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.UsersActivity;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.RecentConversationAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.TopStoryAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.FragmentChatBinding;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.ConversationListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.Story;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.models.UserStory;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment implements ConversationListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private FragmentChatBinding binding;
    private PreferenceManager preferenceManager;
    private ArrayList<ChatMessage> conversations;
    private ArrayList<UserStory> userStories;
    private RecentConversationAdaptor recentConversationAdaptor;
    private TopStoryAdaptor topStoryAdaptor;
    private FirebaseFirestore database;
    private String imageURL = null;
    private StorageTask uploadTask;
    private Uri imageUri;
    ArrayList<Task> tasks = new ArrayList<>();
    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ExploreFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChatFragment newInstance(String param1, String param2) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    // H??m thi???t l???p event
    private void setListeners() {
//        binding.imageSignOut.setOnClickListener(v -> signOut());
        // Chuy???n t???i trang Users
        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getActivity().getApplicationContext(), UsersActivity.class)));
        // T???o story
        binding.createStory.setOnClickListener(v -> createStory());
        // T??m ki???m user
        binding.inputSearch.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity().getApplicationContext(), SearchActivity.class);
            startActivity(intent);
        });
    }

    // H??m t???o story
    private void createStory() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent.createChooser(intent, "Select Image"), 438);
    }

    // Ch???n ???nh t??? thi???t b???
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            // L??u ???nh ???????c ch???n v??o Storage c???a Firebase
            imageUri = data.getData();
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Story Files");
            Date date = new Date();
            StorageReference imagePath = storageReference.child(date.getTime() + "." + "jpg");
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
                    // L???y URL c???a ???nh ???? ???????c l??u tr??n Storage
                    imageURL = downloadURL.toString();

                    // M???t instance c???a UserStory ch???a d??? li???u li??n quan
                    UserStory userStory = new UserStory();
                    userStory.name = preferenceManager.getString(Constants.KEY_NAME);
                    userStory.image = preferenceManager.getString(Constants.KEY_IMAGE);
                    userStory.lastUpdated = date.getTime();

                    // M???t instance c???a Story ch???a d??? li???u li??n quan
                    Story story = new Story(imageURL, userStory.lastUpdated);


                    HashMap<String, Object> userStoryObj = new HashMap<>();
                    userStoryObj.put(Constants.KEY_NAME, userStory.name);
                    userStoryObj.put(Constants.KEY_IMAGE, userStory.image);
                    userStoryObj.put(Constants.KEY_LAST_UPDATED, userStory.lastUpdated);

                    // Trong collection userStory, t???o m???t document v???i id l?? userId, l??u d??? li???u ch???a trong instance UserStory
                    // m???t subcollection story, l??u d??? li???u ch???a trong instance story
                    database.collection(Constants.KEY_COLLECTION_USER_STORY)
                            .document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .set(userStoryObj);
                    (database.collection(Constants.KEY_COLLECTION_USER_STORY)
                            .document(preferenceManager.getString(Constants.KEY_USER_ID)))
                            .collection(Constants.KEY_COLLECTION_STORY)
                            .add(story);
                }
            });
        }
    }

    // L???y d??? li???u trong collection userStory cho v??o RecyclerView
    // N???u user th??m m???i story, c???p nh???t d??? li???u trong collection userStory
    // l???y d??? li???u m???i v???a c???p nh???t xu???ng v?? c???p nh???t cho RecyclerView
    private void listenStories() {
        database.collection(Constants.KEY_COLLECTION_USER_STORY)
                .addSnapshotListener(eventStoryListener);
    }
    private final EventListener<QuerySnapshot> eventStoryListener = ((value, error) -> {
        if (error != null) {
            return;
        }

        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    UserStory userStory = new UserStory();
                    userStory.name = documentChange.getDocument().getString(Constants.KEY_NAME);
                    userStory.image = documentChange.getDocument().getString(Constants.KEY_IMAGE);
                    userStory.lastUpdated = documentChange.getDocument().getLong(Constants.KEY_LAST_UPDATED);
                    userStories.add(userStory);

//                    ArrayList<Story> stories = new ArrayList<>();
                    // T???o task l???y t???t c??? story c???a user t????ng ???ng
                    Task task = documentChange.getDocument().getReference().collection(Constants.KEY_COLLECTION_STORY)
                            .get();
                    tasks.add(task);
//                    documentChange.getDocument().getReference().collection(Constants.KEY_COLLECTION_STORY)
//                            .get()
//                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                                @Override
//                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
//                                        Story story = documentSnapshot.toObject(Story.class);
//                                        stories.add(story);
//
//                                    }
//                                    userStory.stories = stories;
//                                    userStories.add(userStory);
//                                    topStoryAdaptor.notifyDataSetChanged();
//                                }
//                            });
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    // Th??m story v??o collection story ???? c?? s???n c???a user t????ng ???ng
                    // c???p nh???t gi?? tr??? lastUpdated
                    UserStory userStory = new UserStory();
                    userStory.name = documentChange.getDocument().getString(Constants.KEY_NAME);
                    userStory.image = documentChange.getDocument().getString(Constants.KEY_IMAGE);
                    userStory.lastUpdated = documentChange.getDocument().getLong(Constants.KEY_LAST_UPDATED);

                    documentChange.getDocument().getReference().collection(Constants.KEY_COLLECTION_STORY)
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    ArrayList<Story> stories = new ArrayList<>();
                                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                        Story story = documentSnapshot.toObject(Story.class);
                                        stories.add(story);
                                    }
                                    for (UserStory userStory1 : userStories) {
                                        if (userStory1.name.equals(userStory.name)) {
                                            userStory1.stories = stories;
                                            // S???p x???p c??c userStory theo lastUpdated
                                            Collections.sort(userStories, new Comparator<UserStory>() {
                                                @Override
                                                public int compare(UserStory o1, UserStory o2) {
                                                    return (o1.lastUpdated < o2.lastUpdated) ? -1 : ((o1.lastUpdated == o2.lastUpdated) ? 0 : 1);
                                                }
                                            });
                                            // C???p nh???t l???i recyclerView
                                            topStoryAdaptor.notifyDataSetChanged();
                                            break;
                                        }
                                    }
                                }
                            });

                }
            }
            // Th???c hi???n t???t c??? task trong list tasks
            Tasks.whenAllSuccess(tasks).addOnSuccessListener(new OnSuccessListener<List<Object>>() {
                @Override
                public void onSuccess(List<Object> objects) {
                    int i = 0;
                    // V???i m???i task, l???y t???t c??? story c???a ng?????i d??ng t????ng ???ng v?? l??u v??o thu???c t??nh stories c???a instance userStories t???i v??? tr?? i
                    for (Object object : objects) {
                        ArrayList<Story> stories = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : ((QuerySnapshot) object)) {
                            Story story = documentSnapshot.toObject(Story.class);
                            stories.add(story);
                            userStories.get(i).stories = stories;
                        }
                        i += 1;
                    }
                    // C???p nh???t recyclerView
                    topStoryAdaptor.notifyItemRangeChanged(0, userStories.size());
                    binding.storiesRecyclerView.smoothScrollToPosition(0);
                    binding.storiesRecyclerView.setVisibility(View.VISIBLE);
                }
            });
        }
    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentChatBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(getActivity().getApplicationContext());
        init();
//        loadUserDetails();
        setListeners();
        listenConversations();
        listenStories();
        return binding.getRoot();
    }

    // H??m kh???i t???o
    private void init() {
        binding.imageProfile.setImageBitmap(getConversationImage(preferenceManager.getString(Constants.KEY_IMAGE)));
        // Ch???a c??c cu???c tr?? chuy???n g???n ????y
        conversations = new ArrayList<>();
        // Ch???a c??c story c???a c??c user
        userStories = new ArrayList<>();
        // Kh???i t???o c??c adaptor v?? g???n adapter v??o recyclerView
        recentConversationAdaptor = new RecentConversationAdaptor(conversations, preferenceManager.getString(Constants.KEY_USER_ID), this, getActivity());
        topStoryAdaptor = new TopStoryAdaptor(getActivity(), userStories);
        binding.conversationsRecyclerView.setAdapter(recentConversationAdaptor);
        binding.storiesRecyclerView.setAdapter(topStoryAdaptor);
        // instance c???a Firestore ????? t????ng t??c v???i d??? li???u tr??n Firestore
        database = FirebaseFirestore.getInstance();
    }
//    private void loadUserDetails() {
//        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
//        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//        binding.imageProfile.setImageBitmap(bitmap);
//    }

    // H??m nh???n v??o chu???i string v?? hi???n th???
    private void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    // L???y d??? li???u trong collection conversations cho v??o RecyclerView
    // N???u c?? s??? thay ?????i t??? ph??a ng?????i d??ng khi nh???n tin 1-1, c???p nh???t lastMessage v?? th???i gian
    // v?? l???y d??? li???u m???i nh???t c???p nh???t cho RecyclerView
    private void listenConversations() {
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
        if (error != null) {
            return;
        }
        // C???p nh???t d??? li???u n???u 1 trong c??c cu???c tr?? chuy???n c?? s??? thay ?????i(c?? th??m cu???c tr?? chuy???n m???i, tin nh???n m???i)
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;
                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                        chatMessage.conversationId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        chatMessage.conversationName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversationImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                        for (ChatMessage conversation : conversations) {
                            if (conversation.conversationId.equals(chatMessage.receiverId)) {
                                conversations.remove(conversation);
                                break;
                            }
                        }
                    } else {
                        chatMessage.conversationId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        chatMessage.conversationName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversationImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                        for (ChatMessage conversation : conversations) {
                            if (conversation.conversationId.equals(chatMessage.senderId)) {
                                conversations.remove(conversation);
                                break;
                            }
                        }
                    }

                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversations.size(); i++) {
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                            conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).type = documentChange.getDocument().getString(Constants.KEY_TYPE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            // S???p x???p c??c conversation theo th??? t??? th???i gian
            // C???p nh???t recyclerView
            Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            recentConversationAdaptor.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);
            binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    });

//    private void signOut() {
//        showToast("Signing out...");
//        FirebaseFirestore database = FirebaseFirestore.getInstance();
//        DocumentReference documentReference =
//                database.collection(Constants.KEY_COLLECTION_USERS).document(
//                        preferenceManager.getString(Constants.KEY_USER_ID)
//                );
//        HashMap<String, Object> updates = new HashMap<>();
//        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
//        documentReference.update(updates)
//                .addOnSuccessListener(unused -> {
//                    FirebaseAuth.getInstance().signOut();
//                    preferenceManager.clear();
//                    startActivity(new Intent(getActivity().getApplicationContext(), SignInActivity.class));
//                    getActivity().finish();
//                })
//                .addOnFailureListener(e -> showToast("Unable to sign out"));
//    }

    // Chuy???n t???i trang chatting gi???a 2 user
    @Override
    public void onConversationClicked(User user) {
        Intent intent = new Intent(getActivity().getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }

    // Decode ???nh t??? String sang Bitmap
    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        recentConversationAdaptor.notifyDataSetChanged();
    }
}