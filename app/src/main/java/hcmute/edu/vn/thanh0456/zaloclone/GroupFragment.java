package hcmute.edu.vn.thanh0456.zaloclone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.thanh0456.zaloclone.activities.ChatActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.GroupActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.GroupMessageActivity;
import hcmute.edu.vn.thanh0456.zaloclone.activities.UsersActivity;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.RecentConversationAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.adaptor.RecentGroupAdaptor;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.FragmentChatBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.FragmentGroupBinding;
import hcmute.edu.vn.thanh0456.zaloclone.listeners.ConversationListener;
import hcmute.edu.vn.thanh0456.zaloclone.models.ChatMessage;
import hcmute.edu.vn.thanh0456.zaloclone.models.Group;
import hcmute.edu.vn.thanh0456.zaloclone.models.GroupLastMessageModel;
import hcmute.edu.vn.thanh0456.zaloclone.models.User;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GroupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GroupFragment extends Fragment{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private FragmentGroupBinding binding;
    private PreferenceManager preferenceManager;
    private RecentGroupAdaptor recentGroupAdaptor;
    private ArrayList<Group> groups;
    private FirebaseFirestore database;
    private CollectionReference groupRef;
    ArrayList<Task> tasks = new ArrayList<>();

    public GroupFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CloudFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GroupFragment newInstance(String param1, String param2) {
        GroupFragment fragment = new GroupFragment();
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
        if (groups != null) {
            groups.clear();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentGroupBinding.inflate(inflater, container, false);
        init();
        setListeners();
        listenConversations();
        return binding.getRoot();
    }

    // Hàm khởi tạo
    private void init() {
        // Lưu hoặc lấy dữ liệu từ SharePrefs
        preferenceManager = new PreferenceManager(getActivity().getApplicationContext());
        // Danh sách các group mà user là thành viên
        groups = new ArrayList<>();
        // Thiết lập adaptor
        recentGroupAdaptor = new RecentGroupAdaptor(groups);
        // Gắn adaptor vào RecyclerView
        binding.conversationsRecyclerView.setAdapter(recentGroupAdaptor);
        // Tương tác với dữ liệu lưu trên Firestore
        database = FirebaseFirestore.getInstance();
        // Ánh xạ đến collection group trên Firestore
        groupRef = database.collection(Constants.KEY_COLLECTION_GROUP);
    }

    // Hàm thiết lập event
    private void setListeners() {
        // Chuyển đến trang tạo một group mới
        binding.fabNewGroup.setOnClickListener(v ->
                startActivity(new Intent(getActivity().getApplicationContext(), GroupActivity.class)));
    }

    // Hàm theo dõi trạng thái của group, cập nhật tin nhắn mới nhất theo real-time
    private void listenConversations() {
        groupRef.addSnapshotListener(eventListener);
    }

    // Hàm xử lí sự kiện cập nhật tin nhắn mới nhất theo real-time
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                // Lấy dữ liệu tất cả group trên Firestore đẩy vảo RecyclerView
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String groupId = documentChange.getDocument().getId();
                    groupRef.document(groupId)
                            .collection(Constants.KEY_COLLECTIONS_MEMBERS)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null
                                        && task.getResult().getDocuments().size() > 0) {
                                    Group group = new Group();
                                    group.members = new ArrayList<>();
                                    for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                                        String memberId = documentSnapshot.getId();
                                        User user = new User();
                                        user.id = memberId;
                                        user.role = documentSnapshot.getString(Constants.KEY_ROLE);

                                        DocumentReference documentReference1 = database.collection(Constants.KEY_COLLECTION_USERS)
                                                .document(memberId);
                                        documentReference1.addSnapshotListener((value12, error12) -> {
                                            if (error12 != null) {
                                                return;
                                            }
                                            if (value12 != null && value12.exists()) {
                                                user.name = value12.getString(Constants.KEY_NAME);
                                                user.image = value12.getString(Constants.KEY_IMAGE);
                                                if (user.role.equals("owner")) {
                                                    group.ownerId = user.id;
                                                    group.ownerName = user.name;
                                                }
                                            }
                                        });
                                        group.members.add(user);
                                        if (memberId.equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                                            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_GROUP)
                                                    .document(groupId);
                                            documentReference.addSnapshotListener((value1, error1) -> {
                                                if (error1 != null) {
                                                    return;
                                                }
                                                if (value1 != null && value1.exists()) {
                                                    group.image = value1.getString(Constants.KEY_GROUP_IMAGE);
                                                    group.name = value1.getString(Constants.KEY_GROUP_NAME);
                                                    group.id = value1.getId();
                                                    group.dateObject = value1.getDate(Constants.KEY_TIMESTAMP);

//                                                    Task task1 = value1.getReference().collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
//                                                            .document(groupId)
//                                                            .get();

//                                                    tasks.add(task1);
//                                                    groups.add(group);

                                                    value1.getReference().collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                                                            .document(groupId)
                                                            .get()
                                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                    if (documentSnapshot.exists()) {
                                                                        group.lastMessageModel = documentSnapshot.toObject(GroupLastMessageModel.class);
                                                                        group.lastMessageModel.dateObject = documentSnapshot.getDate(Constants.KEY_TIMESTAMP);
                                                                        group.lastMessageModel.dateTime = getReadableDateTime(group.lastMessageModel.dateObject);
                                                                    }
                                                                    else {
                                                                        HashMap<String, Object> message = new HashMap<>();
                                                                        message.put(Constants.KEY_TIMESTAMP, new Date());
                                                                        group.lastMessageModel = new GroupLastMessageModel();
                                                                        group.lastMessageModel.dateObject = (Date) message.get(Constants.KEY_TIMESTAMP);
                                                                        database.collection(Constants.KEY_COLLECTION_GROUP)
                                                                                .document(group.id)
                                                                                .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
                                                                                .document(group.id)
                                                                                .set(message);
                                                                    }
                                                                    groups.add(group);
                                                                    Collections.sort(groups, (obj1, obj2) -> obj2.lastMessageModel.dateObject.compareTo(obj1.lastMessageModel.dateObject));
                                                                    recentGroupAdaptor.notifyDataSetChanged();
                                                                }
                                                            });
                                                }
                                                binding.conversationsRecyclerView.smoothScrollToPosition(0);
                                                binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
                                                binding.progressBar.setVisibility(View.GONE);
                                            });
                                        }
                                    }
                                }
                            });
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (Group group : groups) {
                        if (group.id.equals(documentChange.getDocument().getId())) {
                            groups.remove(group);
                            break;
                        }
                    }
                }
            }
            Collections.sort(groups, (obj1, obj2) -> obj2.lastMessageModel.dateObject.compareTo(obj1.lastMessageModel.dateObject));
            recentGroupAdaptor.notifyDataSetChanged();
