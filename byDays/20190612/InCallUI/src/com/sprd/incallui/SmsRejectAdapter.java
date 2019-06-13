package com.sprd.incallui;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.android.incallui.R;

public class SmsRejectAdapter extends ArrayAdapter<String> {

    public static final int SMS_PREFIX_LENGTH = 9;
    public static final String SMS_PREFIX = "sprd_msg:";
    private int mListTextColor;
    
    public SmsRejectAdapter(Context context, int resource,
            int textViewResourceId, List<String> objects) {
        super(context, resource, textViewResourceId, objects);
        mListTextColor = context.getResources().getColor(R.color.incoming_call_list_text_color_sprd);
    }
    @Override
    public View getView (int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) super.getView(position, convertView, parent);
        textView.setTextColor(mListTextColor);
        String str = (String) textView.getText();
        String savedString = (String) textView.getText();
        String displayString;
        String stringIndex;
        if(savedString.startsWith(SMS_PREFIX)){
            displayString = savedString.substring(SMS_PREFIX_LENGTH+1);
            stringIndex = String.valueOf(savedString.charAt(SMS_PREFIX_LENGTH));
            textView.setTag(stringIndex);
            textView.setText(displayString);
        }else{
            textView.setTag(null);
        }

        return textView;
    }


}
