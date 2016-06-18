package tiger.radio.bubbleview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * BubbleView
 * 点赞的气泡view
 * Created by tiger on 6/16/16.
 */
public class BubbleView extends View {

    private static final String TAG = "BubbleView";

    public static final int MSG_UPDATE = 1;

    public static final int DURATION = 2000;
    public static final int STEP_PERIOD = 30;

    private final static float ScaleInit = 0.3f;
    private final static float ScaleTarget = 1.3f;
    private final static float ScaleRange = ScaleTarget - ScaleInit;

    private final static int AlphaInit = 255;
    private final static int AlphaTarget = 0;
    private final static int AlphaRange = AlphaTarget - AlphaInit;

    private float mVerticalOffset = 2.0f;
    private float mHorizontalOffset = 2.0f;


    public boolean mRunning = false;

    private Drawable mBubbleDrawable;
    private int mBubbleDrawableWidth;
    private int mBubbleDrawableHeight;

    private int mWidth = -1;
    private int mHeight = -1;

    AnimationListener mAnimationListener;

    private ArrayList<Bubble> mBubbles = new ArrayList<>();

    private AccelerateDecelerateInterpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private LinearInterpolator mAlphaInterpolator = new LinearInterpolator();

    public BubbleView(Context context) {
        super(context);
        init();
    }

    public BubbleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public BubbleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {

    }

