package hcmute.edu.vn.thanh0456.zaloclone.adaptor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import hcmute.edu.vn.thanh0456.zaloclone.MainActivity;
import hcmute.edu.vn.thanh0456.zaloclone.databinding.ItemContainerStoryBinding;
import hcmute.edu.vn.thanh0456.zaloclone.models.Story;
import hcmute.edu.vn.thanh0456.zaloclone.models.UserStory;
import omari.hamza.storyview.StoryView;
import omari.hamza.storyview.callback.StoryClickListeners;
import omari.hamza.storyview.model.MyStory;

// Adaptor xử lí các thay đổi liên quan đến Story RecyclerView(danh sách các story của các người dùng)
// cập nhật và đính dữ liệu vào các viewHolder, hiển thị
public class TopStoryAdaptor extends RecyclerView.Adapter<TopStoryAdaptor.TopStoryViewHolder>{

    // Lưu context truyền vào
    Context context;
    // Dữ liệu về các story của mỗi user
    ArrayList<UserStory> userStories;

    // Constructor lấy context, danh sách story của mỗi người dùng được truyền vào
    public TopStoryAdaptor(Context context, ArrayList<UserStory> userStories) {
        this.context = context;
        this.userStories = userStories;
    }

    // Tạo ViewHolder
    @NonNull
    @Override
    public TopStoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TopStoryViewHolder(
                ItemContainerStoryBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull TopStoryViewHolder holder, int position) {
        // Đính dữ liệu vào ViewHolder
        UserStory userStory = userStories.get(position);
        holder.setData(userStory);
        if (userStory.stories != null) {
            holder.binding.circularStatusView.setPortionsCount(userStory.stories.size());
        }
        // Chuyển đến trang xem story của user được click vào
        holder.binding.circularStatusView.setOnClickListener(v -> {
            ArrayList<MyStory> myStories = new ArrayList<>();
            for (Story story : userStory.stories) {
                myStories.add(new MyStory(story.imageURL));
            }
            new StoryView.Builder(((MainActivity)context).getSupportFragmentManager())
                    .setStoriesList(myStories)
                    .setStoryDuration(3000)
                    .setTitleText(userStory.name)
                    .setSubtitleText("")
                    .setTitleLogoUrl(userStory.image)
                    .setStoryClickListeners(new StoryClickListeners() {
                        @Override
                        public void onDescriptionClickListener(int position) {
                        }

                        @Override
                        public void onTitleIconClickListener(int position) {
                        }
                    })
                    .build()
                    .show();
        });
    }

    // Trả về số lượng user có đăng story
    @Override
    public int getItemCount() {
        return userStories.size();
    }

    public class TopStoryViewHolder extends RecyclerView.ViewHolder {
        ItemContainerStoryBinding binding;
        // binding thiết kế ViewHolder
        public TopStoryViewHolder(ItemContainerStoryBinding itemContainerStoryBinding) {
            super(itemContainerStoryBinding.getRoot());
            binding = itemContainerStoryBinding;
        }
        // Đính dữ liệu vào ViewHolder
        void setData(UserStory userStory) {
            binding.imageProfile.setImageBitmap(getConversationImage(userStory.image));
        }
    }

    // Chuyển ảnh dạng String sang Bitmap để đính vào thiết kế ViewHolder
    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
