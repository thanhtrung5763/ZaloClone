package hcmute.edu.vn.thanh0456.zaloclone.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import hcmute.edu.vn.thanh0456.zaloclone.utilities.Constants;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.PreferenceManager;

// Activity này được dùng để theo dõi và cập nhật tình trạng on/off của người dùng
public class BaseActivity extends AppCompatActivity {

    private DocumentReference documentReference;

    // Tạo lập các biến cần thiết, sử dụng để cập nhật tình trạng người dùng
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lưu hoặc lấy dữ liệu trong SharePrefs
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        // Tương tác với dữ liệu lưu trên Firestore
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        // ánh xạ đến document có id là id của người dùng trong collection user
        documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
    }

    // Khi người dùng hiện không ở trong ứng dụng
    @Override
    protected void onPause() {
        super.onPause();
        documentReference.update(Constants.KEY_AVAILABILITY, 0);
    }

    // Khi người dùng mở lại ứng dụng
    @Override
    protected void onResume() {
        super.onResume();
        documentReference.update(Constants.KEY_AVAILABILITY, 1);
    }
}
