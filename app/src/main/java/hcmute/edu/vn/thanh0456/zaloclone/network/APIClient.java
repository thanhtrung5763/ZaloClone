package hcmute.edu.vn.thanh0456.zaloclone.network;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

// Tạo và sử dụng API của Google để gửi thông báo hoặc dữ liệu đến thiết bị khác dựa trên FCM_TOKEN
public class APIClient {

    private static Retrofit retrofit = null;

    public static  Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://fcm.googleapis.com/fcm/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
