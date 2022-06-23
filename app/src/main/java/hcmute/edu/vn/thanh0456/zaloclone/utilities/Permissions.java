package hcmute.edu.vn.thanh0456.zaloclone.utilities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Permissions {
    // Kiểm tra đã cấp quyền record cho ứng dụng hay chưa
    public static boolean isRecordingok (Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    // Nếu chưa, yêu cầu cấp quyền cho ứng dụng
    public static void requestRecording(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
    }
}
