package com.dyt.bluetooth.le;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;

class MyAdapter extends BaseAdapter {

    Context context;
    int layout;
    ArrayList <String[]>  al;
    LayoutInflater inf;

    public MyAdapter(Context context, int layout, ArrayList al) {
        this.context = context;
        this.layout = layout;
        this.al = al;
        this.inf = (LayoutInflater)context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return al.size();
    }
    @Override
    public Object getItem(int position) {
        return al.get(position);
    }
    @Override
    public long getItemId(int position){
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final int pos = position ;
        if (convertView == null) {
            convertView = inf.inflate(layout, null);
        }
        TextView tv = (TextView)convertView.findViewById(R.id.textView1);
        Button btn =(Button)convertView.findViewById(R.id.button);
        btn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.d("df",Integer.toString(pos + 1) + "번 아이템이 선택되었습니다.");
            }
        });

        tv.setText(al.get(position)[0].toString());
        btn.setText(al.get(position)[1].toString());

        return convertView;
    }
} // end class MyAdapter