    public void setBubbleDrawable(Drawable bubbleDrawable, int width, int height) {
        if (bubbleDrawable == null) {
            return;
        }

        mBubbleDrawable = bubbleDrawable;
        mBubbleDrawableWidth = width;
        mBubbleDrawableHeight = height;
        mBubbleDrawable.setBounds(0, 0, mBubbleDrawableWidth, mBubbleDrawableHeight);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBubbles(canvas);
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE && mRunning) {
                updateBubbles();
                this.sendEmptyMessageDelayed(MSG_UPDATE, STEP_PERIOD);
            }
        }

    };

    private void drawBubbles(Canvas canvas) {
        Iterator<Bubble> iterator = mBubbles.iterator();
        while (iterator.hasNext()) {
            Bubble bubble = iterator.next();
            if (bubble.curTime > 0 && bubble.curTime <= DURATION) {
                canvas.save();
                canvas.translate(bubble.position.x, bubble.position.y);
                bubble.drawable.setBounds(0, 0, (int) (mBubbleDrawableWidth * bubble.scale), (int) (mBubbleDrawableHeight * bubble.scale));
                bubble.drawable.setAlpha(bubble.alpha);
                bubble.drawable.draw(canvas);
                canvas.restore();
            }
        }

    }


    private void updateBubbles() {
        Iterator<Bubble> iterator = mBubbles.iterator();
        boolean needToDraw = false;
        while (iterator.hasNext()) {
            Bubble bubble = iterator.next();
            if (bubble.curTime <= DURATION) {
                initBezierPointIfNeed(bubble);

                bubble.curTime = bubble.curTime + STEP_PERIOD;
                float factor = mInterpolator.getInterpolation(bubble.curTime / (float) (DURATION));
                bubble.scale = ScaleInit + ScaleRange * factor;
                calculateBezierPoint(bubble.position, factor, bubble.startPoint, bubble.ctrlPoint1, bubble.ctrlPoint2, bubble.endPoint);

                //The last Half of the period make transparency change
                if (bubble.curTime > DURATION / 2) {
                    float alphaFactor = mAlphaInterpolator.getInterpolation((bubble.curTime - DURATION / 2) / (float) (DURATION / 2));
                    bubble.alpha = AlphaInit + (int) (AlphaRange * alphaFactor);
                }
                needToDraw = true;
            } else {
                iterator.remove();
            }
        }

        if (needToDraw) {
            postInvalidate();
        } else {
            mRunning = false;
            if (mAnimationListener != null) {
                mAnimationListener.onAnimationEnd(this);
            }
        }
    }


    public void addBubble(int posX, int posY) {
        Bubble bubble = new Bubble();
        bubble.position = new Point();
        bubble.startPoint = new Point(posX, posY);
        bubble.alpha = AlphaInit;
        bubble.scale = ScaleInit;
        bubble.drawable = mBubbleDrawable.mutate();
        bubble.drawable.setBounds(0, 0, mBubbleDrawableWidth, mBubbleDrawableHeight);
        mBubbles.add(bubble);

        start();
    }

    public void start() {
        if (!mHandler.hasMessages(MSG_UPDATE)) {
            mRunning = true;
            mHandler.sendEmptyMessage(MSG_UPDATE);
            if (mAnimationListener != null) {
                mAnimationListener.onAnimationStart(this);
            }
        }
    }

    public void pause() {
        mRunning = false;
        mHandler.removeMessages(MSG_UPDATE);
    }

    public void stop() {
        mRunning = false;
        mBubbles.clear();
        mHandler.removeMessages(MSG_UPDATE);
        if (mAnimationListener != null) {
            mAnimationListener.onAnimationEnd(this);
        }
    }

    public boolean isRunning() {
        return mRunning;
    }

    public class Bubble {
        public Point position;
        /**
         * scale
         */
        public float scale;

        /**
         * 0-255
         */
        public int alpha;

        public int curTime;

        public Drawable drawable;


        /**
         * for Bezier
         */
        public Point startPoint;
        public Point ctrlPoint1;
        public Point ctrlPoint2;
        public Point endPoint;

    }

    /**
     * 计算贝塞尔曲线
     *
     * @param t  时间，范围0-1
     * @param s  起始点
     * @param c1 拐点1
     * @param c2 拐点2
     * @param e  终点
     */
    public void calculateBezierPoint(Point output, float t, Point s, Point c1, Point c2, Point e) {
        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;

        float x = s.x * uuu + 3 * c1.x * t * uu + 3 * c2.x * tt * u + e.x * ttt;
        float y = s.y * uuu + 3 * c1.y * t * uu + 3 * c2.y * tt * u + e.y * ttt;

        output.x = (int) x;
        output.y = (int) y;
    }

    private void initBezierPointIfNeed(Bubble bubble) {
        if (bubble.ctrlPoint1 != null) {
            return;
        }

        int xPosMax = (int) (bubble.startPoint.x + mBubbleDrawableWidth * mHorizontalOffset);

        if (xPosMax > mWidth) {
            bubble.startPoint.x = (int) (bubble.startPoint.x - mBubbleDrawableWidth * mHorizontalOffset);
        }
        bubble.ctrlPoint1 = new Point();
        bubble.ctrlPoint1.x = (int) (bubble.startPoint.x + mBubbleDrawableWidth * mHorizontalOffset);
        bubble.ctrlPoint1.y = (int) (bubble.startPoint.y - mBubbleDrawableHeight * mVerticalOffset);

        bubble.ctrlPoint2 = new Point();
        bubble.ctrlPoint2.x = (int) (bubble.startPoint.x - mBubbleDrawableWidth * mHorizontalOffset * 0.5f);
        bubble.ctrlPoint2.y = (int) (bubble.ctrlPoint1.y - mBubbleDrawableHeight * mVerticalOffset);

        bubble.endPoint = new Point();
        bubble.endPoint.x = (int) (bubble.ctrlPoint2.x + Math.random() * (bubble.ctrlPoint1.x - bubble.ctrlPoint2.x) * 0.5f);
        bubble.endPoint.y = (int) (bubble.ctrlPoint2.y - mBubbleDrawableHeight * mVerticalOffset);
    }

    public void setAnimationListener(AnimationListener animationListener) {
        mAnimationListener = animationListener;
    }

    /**
     * <p>An animation listener receives notifications from an animation.
     * Notifications indicate animation related events, such as the end or the
     * repetition of the animation.</p>
     */
    public interface AnimationListener {
        /**
         * <p>Notifies the start of the animation.</p>
         */
        void onAnimationStart(BubbleView bubbleView);

        /**
         * <p>Notifies the end of the animation. This callback is not invoked
         * for animations with repeat count set to INFINITE.</p>
         */
        void onAnimationEnd(BubbleView bubbleView);

    }

}
