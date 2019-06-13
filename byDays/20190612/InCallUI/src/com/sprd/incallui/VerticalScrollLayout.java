/*
 * Copyright (C) 2013 Spreadtrum Communications Inc. 
 *
 */

package com.sprd.incallui;

import android.content.Context;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;

import com.android.incallui.CallCommandClient;
import com.android.incallui.R;

public class VerticalScrollLayout extends ViewGroup {

    private static final String TAG = "ScrollLayout";
    private static final boolean DBG = true;//Debug.isDebug() || Constants.IS_DEBUG;
    private VelocityTracker mVelocityTracker; //
    private static final int SNAP_VELOCITY = 200;
    private static final int MOVE_THRESHOLD = 15;
    private Scroller mScroller; //
    private int mCurScreen;
    private int mDefaultScreen = 0;
    private float mLastMotionY;
    private OnViewChangeListener mOnViewChangeListener;
    private int mAccept = LOCKSCREEN_NOACTION;
    private int mLastAccept = LOCKSCREEN_NOACTION;
    public static final int LOCKSCREEN_REJECT = 0;
    public static final int LOCKSCREEN_ANSWER = 1;
    public static final int LOCKSCREEN_NOACTION = -1;
    public static final int LOCKSCREEN_MAIN = 0;
    public static final int LOCKSCREEN_SMS = 1;
    public static final int DELAYREFRESH = 100;
    public static final int DELAYREFRESH_DELAYTIME = 10000;
    private boolean bDelayRefresh = false;
    private boolean mTriggered = false;
    private boolean mIsInitialize = false;
    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DELAYREFRESH) {
                snapToScreen(LOCKSCREEN_MAIN);
            }
        }
    };

    public VerticalScrollLayout(Context context) {
        super(context);
    }

    public VerticalScrollLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalScrollLayout(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(Context context, int curScreen) {
        mCurScreen = LOCKSCREEN_MAIN;
        mScroller = new Scroller(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            int childTop = 0;
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View childView = getChildAt(i);
                if (childView.getVisibility() != View.GONE) {
                    final int childHeight = childView.getMeasuredHeight();
                    childView.layout(0, childTop, childView.getMeasuredWidth(),
                            childTop + childHeight);
                    childTop += childHeight;
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
        scrollTo(0, mCurScreen * height);
    }

    public void snapToDestination() {
        final int screenHeight = getHeight();
        final int destScreen = (getScrollY() + screenHeight / 2) / screenHeight;
        snapToScreen(destScreen);
    }

    public void snapToScreen(int whichScreen) {
        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        int scrollY = getScrollY();
        int height = getHeight();
        if(DBG) Log.d(TAG,"snapToScreen,scrollY="+scrollY+" height="+height+" whichScreen="+whichScreen);
        if (scrollY != (whichScreen * height)) {
            final int delta = whichScreen * height - scrollY;
            mScroller.startScroll(0, scrollY, 0, delta,
                    Math.abs(delta) * 2);

            mCurScreen = whichScreen;
            invalidate();
        }
        if(mIsInitialize && whichScreen == 1 && CallCommandClient.getInstance().isRingtonePlaying()){
            CallCommandClient.getInstance().silenceRinger();
        }
        mIsInitialize = true;
    }

    public void setScreen(int view) {
        mCurScreen = view;
        snapToScreen(mCurScreen);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event){
        super.onInterceptTouchEvent(event);
        onTouchEvent(event);
        if(mScroller.isFinished()){
            return false;
        } else{
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();

        switch (action) {
        case MotionEvent.ACTION_DOWN:

            mAccept = LOCKSCREEN_NOACTION;
            if (inRejectCallRect((int) x, (int) y)) {
                mAccept = LOCKSCREEN_REJECT;
                setBackground(true, false);
            }else if (inAnserCallRect((int) x, (int) y)) {
                mAccept = LOCKSCREEN_ANSWER;
                setBackground(true, true);
            }
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(event);
            }
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            mLastMotionY = y;
            break;
        case MotionEvent.ACTION_MOVE:
            int deltaY = (int) (mLastMotionY - y);
            if (IsCanMove(deltaY)) {
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(event);
                }
                mLastMotionY = y;
                scrollBy(0, deltaY);
            }


            if(isEnableToUnlock(y))
            {
                bDelayRefresh = true;
                if (mOnViewChangeListener != null && LOCKSCREEN_NOACTION != mAccept) {
                    if(DBG) Log.i(TAG, "mOnViewChangeListener.ACTION_MOVE-->mAccept="+mAccept
                            +"   mLastAccept="+mLastAccept+"  deltaY="+deltaY);
                    if(mLastAccept != mAccept){
                        mLastAccept = mAccept;
                        mTriggered = true;
                        mOnViewChangeListener.OnViewChange(mCurScreen,mAccept);
                    }
                }
            }else{
                bDelayRefresh = false;
            }
            break;
        case MotionEvent.ACTION_UP:
            setBackground(false, false);

            int velocityY = 0;
            if (mVelocityTracker != null) {
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                velocityY = (int) mVelocityTracker.getYVelocity();
            }
            if(DBG) Log.d(TAG,"velocityY="+velocityY+"   mCurScreen="+mCurScreen);
            if (velocityY > SNAP_VELOCITY && mCurScreen > 0) {
                snapToScreen(mCurScreen - 1);
            } else if (velocityY < -SNAP_VELOCITY
                    && mCurScreen < getChildCount() - 1) {
                snapToScreen(mCurScreen + 1);
            } else {
                if(mTriggered){
                    mTriggered = false;
                } else {
                    snapToDestination();
                }
            }

            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }

            //back to LockScreen Activity
            if(bDelayRefresh && null != mRefreshHandler){
                this.mRefreshHandler.sendEmptyMessageDelayed(DELAYREFRESH, DELAYREFRESH_DELAYTIME);
            }
            break;
        }
        return true;
    }

    private boolean IsCanMove(int deltaY) {
        if(Math.abs(deltaY)> MOVE_THRESHOLD){
            return true;
        } else {
            return false;
        }
    }

    public void SetOnViewChangeListener(OnViewChangeListener listener) {
        mOnViewChangeListener = listener;
    }

    private boolean inAnserCallRect(int x, int y) {
        View answerView = this.findViewById(R.id.AcceptCall);
        View background = this.findViewById(R.id.lockscreenLayout);
        if (null != answerView && mCurScreen == LOCKSCREEN_MAIN) {
            Rect answerRect = new Rect(answerView.getLeft(),
                    background.getTop(), background.getRight(),
                    background.getBottom());
            boolean isAnswerRect = answerRect.contains(x, y);
            if(DBG) Log.i(TAG,"answerRect="+answerRect+" (x="+x+",y="+y+")answerRect : " + isAnswerRect);
            return isAnswerRect;
        } else {
            return false;
        }
    }

    private boolean inRejectCallRect(int x, int y) {
        View rejectView = this.findViewById(R.id.RejectCall);
        View background = this.findViewById(R.id.lockscreenLayout);
        if (null != rejectView && mCurScreen == LOCKSCREEN_MAIN) {
            Rect rejectRect = new Rect(background.getLeft(),
                    background.getTop(), rejectView.getRight(),
                    background.getBottom());
            boolean isRejectRect = rejectRect.contains(x, y);
            if(DBG) Log.i(TAG,"rejectRect="+rejectRect+" (x="+x+",y="+y+") isRejectRect : " + isRejectRect);
            return isRejectRect;
        } else {
            return false;
        }
    }

    private boolean inMiddleRect(int x, int y) {
        View middleView = this.findViewById(R.id.comments);
        View background = this.findViewById(R.id.lockscreenLayout);
        if (null != middleView && mCurScreen == LOCKSCREEN_MAIN) {
            Rect middleRect = new Rect(middleView.getLeft(),
                    background.getTop(), middleView.getRight(),
                    background.getBottom());
            boolean isMiddleRect = middleRect.contains(x, y);
            if(DBG) Log.i(TAG, "middleRect="+middleRect+" (x="+x+",y="+y+") isMiddleRect : " + isMiddleRect);
            return isMiddleRect;
        } else {
            return false;
        }
    }

    private void setBackground(boolean bIsPressed, boolean bIsAccept) {
        ImageView rejectImageView = (ImageView) this
                .findViewById(R.id.RejectCall);
        TextView smsRjectTitle = (TextView)this.findViewById(R.id.smsRjectTitle);
        ImageView smsRjectTitleIndicator = (ImageView)this.findViewById(R.id.smsRjectTitleIndicator);
        if (null != rejectImageView) {
            rejectImageView
            .setImageResource(bIsPressed ? R.drawable.ic_lock_call_reject_pressed_sprd
                    : R.drawable.ic_lock_call_reject_normal_sprd);
        }
        ImageView acceptImageView = (ImageView) this
                .findViewById(R.id.AcceptCall);
        if (null != acceptImageView) {
            acceptImageView
            .setImageResource(bIsPressed ? R.drawable.ic_lock_call_answer_pressed_sprd
                    : R.drawable.ic_lock_call_answer_normal_sprd);
        }
        View background = this.findViewById(R.id.lockscreenLayout);
        if (null != background) {
            if (bIsPressed) {
                if(bIsAccept){
                    background.setBackgroundResource(R.drawable.ic_lock_bg_answer_sprd);
                }else{
                    background.setBackgroundResource(R.drawable.ic_lock_bg_reject_sprd);
                }
                if(smsRjectTitle != null) smsRjectTitle.setVisibility(View.INVISIBLE);
                if(smsRjectTitleIndicator != null) smsRjectTitleIndicator.setVisibility(View.INVISIBLE);
            } else {
                background.setBackgroundResource(R.drawable.sms_reject_bg_sprd);
                if(smsRjectTitle != null) smsRjectTitle.setVisibility(View.VISIBLE);
                if(smsRjectTitleIndicator != null) smsRjectTitleIndicator.setVisibility(View.VISIBLE);
            }
        }
    }


    private boolean isEnableToUnlock(float y) {
        boolean isEnableToUnlock = false;
        int scrollY = getScrollY();
        View background = this.findViewById(R.id.lockscreenLayout);
        if ((y > (background.getTop()+background.getHeight()/2)) 
                && Math.abs(scrollY) > background.getHeight()/2
                && mCurScreen == LOCKSCREEN_MAIN 
                && scrollY < 0) {
            isEnableToUnlock = true;
        }
        if(DBG) Log.i(TAG,"isEnableToUnlock :"+isEnableToUnlock+" getScrollY()="+scrollY);
        return isEnableToUnlock;
    }

    public void init(){
        mLastAccept = LOCKSCREEN_NOACTION;
        mCurScreen = LOCKSCREEN_MAIN;
        snapToScreen(mCurScreen);
    }

    public void resetScrollLayout(){
        setScreen(LOCKSCREEN_MAIN);
        mLastAccept = LOCKSCREEN_NOACTION;
    }
}
