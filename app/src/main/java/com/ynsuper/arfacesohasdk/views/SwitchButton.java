

package com.ynsuper.arfacesohasdk.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.ynsuper.arfacesohasdk.R;


public class SwitchButton extends View {
    private Bitmap mSwitchIcon;

    private int mSwitchIconWidth;

    private int mSwitchIconXPision;

    private boolean mSwitchButtonCurrentState = false;

    private Paint mPaint;

    private OnSwitchButtonStateChangeListener mListener;

    public SwitchButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initView();
    }

    public void setCurrentState(boolean currentState) {
        this.mSwitchButtonCurrentState = currentState;
    }

    private void initView() {
        this.mSwitchIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.swich_slider_new);
        this.mSwitchIconWidth = this.mSwitchIcon.getWidth();
        this.mPaint = new Paint();
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStrokeWidth(2);
        // init value
        this.mSwitchIconXPision = 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.setMeasuredDimension(this.mSwitchIconWidth * 2, this.mSwitchIconWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        RectF re3 = new RectF(0, 0, this.mSwitchIconWidth * 2, this.mSwitchIconWidth);
        if (this.mSwitchButtonCurrentState) {
            this.mPaint.setColor(this.getResources().getColor(R.color.button_background));
            this.mSwitchIconXPision = this.mSwitchIconWidth - 1;
        } else {
            this.mPaint.setColor(this.getResources().getColor(R.color.white));
            this.mSwitchIconXPision = 0;
        }
        canvas.drawRoundRect(re3, this.mSwitchIconWidth / 2.0f, this.mSwitchIconWidth / 2.0f, this.mPaint);
        canvas.drawBitmap(this.mSwitchIcon, this.mSwitchIconXPision, 1.5f, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            this.mSwitchButtonCurrentState = !this.mSwitchButtonCurrentState;
            this.mListener.onSwitchButtonStateChange(this.mSwitchButtonCurrentState);
        }
        this.invalidate();
        return true;
    }

    public void setOnSwitchButtonStateChangeListener(OnSwitchButtonStateChangeListener listener) {
        this.mListener = listener;
    }

    public interface OnSwitchButtonStateChangeListener {
        /**
         * Switch state change callback method
         * @param state draw facePoints or not
         */
        void onSwitchButtonStateChange(boolean state);
    }
}
