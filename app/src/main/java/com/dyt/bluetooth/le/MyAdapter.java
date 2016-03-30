package com.dyt.bluetooth.le;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

class MyAdapter extends BaseAdapter {

    Context context;   // ���� ȭ���� �������
    int layout;         // �� ���� �׷��� ���̾ƿ�
    ArrayList <String[]>  al;       // �ٷ��� ������
    LayoutInflater inf;     // layout xml ������ ��ü�� ��ȯ�Ҷ� �ʿ�

    public MyAdapter(Context context, int layout, ArrayList al) {// �ʱ�ȭ
        this.context = context;
        this.layout = layout;
        this.al = al;
        this.inf = (LayoutInflater)context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() { // ListView ���� ����� �������� �Ѱ���
        return al.size();
    }
    @Override
    public Object getItem(int position) { // �ش� position��°�� ������ ��
        return al.get(position);
    }
    @Override
    public long getItemId(int position){// �ش� position��°�� ����ũ��id ��
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //�ش��� ����,   �ش��� ���̾ƿ�,          ����Ʈ��
        // ������ ȭ���� �����ϴ� �޼��� (���� �߿�)

        if (convertView == null) {
            convertView = inf.inflate(layout, null);
            //xml���Ϸ� ���̾ƿ���ü ����
        }
        TextView tv = (TextView)convertView.findViewById(R.id.textView1);
        Button btn =(Button)convertView.findViewById(R.id.button);

        tv.setText(al.get(position)[0].toString());
        btn.setText(al.get(position)[1].toString()); // �ش��°�� ���� ����

        return convertView;
    }
} // end class MyAdapter