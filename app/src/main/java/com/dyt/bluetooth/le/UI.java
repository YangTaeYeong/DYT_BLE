package com.dyt.bluetooth.le;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by user on 2016-03-30.
 */
public class UI {

    private LinearLayout topLL;
    private Context context;

    public UI(Context context){
        this.context = context;
    }

    protected void makeTitleView(String str) {
        LinearLayout.LayoutParams titleParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout titleLayout = new LinearLayout(context);
        TextView title = new TextView(context);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(titleParam);
        titleLayout.setLayoutParams(titleParam);
        title.setBackgroundColor(Color.parseColor("#000000"));
        title.setTextColor(Color.parseColor("#FF7200"));
        title.setTextSize(20);
        title.setText(str);
        topLL.addView(titleLayout);
        titleLayout.addView(title);
    }

    protected void makeDetailView(String str1, String str2) {

        LinearLayout.LayoutParams titleParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        titleParam.gravity = Gravity.CENTER;
        LinearLayout detailLayout = new LinearLayout(context);
        detailLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView detailTv = new TextView(context);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        detailTv.setBackgroundColor(Color.parseColor("#FFFFFF"));
        detailTv.setTextColor(Color.parseColor("#000000"));
        detailTv.setTextSize(13);
        detailTv.setText(str1);
        detailTv.setGravity(Gravity.CENTER);

        param.weight = 1;
        detailTv.setLayoutParams(param);
        Button detailBtn = new Button(context);
        detailBtn.setBackgroundColor(Color.parseColor("#EEEEEE"));
        detailBtn.setTextColor(Color.parseColor("#000000"));
        detailBtn.setTextSize(13);
        detailBtn.setText(str2);
        detailBtn.setGravity(Gravity.CENTER);
        param.weight = 1;
        detailBtn.setLayoutParams(param);

        topLL.addView(detailLayout);
        detailLayout.addView(detailTv);
        detailLayout.addView(detailBtn);
        //¤±¤± zzaas
        //dfdf
    }
}