//            Task combineTask = Tasks.whenAllSuccess(tasks).addOnSuccessListener(new OnSuccessListener<List<Object>>() {
//                @Override
//                public void onSuccess(List<Object> objects) {
//                    for (Object object : objects) {
//                        for (DocumentSnapshot documentSnapshot : (QuerySnapshot) object) {
//                            if (documentSnapshot.exists()) {
//                                for (Group group : groups) {
//                                    if (group.id.equals(documentSnapshot.getId())) {
//                                        group.lastMessageModel = documentSnapshot.toObject(GroupLastMessageModel.class);
//                                        group.lastMessageModel.dateObject = documentSnapshot.getDate(Constants.KEY_TIMESTAMP);
//                                        group.lastMessageModel.dateTime = getReadableDateTime(group.lastMessageModel.dateObject);
//                                    }
//                                }
//                            }
//                            else {
//                                for (Group group : groups) {
//                                    if (group.lastMessageModel == null) {
//                                        HashMap<String, Object> message = new HashMap<>();
//                                        message.put(Constants.KEY_TIMESTAMP, new Date());
//                                        group.lastMessageModel = new GroupLastMessageModel();
//                                        group.lastMessageModel.dateObject = (Date) message.get(Constants.KEY_TIMESTAMP);
//                                        database.collection(Constants.KEY_COLLECTION_GROUP)
//                                                .document(group.id)
//                                                .collection(Constants.KEY_COLLECTION_LAST_MESSAGE)
//                                                .document(group.id)
//                                                .set(message);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    Collections.sort(groups, (obj1, obj2) -> obj2.lastMessageModel.dateObject.compareTo(obj1.lastMessageModel.dateObject));
//                    recentGroupAdaptor.notifyDataSetChanged();
//                    binding.conversationsRecyclerView.smoothScrollToPosition(0);
//                    binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
//                    binding.progressBar.setVisibility(View.GONE);
//                }
//            });
        }
    };

    // Hàm chuyển đổi thời gian dạng Date sang String
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM, dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    // Nếu người dùng quay trở lại trang, cập nhật RecyclerView
    @Override
    public void onResume() {
        super.onResume();
        recentGroupAdaptor.notifyDataSetChanged();
    }

    // Hàm nhận dữ liệu nếu group có sự thay đổi như tên, ảnh, số lượng thành viên
    // cập nhật trạng thái group lên version mới nhất
    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Group changedGroup = intent.getParcelableExtra(Constants.KEY_COLLECTION_GROUP);
            if (changedGroup != null) {
                int i = 0;
                while (groups.size() > 0 && i < groups.size()) {
                    if (groups.get(i).id.equals(changedGroup.id)) {
                        groups.remove(i);
                    } else {
                        i += 1;
                    }
                }
                groups.add(0, changedGroup);
                recentGroupAdaptor.notifyDataSetChanged();
                Toast.makeText(context, "Changed Group", Toast.LENGTH_SHORT).show();
            }
        }
    };

    // Khi bắt đầu activity, đăng kí hàm sẽ nhận dữ liệu truyền về từ một activity khác
    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.KEY_COLLECTION_GROUP)
        );
    }

    // Huỷ đăng kí hàm nhận dữ liệu khi activity bị dừng
    @Override
    public void onDestroy() {
        // Unregister when activity finished
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
        super.onDestroy();
    }
}