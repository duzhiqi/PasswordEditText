package com.dzq.passwordedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;


import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dzq on 2017/5/17.
 *
 * Description:
 *
 * ----------------------------
 * | Paint.Style   |  画笔风格 |
 * ----------------------------
 * |      FILL     |  填充    |
 * ----------------------------
 * |    STROKE     |  描边    |
 * ----------------------------
 * |FILL_AND_STROKE| 填充且描边|
 * ----------------------------
 */

public class PasswordEditText extends View{

    public static final int STYLE_PASSWORD_UNDERLINE = 1;
    public static final int STYLE_PASSWORD_RECT = 2;

    private int mode = STYLE_PASSWORD_UNDERLINE;
    private int passwordLength;
    private long cursorFlashTime = 2000;//default value 1000
    private int passwordPadding;
    private int passwordSize = dp2px(40);
    private int borderColor;
    private int borderWidth;
    private int cursorPosition;
    private int cursorWidth;
    private int cursorHeight;
    private int cursorColor = Color.DKGRAY;//default value dkGray
    private boolean isCursorShowing;
    private boolean isCursorEnable;
    private boolean isInputComplete;
    private int cipherTextSize = sp2px(14); // default value 14sp
    private boolean cipherEnable;
    private static String CIPHER_STR = "*";
    private String[] password;
    private TimerTask timerTask;
    private Timer timer;
    private InputMethodManager inputManager;
    private OnPasswordListener mOnPasswordListener;

    private Paint mPaint;

    public PasswordEditText(Context context) {
        this(context, null);
    }

