package hcmute.edu.vn.thanh0456.zaloclone;

import static android.app.Activity.RESULT_OK;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

import hcmute.edu.vn.thanh0456.zaloclone.activities.SignInActivity;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.DialogEditUsernameBinding;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.FragmentPersonBinding;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PersonFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PersonFragment extends Fragment{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private @NonNull FragmentPersonBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    // ???nh ???????c m?? ho?? th??nh ki???u String
    private String encodeImage;

    public PersonFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PersonFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PersonFragment newInstance(String param1, String param2) {
        PersonFragment fragment = new PersonFragment();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPersonBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(getActivity().getApplicationContext());
        init();
        setListeners();
        return binding.getRoot();
    }

    // H??m kh???i t???o
    private void init() {
        binding.imageProfile.setImageBitmap(getConversationImage(preferenceManager.getString(Constants.KEY_IMAGE)));
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        // instance c???a Firestore ????? t????ng t??c v???i d??? li???u tr??n Firestore
        database = FirebaseFirestore.getInstance();
    }

    // H??m thi???t l???p event
    private void setListeners() {
        binding.signout.setOnClickListener(v -> signOut());
        // Hi???n th??? m???t popupMenu c?? c??c l???a ch???n thay ?????i t??n group, ???nh group, xo?? group, r???i group
        binding.imageMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(getActivity(), v);
            popupMenu.getMenuInflater().inflate(R.menu.popup_user, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.changePhoto) {
                    changeUserPhoto();
                } else if(menuItem.getItemId() == R.id.changeName) {
                    changeUserName();
                }
                return true;
            });
            popupMenu.show();
        });
    }

    // H??m x??? l?? thay ???nh user
    private void changeUserPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImage.launch(intent);
    }

    // Sau khi ch???n ???nh ?????i di???n cho user, c???p nh???t l??n giao di???n, ?????y d??? li???u ???nh l??n Firestore
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = MainActivity.getContextOfApplication().getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imageProfile.setImageBitmap(bitmap);
                        encodeImage = encodeImage(bitmap);
                        updateUserPhoto();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    // C???p nh???t ???nh m???i trong SharePrefs v?? Firestore
    private void updateUserPhoto() {
        preferenceManager.putString(Constants.KEY_IMAGE, encodeImage);
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedImage(encodeImage));
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .update(Constants.KEY_IMAGE, encodeImage);
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, (preferenceManager.getString(Constants.KEY_USER_ID)))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        if (documentSnapshot.exists()) {
                            documentSnapshot
                                    .getReference()
                                    .update(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                        }
                    }
                });
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, (preferenceManager.getString(Constants.KEY_USER_ID)))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        if (documentSnapshot.exists()) {
                            documentSnapshot
                                    .getReference()
                                    .update(Constants.KEY_RECEIVER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                        }
                    }
                });
        showToast("Updated Photo");
    }

    // H??m x??? l?? thay t??n user
    private void changeUserName() {
        // Hi???n th??? m???t dialog ????? thay ?????i t??n m???i cho user
        // C?? 3 option l?? remove, cancel, save
        DialogEditUsernameBinding dialogEditUsernameBinding = DialogEditUsernameBinding.inflate(getLayoutInflater());
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogEditUsernameBinding.getRoot());

        Window window = dialog.getWindow();

        if (window == null) {
            return;
        }

        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams windowAtrributes = window.getAttributes();
        windowAtrributes.gravity = Gravity.CENTER;
        window.setAttributes(windowAtrributes);

        dialogEditUsernameBinding.edtUserName.setText(binding.textName.getText().toString());
        dialogEditUsernameBinding.edtUserName.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(getActivity().getApplicationContext().INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        setDialogListeners(dialogEditUsernameBinding, dialog);
        dialog.show();

    }

    // H??m thi???t l???p event cho dialog
    private void setDialogListeners(DialogEditUsernameBinding dialogEditUsernameBinding, Dialog dialog) {
        // L??m tr???ng n??i nh???p t??n user
        dialogEditUsernameBinding.removeButton.setOnClickListener(v -> {
            dialogEditUsernameBinding.edtUserName.setText("");
        });
        // ????ng dialog
        dialogEditUsernameBinding.cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });
        // C???p nh???t t??n m???i cho user
        dialogEditUsernameBinding.saveButton.setOnClickListener(v -> {
            if (!binding.textName.equals(dialogEditUsernameBinding.edtUserName)) {
                updateUserName(dialogEditUsernameBinding.edtUserName.getText().toString());
            }
            dialog.dismiss();
        });
    }

    // C???p nh???t t??n m???i trong SharePrefs v?? Firestore
    private void updateUserName(String newName) {
        preferenceManager.putString(Constants.KEY_NAME, newName);
        binding.textName.setText(newName);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .update(Constants.KEY_NAME, newName);
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, (preferenceManager.getString(Constants.KEY_USER_ID)))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        if (documentSnapshot.exists()) {
                            documentSnapshot
                                    .getReference()
                                    .update(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                        }
                    }
                });
        database.collection(Constants.KEY_COLLECTIONS_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, (preferenceManager.getString(Constants.KEY_USER_ID)))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        if (documentSnapshot.exists()) {
                            documentSnapshot
                                    .getReference()
                                    .update(Constants.KEY_RECEIVER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                        }
                    }
                });
        showToast("Updated Name");
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

    // H??m chuy???n ???nh d???ng Bitmap sang String
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // Decode ???nh t??? String sang Bitmap
    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // H??m nh???n chu???i String truy???n v??o v?? hi???n th???
    private void showToast(String message) {
        Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // H??m ????ng xu???t, khi ???????c g???i s??? xo?? FCM_TOKEN trong document c???a user tr??n Firestore, xo?? d??? li???u user trong SharePrefs
    // v?? chuy???n t???i trang ????ng nh???p
    private void signOut() {
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    FirebaseAuth.getInstance().signOut();
                    preferenceManager.clear();
                    startActivity(new Intent(getActivity().getApplicationContext(), SignInActivity.class));
                    getActivity().finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

}