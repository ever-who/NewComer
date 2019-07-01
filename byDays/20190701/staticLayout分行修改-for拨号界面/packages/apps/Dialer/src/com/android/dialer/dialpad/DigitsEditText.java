/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dialer.dialpad;

import android.content.Context;
import android.graphics.Rect;
import android.text.BirdDynamicLayout;
import android.text.DynamicLayout;
import android.text.InputType;
import android.text.Layout;
import android.text.TextDirectionHeuristic;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
/* star/+/P/W add by meifangting 20190318 begin*/
import android.view.KeyEvent;
/* star/+/P/W add by meifangting 20190318 end*/

/**
 * EditText which suppresses IME show up.
 */
public class DigitsEditText extends EditText {
    public DigitsEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setShowSoftInputOnFocus(false);
        Log.d("TextView", "DigitsEditText: "+this);//mark
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        final InputMethodManager imm = ((InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE));
        if (imm != null && imm.isActive(this)) {
            imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final boolean ret = super.onTouchEvent(event);
        // Must be done after super.onTouchEvent()
        final InputMethodManager imm = ((InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE));
        if (imm != null && imm.isActive(this)) {
            imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
        }
        return ret;
    }
   /* star/+/P/W add by meifangting 20190318 begin*/
    public static final String BIRD_CUSTOM_DIALERDIGIT_LONGP_CHANGE = android.os.SystemProperties.get("ro.bdmisc.longp_digit_change","none");
    private int mKeyDownTimes = 0;
    private boolean firstKeyDown = false;
    private String befoteText;
    private String key0StrsNormal[] = {"0"};
    private String key0Strs[] = {"0","+","P","W"};
    private String keyStarStrs[] = {"*","+"};
    private String keyStarStrsBihee[] = {"*","+","P","W"};
	public interface OnTextToSpeech {
           void speechNumber(String number);
    };
	
	private OnTextToSpeech mOnTextToSpeech;

	public void setTextSpeechListener(OnTextToSpeech textToSpeech){
	      mOnTextToSpeech = textToSpeech;
	}
    private void handCustomkeyInput(String customStr[]){
        String currentText = getText().toString();
        int selectionPos = getSelectionStart();
        if(befoteText!=null&&currentText!=null){
            if(befoteText.length()==currentText.length()){
                StringBuffer temp = new StringBuffer(befoteText);
				int tempLen = getText().length();
                setText(temp.insert(selectionPos,customStr[0]));
                if(getText().length()>0) {
                    setSelection(getText().length()>tempLen?selectionPos + 1:selectionPos);
                }
            }else if(befoteText.length()<currentText.length()){
                String currentChar = currentText.substring(selectionPos-1,selectionPos);
                StringBuffer temp = new StringBuffer(currentText);
                for(int i=0;i<customStr.length;i++){
                    if(customStr[i].equals(currentChar)){
                        if(i!=customStr.length-1){
                            setText(temp.replace(selectionPos-1,selectionPos,customStr[i+1]).toString());
                            setSelection(selectionPos);
                            break;
                        }
                    }
                }
            }else{
				setText(customStr[0]);
                setSelection(1);
			}
        }else{
            setText(customStr[0]);
            setSelection(1);
        }
    }
    public void handKeyInput(KeyEvent evt,String customStr[]){
        if(KeyEvent.ACTION_DOWN==evt.getAction()){
            if(!firstKeyDown) {
                mKeyDownTimes = 0;
                befoteText = getText().toString();
                firstKeyDown = true;
				if(mOnTextToSpeech!=null){
				   mOnTextToSpeech.speechNumber(customStr[0]);
				}
                
            }
            if(((mKeyDownTimes++)%8)==0){
                handCustomkeyInput(customStr);
            }

        }else if(KeyEvent.ACTION_UP==evt.getAction()){
			if(!firstKeyDown){
				if(mOnTextToSpeech!=null){
				   mOnTextToSpeech.speechNumber(customStr[0]);
				}
				handCustomkeyInput(customStr);;
			}
            firstKeyDown = false;
            mKeyDownTimes =0;
        }
    }


    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent evt) {
		android.util.Log.v("mfttest","dispatchKeyEventPreIme:"+evt.getKeyCode());
        if (evt.getKeyCode() == KeyEvent.KEYCODE_0) {
            if("philips".equals(BIRD_CUSTOM_DIALERDIGIT_LONGP_CHANGE)) {
                handKeyInput(evt,key0Strs);
            }else{
                handKeyInput(evt,key0StrsNormal);
            }
            return true;
        }else if (evt.getKeyCode() == KeyEvent.KEYCODE_STAR) {
            if("bihee".equals(BIRD_CUSTOM_DIALERDIGIT_LONGP_CHANGE)) {
                handKeyInput(evt,keyStarStrsBihee);
                return true;
            }else if(!"philips".equals(BIRD_CUSTOM_DIALERDIGIT_LONGP_CHANGE)){
                handKeyInput(evt,keyStarStrs);
                return true;
            }
        }
        return super.dispatchKeyEventPreIme(evt);
    }
/* star/+/P/W add by meifangting 20190318 end*/

    /**
     * add by hujingcheng for land ui fill bottom lines first 20190625
     * @return
     */
    @Override
    public BirdDynamicLayout getCustomizeDynamicLayout(CharSequence base, CharSequence display,
                                                   TextPaint paint,
                                                   int width, Layout.Alignment align, TextDirectionHeuristic textDir,
                                                   float spacingmult, float spacingadd,
                                                   boolean includepad,
                                                   TextUtils.TruncateAt ellipsize, int ellipsizedWidth){
        return new BirdDynamicLayout(base,display,paint,width,align,textDir,spacingmult,spacingadd,includepad,ellipsize,ellipsizedWidth);
    }
}
