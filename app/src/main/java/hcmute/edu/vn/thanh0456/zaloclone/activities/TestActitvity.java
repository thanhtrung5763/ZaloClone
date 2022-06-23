package hcmute.edu.vn.thanh0456.zaloclone.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.devlomi.record_view.OnBasketAnimationEnd;
import com.devlomi.record_view.OnRecordClickListener;
import com.devlomi.record_view.OnRecordListener;
import com.devlomi.record_view.RecordButton;
import com.devlomi.record_view.RecordView;

import hcmute.edu.vn.thanh0456.zaloclone.R;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.TestRecordLayoutBinding;
import hcmute.edu.vn.thanh0456.zaloclone.utilities.Permissions;

public class TestActitvity extends AppCompatActivity {

    private TestRecordLayoutBinding binding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = TestRecordLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        RecordView recordView = (RecordView) findViewById(R.id.record_view);
        RecordButton recordButton = (RecordButton) findViewById(R.id.record_button);

//IMPORTANT
        recordButton.setRecordView(recordView);
        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                //Start Recording..
                Log.d("RecordView", "onStart");
                binding.layoutBottomLeft.setVisibility(View.GONE);
                binding.inputMessage.setVisibility(View.GONE);
                binding.layoutLikeAndSend.setVisibility(View.GONE);

            }

            @Override
            public void onCancel() {
                //On Swipe To Cancel
                Log.d("RecordView", "onCancel");
            }

            @Override
            public void onFinish(long recordTime) {
                //Stop Recording..
                //limitReached to determine if the Record was finished when time limit reached.
                String time = "time";
                Log.d("RecordView", "onFinish");

                Log.d("RecordTime", time);
                binding.layoutBottomLeft.setVisibility(View.VISIBLE);
                binding.inputMessage.setVisibility(View.VISIBLE);
                binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLessThanSecond() {
                //When the record time is less than One Second
                Log.d("RecordView", "onLessThanSecond");
                binding.layoutBottomLeft.setVisibility(View.VISIBLE);
                binding.inputMessage.setVisibility(View.VISIBLE);
                binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
            }
        });

        recordButton.setListenForRecord(false);

        //ListenForRecord must be false ,otherwise onClick will not be called
        recordButton.setOnRecordClickListener(new OnRecordClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TestActitvity.this, "RECORD BUTTON CLICKED", Toast.LENGTH_SHORT).show();
                Log.d("RecordButton","RECORD BUTTON CLICKED");
            }
        });
        recordView.setOnBasketAnimationEndListener(new OnBasketAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                Log.d("RecordView", "Basket Animation Finished");
                binding.layoutBottomLeft.setVisibility(View.VISIBLE);
                binding.inputMessage.setVisibility(View.VISIBLE);
                binding.layoutLikeAndSend.setVisibility(View.VISIBLE);
            }
        });

        binding.recordButton.setOnClickListener(v -> {
            if (Permissions.isRecordingok(TestActitvity.this)) {
                binding.recordButton.setListenForRecord(true);
            } else {
                Permissions.requestRecording(TestActitvity.this);
            }
        });
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
    }
}