    public PasswordEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PasswordEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        readAttri(attrs);
        init();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    private void readAttri(AttributeSet attrs){
        if (attrs != null){
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.PasswordEditText);
            borderWidth = ta.getDimensionPixelSize(R.styleable.PasswordEditText_border_width, 2);
            passwordLength = ta.getInteger(R.styleable.PasswordEditText_password_length, 6);
            cursorFlashTime = ta.getInteger(R.styleable.PasswordEditText_cursor_flash_time, 1000);
            borderColor = ta.getColor(R.styleable.PasswordEditText_border_color, Color.BLACK);
            cursorColor = ta.getColor(R.styleable.PasswordEditText_cursor_color, Color.DKGRAY);
            isCursorEnable = ta.getBoolean(R.styleable.PasswordEditText_isCursorEnable, true);
            if (mode == STYLE_PASSWORD_UNDERLINE) {
                passwordPadding = ta.getDimensionPixelSize(R.styleable.PasswordEditText_password_padding, dp2px(15));
            } else {
                passwordPadding = ta.getDimensionPixelSize(R.styleable.PasswordEditText_password_padding, 0);
            }
            cipherEnable = ta.getBoolean(R.styleable.PasswordEditText_cipherEnable, true);
            ta.recycle();
        }
        init();
        password = new String[passwordLength];
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = 0;
        switch (widthMode){
            case MeasureSpec.UNSPECIFIED:
            case MeasureSpec.AT_MOST:
                //为指定view大小
                width = passwordSize * passwordLength + passwordPadding * (passwordLength - 1);
                break;
            case MeasureSpec.EXACTLY:
                width = MeasureSpec.getSize(widthMeasureSpec);
                passwordSize = ((width - passwordPadding * (passwordLength - 1))) / passwordLength;
                break;
        }
        setMeasuredDimension(width, passwordSize);
    }

    private void init(){
        setFocusableInTouchMode(true);
        MKeyListener listener = new MKeyListener();
        setOnKeyListener(listener);
        inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        timerTask = new TimerTask() {
            @Override
            public void run() {
                isCursorShowing = !isCursorShowing;
                postInvalidate();
            }
        };
        timer = new Timer();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cipherTextSize = passwordSize / 2;
        cursorWidth = dp2px(2);
        cursorHeight = passwordSize / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mode == STYLE_PASSWORD_UNDERLINE){
            drawUnderline(canvas, mPaint);
        } else if (mode == STYLE_PASSWORD_RECT){
            drawRect(canvas, mPaint);
        }
        drawCursor(canvas, mPaint);
        drawCipherStr(canvas, mPaint);
    }

    private void drawUnderline(Canvas canvas, Paint paint){
        paint.setColor(borderColor);
        paint.setStrokeWidth(borderWidth);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        float height = getPaddingTop() + passwordSize;
        for (int i = 0; i < passwordLength; i++) {
            canvas.drawLine(getPaddingLeft() + (passwordSize + passwordPadding) * i,
                    height,
                    getPaddingLeft() + passwordSize + (passwordSize + passwordPadding) * i,
                    height, mPaint);
        }
    }

    private void drawRect(Canvas canvas, Paint paint){
        paint.setColor(borderColor);
        paint.setStrokeWidth(borderWidth);
        paint.setStyle(Paint.Style.STROKE);

        for (int i = 0; i < passwordLength; i++) {
            int l = getPaddingLeft() + (passwordSize + passwordPadding) * i;
            int t = getPaddingTop();
            int r = l + passwordSize;
            int b = t + passwordSize;
            Rect rect = new Rect(l, t, r, b);
            canvas.drawRect(rect, paint);
        }
    }

    private void drawCursor(Canvas canvas, Paint paint){
        paint.setColor(cursorColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(cursorWidth);

        if (isCursorEnable && !isCursorShowing && !isInputComplete && hasFocus()) {
            canvas.drawLine(getPaddingLeft() + passwordSize / 2 + (passwordSize + passwordPadding) * cursorPosition,
                    getPaddingTop() + (passwordSize - cursorHeight) / 2,
                    getPaddingLeft() + cursorWidth + passwordSize / 2 + (passwordSize + passwordPadding) * cursorPosition,
                    getPaddingTop() + (passwordSize + cursorHeight) / 2,
                    paint);
        }
    }

    private void drawCipherStr(Canvas canvas, Paint paint){
        paint.setColor(Color.GRAY);
        paint.setTextSize(cipherTextSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        Rect r = new Rect();
        canvas.getClipBounds(r);
        int cellH = r.height();
        paint.getTextBounds(CIPHER_STR, 0, CIPHER_STR.length(), r);
        float y = cellH/2.f + r.height()/2.f - r.bottom;

        for (int i = 0; i < password.length; i++) {
            if (!TextUtils.isEmpty(password[i])){
                canvas.drawText(cipherEnable ? CIPHER_STR : password[i],
                        getPaddingLeft() + passwordSize/2 + (passwordPadding + passwordSize) * i,
                        getPaddingTop() + y,
                        paint);
            }
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_NUMBER;
        return super.onCreateInputConnection(outAttrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        timer.scheduleAtFixedRate(timerTask, 0, cursorFlashTime);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        timer.cancel();
    }

    private int dp2px(int dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, dp, getContext().getResources().getDisplayMetrics());
    }

    private int sp2px(int sp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, sp, getContext().getResources().getDisplayMetrics());
    }

    class MKeyListener implements OnKeyListener{

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            int action = event.getAction();
            if (action == KeyEvent.ACTION_DOWN){
                if (keyCode == KeyEvent.KEYCODE_DEL){
                    if (TextUtils.isEmpty(password[0])) return true;
                    String deleteText = delete();
                    if (!TextUtils.isEmpty(deleteText) && mOnPasswordListener != null){
                        mOnPasswordListener.onPasswordChanged(deleteText);
                    }
                    postInvalidate();
                    return true;
                }

                if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9){
                    if (isInputComplete){
                        return true;
                    }
                    String addText = add((keyCode - 7) + "");
                    if (!TextUtils.isEmpty(addText) && mOnPasswordListener != null){
                        mOnPasswordListener.onPasswordChanged(addText);
                    }
                    postInvalidate();
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_ENTER){
                    if (mOnPasswordListener != null){
                        mOnPasswordListener.onKeyEnterPressed(getPassword(), isInputComplete);
                    }
                }
            }
            return false;
        }
    }

    /**
     * 获取焦点，弹出键盘
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            requestFocus();
            inputManager.showSoftInput(this, InputMethodManager.SHOW_FORCED);
        }
        return super.onTouchEvent(event);
    }

    private String add(String inputText){
        String addText = "";

        if (cursorPosition < passwordLength){
            password[cursorPosition] = inputText;
            cursorPosition ++;
            addText = inputText;
        }

        if (cursorPosition == passwordLength && mOnPasswordListener != null){
            isInputComplete = true;
            mOnPasswordListener.onPasswordComplete(getPassword());
        }
        return addText;
    }

    private String delete(){
        String deleteText = "";
        if (cursorPosition == 0){
            deleteText = password[0];
            password[0] = null;
        } else if (cursorPosition > 0){
            deleteText = password[cursorPosition - 1];
            password[cursorPosition - 1] = null;
            cursorPosition --;
        }

        isInputComplete = false;
        return deleteText;
    }

    private String getPassword(){
        StringBuilder sb = new StringBuilder();
        for (String s : password) {
            if (!TextUtils.isEmpty(s)) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    /**
     * 失去焦点，收起键盘
     * @param hasWindowFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus){
            inputManager.hideSoftInputFromInputMethod(this.getWindowToken(), 0);
        }
    }


    public void setPasswordStyle(int mode) {

        this.mode = mode;
        postInvalidate();
    }

    public void setCipherEnable(boolean cipherEnable) {

        this.cipherEnable = cipherEnable;
        postInvalidate();
    }

    public void setPasswordSize(int passwordSize) {

        this.passwordSize = passwordSize;
        postInvalidate();
    }

    public void setPasswordLength(int passwordLength) {

        this.passwordLength = passwordLength;
        postInvalidate();
    }

    public void setCursorColor(int cursorColor) {

        this.cursorColor = cursorColor;
        postInvalidate();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();

        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putStringArray("password", password);
        bundle.putInt("cursorPosition", cursorPosition);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle){
            Bundle bundle = (Bundle) state;
            password = bundle.getStringArray("password");
            cursorPosition = bundle.getInt("cursorPosition");
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    public void setOnPasswordListener(OnPasswordListener listener){
        mOnPasswordListener = listener;
    }

    public interface OnPasswordListener{
        void onPasswordChanged(String changeText);
        void onPasswordComplete(String password);
        void onKeyEnterPressed(String password, boolean isComplete);
    }
}
