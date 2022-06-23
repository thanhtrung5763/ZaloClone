package hcmute.edu.vn.thanh0456.zaloclone.network;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

// interface thiết lập cấu trúc nội dung tin nhắn, thông báo
// để gửi đến thiết bị khác
public interface APIService {

    @POST("send")
    Call<String> sendMessage(
            @HeaderMap HashMap<String, String> headers,
            @Body String messageBody
    );
}
