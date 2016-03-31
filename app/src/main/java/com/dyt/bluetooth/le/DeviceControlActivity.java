/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.dyt.bluetooth.le;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements OnClickListener, AdapterView.OnItemClickListener {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public static final byte MOD_NULL = 0x00;
    public static final byte MOD_EDFA1 = 0x0a;
    public static final byte MOD_EDFA2 = 0x0c;
    public static final byte MOD_EDFA3 = 0x0e;
    public static final byte MOD_ONU = 0x12;
    public static final byte MOD_OTX = 0x22;
    public static final byte MOD_REPEAT1 = 0x42;
    public static final byte MOD_REPEAT2 = 0x44;
    public static final byte MOD_RF_AMP = 0x1a;
    public static final byte MOD_RMC = (byte) 0x82;

    public static TextView mtv_ModName;
    private TextView mdevice_addr;
    private Button mButtonEDFA1;
    private Button mButtonEDFA2;
    private Button mButtonONU;
    private Button mButtonFTX;
    private Button mButtonREPEAT1;
    private Button mButtonRMC;

    private CountDownTimer timer;
    private String mDeviceName;
    private String mDeviceAddress;
    private final static String TAG = BluetoothLeService.class.getName();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private Network network;
    private MyAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_activity);

        ArrayList<String[]> al = new ArrayList();

        //     ListView 커스터마이징
        //    1. 다량의 데이터 (ArrayList)
        //    2. Adapter
        //    3. AdapterView 선정 (ListView)
        adapter = new MyAdapter(
                getApplicationContext(), // 현재 화면의 제어권자
                R.layout.row, // 한행을 담당할 Layout
                al); // 데이터

        ListView lv = (ListView) findViewById(R.id.listView2);
        lv.setAdapter(adapter);


        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //타이머 설정
        timer = new CountDownTimer(Integer.MAX_VALUE, 3000) {

            @Override
            public void onTick(long millisUntilFinished) {
                network.sendtomoduleCMD();
            }

            @Override
            public void onFinish() {
            }
        };

        //타이머 시작
        timer.start();
        mtv_ModName = (TextView) findViewById(R.id.data_rx);
        mdevice_addr = (TextView) findViewById(R.id.device_addr);
        mdevice_addr.setText("  " + mDeviceAddress);

        mButtonEDFA1 = (Button) findViewById(R.id.button1EDFA1);
        mButtonEDFA2 = (Button) findViewById(R.id.button2EDFA2);
        mButtonONU = (Button) findViewById(R.id.button3ONU);
        mButtonFTX = (Button) findViewById(R.id.button4FTX);
        mButtonREPEAT1 = (Button) findViewById(R.id.button5REPEAT1);
        mButtonRMC = (Button) findViewById(R.id.button6RMC);

        mButtonEDFA1.setOnClickListener(this);
        mButtonEDFA2.setOnClickListener(this);
        mButtonONU.setOnClickListener(this);
        mButtonFTX.setOnClickListener(this);
        mButtonREPEAT1.setOnClickListener(this);
        mButtonRMC.setOnClickListener(this);
        lv.setOnItemClickListener(this);

        ActionBar actionBar = getActionBar();
        getActionBar().setDisplayHomeAsUpEnabled(true);
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getActionBar().setTitle(mDeviceName);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        network = new Network(this, al, adapter);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            network.mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!network.mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            network.mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            network.mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read
    // or notification operations.

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
                    .equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                // displayGattServices(mBluetoothLeService.getSupportedGattServices());
                // jw : 데이터 수신 Enable
                network.setDataNotify();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                // jw : 데이터 수신
                network.dataRecv(intent
                        .getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (network.mBluetoothLeService != null) {
            final boolean result = network.mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        network.mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_control, menu);

        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                network.mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                network.mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                timer.cancel();
                return true;
            case R.id.menu_refresh:
                network.sendtomoduleCMD();
                return true;
            case R.id.version:
                Toast.makeText(this, "version", Toast.LENGTH_SHORT).show();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mtv_ModName != null) {
                    mtv_ModName.setText(resourceId);
                }
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter
                .addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    @Override
    public void onClick(View v) {
        if(v == mButtonEDFA1){
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            network.setModulename(MOD_EDFA1);
        }else if(v == mButtonEDFA2){
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            network.setModulename(MOD_EDFA2);
        }else if(v == mButtonONU){
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            network.setModulename(MOD_ONU);

        }else if(v == mButtonFTX){
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            network.setModulename(MOD_OTX);

        }else if(v == mButtonREPEAT1){
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            network.setModulename(MOD_REPEAT1);

        }else if(v == mButtonRMC){
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            network.setModulename(MOD_RMC);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // parent는 AdapterView의 속성의 모두 사용 할 수 있다.
        String tv = (String)parent.getAdapter().getItem(position);
        Toast.makeText(getApplicationContext(), tv, Toast.LENGTH_SHORT).show();


    }
}
