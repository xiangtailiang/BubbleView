package tiger.radio.bubbleview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.ScaleAnimation;

public class MainActivity extends AppCompatActivity {

    BubbleView mBubbleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBubbleView = (BubbleView) findViewById(R.id.bubbleView);

        mBubbleView.setBubbleDrawable(getResources().getDrawable(R.drawable.qz_lv_em_like_1), dp2px(30), dp2px(25));
        mBubbleView.setAnimationListener(new BubbleView.AnimationListener() {
            @Override
            public void onAnimationStart(BubbleView bubbleView) {
                Log.d("BubbleView", "onAnimationStart");
            }

            @Override
            public void onAnimationEnd(BubbleView bubbleView) {
                Log.d("BubbleView", "onAnimationEnd");
            }
        });
    }


    public void danmu_click_1(View view) {
        showClickAnimation(view);
        addBubbleView(view);
    }

    public void danmu_click_2(View view) {
        showClickAnimation(view);
        addBubbleView(view);
    }

    public void danmu_click_3(View view) {
        showClickAnimation(view);
        addBubbleView(view);
    }

    private void showClickAnimation(View view) {
        view.clearAnimation();
        Animation animation = new ScaleAnimation(1f, 1.1f, 1f, 1.1f);
        animation.setDuration(500);
        animation.setInterpolator(new BounceInterpolator());
        view.setAnimation(animation);
        animation.start();
    }

    private void addBubbleView(View view) {
        int left = view.getLeft();
        int top = view.getTop();
        int right = view.getRight();
        int bottom = view.getBottom();
        int height = view.getHeight();

        Log.d("BubbleView", "addBubbleView[left,top,right,bottom]=" + left + " , " + top + " , " + right + " , " + bottom);
        Log.d("BubbleView", "height= " + height);
        mBubbleView.addBubble(right - dp2px(15), top);
    }

    public void release() {
    }


    private int dp2px(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }


}
