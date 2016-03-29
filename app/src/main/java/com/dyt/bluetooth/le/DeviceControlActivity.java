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
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity{
    private final static String TAG = BluetoothLeService.class.getName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public static final byte CMD_MOD_VALUE_REQ = (byte) 0x91;
    public static final byte CMD_MOD_VALUE_RES = (byte) 0x11;

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

    public static final byte FIX__ = 0;
    public static final byte STAT_ = 64;

    public static final int STAT_ALL_BLOCK = 0x0FFFF;
    public static final int STAT_FIX_BLOCK1 = 0x001;
    public static final int STAT_FIX_BLOCK2 = 0x002;
    public static final int STAT_FIX_BLOCK3 = 0x004;
    public static final int STAT_FIX_BLOCK4 = 0x008;
    public static final int STAT_FIX_BLOCK5 = 0x010;
    public static final int STAT_FIX_BLOCK6 = 0x020;
    public static final int STAT_FIX_BLOCK7 = 0x040;
    public static final int STAT_FIX_BLOCK8 = 0x080;

    public static final int STAT_VAL_BLOCK1 = 0x0100;
    public static final int STAT_VAL_BLOCK2 = 0x0200;
    public static final int STAT_VAL_BLOCK3 = 0x0400;
    public static final int STAT_VAL_BLOCK4 = 0x0800;
    public static final int STAT_VAL_BLOCK5 = 0x1000;
    public static final int STAT_VAL_BLOCK6 = 0x2000;
    public static final int STAT_VAL_BLOCK7 = 0x4000;
    public static final int STAT_VAL_BLOCK8 = 0x8000;

    public static final int UPTIME_ENABLE = 0x0000;
    public static final int UPTIME_DISABLE = 0x00FF;
    private int mod_Block_stat;
    private int asciitobin;
    private int uptimedisplay;
    private byte[] Value_Module_all = new byte[128];
    private byte[] Value_fixed = new byte[64];
    private byte[] Value_status = new byte[64];

    private TextView mConnectionState;
    private TextView mDataFieldRx;
    private EditText mDataFieldTx;
    // [ims] Tx 입력 지움 private Button mDataSend; //Send button은 삭제함.

    private TextView mtv_ModName;

    private TextView mdevice_addr;

    private Button mButtonEDFA1;
    private Button mButtonEDFA2;
    private Button mButtonONU;
    private Button mButtonFTX;
    private Button mButtonREPEAT1;
    private Button mButtonRMC;
    private Button btn_scroll;
    private Button mButtonRefresh;
    private int value = 0;
    private TextView textView;
    private CountDownTimer timer;
    private ScrollView mScrollDbgmsg;
    int scrollPosX, scrollPosY;
    private String mDeviceName;
    private String mDeviceAddress;

    private int mModName;
    LinearLayout topLL;
    private Handler handler = new Handler();


    // private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
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
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                // displayGattServices(mBluetoothLeService.getSupportedGattServices());
                // jw : 데이터 수신 Enable
                setDataNotify();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                // jw : 데이터 수신
                dataRecv(intent
                        .getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void clearUI() {
        // mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        //mDataFieldRx.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_activity);


        topLL = (LinearLayout)findViewById(R.id.dynamicArea);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mScrollDbgmsg = (ScrollView)findViewById(R.id.dbgMsg_scroll);


        //타이머 설정
        timer = new CountDownTimer(Integer.MAX_VALUE, 5000) {

            @Override
            public void onTick(long millisUntilFinished) {
                value++;
                //scrollPosX = mScrollDbgmsg.getScrollX();
                scrollPosY = mScrollDbgmsg.getScrollY();
                sendtomoduleCMD();

                mScrollDbgmsg.scrollTo(0, scrollPosY);
                //Toast.makeText(DeviceControlActivity.this, "sendtomoduleCMD" + value, Toast.LENGTH_SHORT).show();

                //if (value == 5) .cancel();
            }

            @Override
            public void onFinish() {
            }
        };

        //타이머 시작
        timer.start();

        //mDataFieldRx = (TextView) findViewById(R.id.data_value_rx);

        mtv_ModName = (TextView) findViewById(R.id.data_rx);

        mdevice_addr = (TextView) findViewById(R.id.device_addr);

        mdevice_addr.setText("  " + mDeviceAddress);

        mButtonEDFA1 = (Button) findViewById(R.id.button1EDFA1);
        mButtonEDFA2 = (Button) findViewById(R.id.button2EDFA2);
        mButtonONU = (Button) findViewById(R.id.button3ONU);
        mButtonFTX = (Button) findViewById(R.id.button4FTX);
        mButtonREPEAT1 = (Button) findViewById(R.id.button5REPEAT1);
        mButtonRMC = (Button) findViewById(R.id.button6RMC);
        btn_scroll = (Button)findViewById(R.id.btn_scroll);


        // [ims]지움 mDataSend.setOnClickListener(mDataSendClickListener);
        mButtonEDFA1.setOnClickListener(mButtonEDFA1ClickListener);
        mButtonEDFA2.setOnClickListener(mButtonEDFA2ClickListener);
        mButtonONU.setOnClickListener(mButtonONUClickListener);
        mButtonFTX.setOnClickListener(mButtonFTXClickListener);
        mButtonREPEAT1.setOnClickListener(mButtonREPEAT1ClickListener);
        mButtonRMC.setOnClickListener(mButtonRMCClickListener);
        btn_scroll.setOnClickListener(btnClickListener);


        ActionBar actionBar = getActionBar();
        getActionBar().setDisplayHomeAsUpEnabled(true);
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getActionBar().setTitle(mDeviceName);


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
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
        mBluetoothLeService = null;
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
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                timer.cancel();
                return true;
            case R.id.menu_refresh:
                sendtomoduleCMD();
                value++;
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

    // jw:여기서 송수신 Enable 처리
    private void setDataNotify() {
        List<BluetoothGattService> gattServices = mBluetoothLeService
                .getSupportedGattServices();
        UUID dataUuid = SampleGattAttributes.DATA_RX_UUID;

        for (BluetoothGattService gattService : gattServices) {
            BluetoothGattCharacteristic charac = gattService
                    .getCharacteristic(dataUuid);
            if (charac != null) {
                Log.d(TAG, ">> Data Rx/Tx Enable.");
                mBluetoothLeService.setCharacteristicNotification(charac, true);
            }
        }
    }

    private byte SendChecksum(byte[] data) {
        int chksum = 0;
        byte len = data[1];

        for (int i = 1; i <= (len); i++) {
            chksum += data[i];
        }
        chksum = ~chksum;
        return (byte) chksum;
    }

    private byte CalChecksum(byte[] data) {
        int chksum = 0;

        for (int i = 1; i <= 18; i++) {
            chksum += data[i];
        }
        chksum = ~chksum;
        if (chksum == 0x0d) {
            chksum = 0xFF;
        }
        return (byte) chksum;
    }

    // Recive 20Byte data decoding.....
    private boolean moduledata_RecieveCheck(byte[] data) {
        int ptr;
        int indextmp;
        int datatmp;
        byte tmpchksum;

        if (data[0] != 0x11) return false;

        indextmp = data[2] - 0x30;

        mod_Block_stat += (STAT_FIX_BLOCK1 << indextmp);
        ptr = 8 * indextmp;


        // checksum cal....
        tmpchksum = CalChecksum(data);
        if (tmpchksum != data[19]) return false;

        for (int i = 0; i < 16; i++) {
            if ((data[3 + i] >= '0') && (data[3 + i] <= '9')) {
                datatmp = (data[3 + i] - '0');
            } else if ((data[3 + i] >= 'A') && (data[3 + i] <= 'F')) {
                datatmp = (data[3 + i] - 'A') + 10;
            } else {
                datatmp = 0;
            }

            if ((i % 2) == 0) {
                asciitobin = (datatmp << 4) & 0xF0;
            } else {
                asciitobin |= datatmp & 0x0F;
                Value_Module_all[ptr++] = (byte) asciitobin;
            }
        }

        if (mod_Block_stat == STAT_ALL_BLOCK) {
            mod_Block_stat = 0;
            return true;
        } else {
            return false;
        }
    }

    private void modDisplay_EDFA1(byte[] data) {
        int tmp =1;
        int tmp2;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            mtv_ModName.setText("EDFA1 Module");
            return;
            //stringBuilder.append(String.format("Receiving Data...\n"));
        } else {
            stringBuilder.append(String.format("\n< Alarm List >\n"));
            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String.format("Optical Input\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String.format("Optical Output\n"));
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String.format("LD1 Temperature\n"));
            }
            if ((tmp & (0x0001 << 4)) == (0x0001 << 4)) {
                stringBuilder.append(String.format("LD2 Temperature\n"));
            }
            if ((tmp & (0x0001 << 6)) == (0x0001 << 6)) {
                stringBuilder.append(String.format("Board Temperature\n"));
            }
            stringBuilder.append(String.format("\n< Status >\n"));

            tmp = Value_Module_all[STAT_ + 13] * 256;
            tmp |= (Value_Module_all[STAT_ + 12] & 0xFF);
            stringBuilder.append(String.format(
                    "Optical Input Power   :: %.1f dBm\n", (float) tmp / 10));
            if (Value_Module_all[STAT_ + 7] == 0x31) {
                stringBuilder.append(String
                        .format("Laser Key Switch Status   :: ON\n"));
            } else {
                stringBuilder.append(String
                        .format("Laser Key Switch Status   :: OFF\n"));
            }
            if (Value_Module_all[STAT_ + 8] == 0x31) {
                stringBuilder.append(String.format("Laser  Switch   :: ON\n"));
            } else {
                stringBuilder.append(String.format("Laser  Switch   :: OFF\n"));
            }

            stringBuilder.append(String.format("LD Status   :: LD ON\n"));
            tmp = Value_Module_all[STAT_ + 15] * 256;
            tmp |= (Value_Module_all[STAT_ + 14] & 0xFF);
            stringBuilder.append(String.format(
                    "Optical Output Power   :: %.1f dBm\n", (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 19] * 256;
            tmp |= (Value_Module_all[STAT_ + 18] & 0xFF);

            tmp2 = Value_Module_all[STAT_ + 25] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 24] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Bias Current   :: %.1f mA, %.1f mA\n",
                    (float) (tmp / 10), (float) (tmp2 / 10)));

            tmp = Value_Module_all[STAT_ + 17] * 256;
            tmp |= (Value_Module_all[STAT_ + 16] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 23] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 22] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Temperature   :: %.1f `C, %.1f `C\n", (float) tmp / 10,
                    (float) tmp2 / 10));

            tmp = Value_Module_all[STAT_ + 11] * 256;
            tmp |= (Value_Module_all[STAT_ + 10] & 0xFF);
            stringBuilder.append(String.format(
                    "Module Temperature   :: %.1f `C\n", (float) tmp / 10));

            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String
                        .format("\nInput Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("\nInput Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String
                        .format("Output Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("Output Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String
                        .format("LD Temper Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("LD Temper Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 5)) == (0x0001 << 5)) {
                stringBuilder.append(String
                        .format("B'd Temper Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("B'd Temper Alarm   :: Disable\n"));
            }

            tmp = Value_Module_all[STAT_ + 29] * 256;
            tmp |= (Value_Module_all[STAT_ + 28] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 31] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 30] & 0xFF);
            stringBuilder
                    .append(String
                            .format("\nInput Power Alarm Threshold MIN/MAX   :: %.1f dBm / %.1f dBm\n",
                                    (float) tmp2 / 10, (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 33] * 256;
            tmp |= (Value_Module_all[STAT_ + 32] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 35] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 34] & 0xFF);
            stringBuilder
                    .append(String
                            .format("Output Power Alarm Threshold MIN/MAX   :: %.1f dBm / %.1f dBm\n",
                                    (float) tmp2 / 10, (float) tmp / 10));
            stringBuilder.append(String.format("\n< Reference value >\n"));

            tmp = Value_Module_all[FIX__ + 33] * 256;
            tmp |= (Value_Module_all[FIX__ + 32] & 0xFF);
            stringBuilder
                    .append(String.format(
                            "Output Power Reference   :: %.1f dBm\n",
                            (float) tmp / 10));

            tmp = Value_Module_all[FIX__ + 37] * 256;
            tmp |= (Value_Module_all[FIX__ + 36] & 0xFF);
            tmp2 = Value_Module_all[FIX__ + 43] * 256;
            tmp2 |= (Value_Module_all[42] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Bias Current Reference   :: %.1f mA, %.1f mA\n",
                    (float) tmp / 10, (float) tmp2 / 10));

            tmp = Value_Module_all[FIX__ + 39] * 256;
            tmp |= (Value_Module_all[FIX__ + 38] & 0xFF);
            tmp2 = Value_Module_all[FIX__ + 45] * 256;
            tmp2 |= (Value_Module_all[FIX__ + 44] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Temperature  Reference   :: %.1f `C, %.1f `C\n",
                    (float) tmp / 10, (float) tmp2 / 10));

            stringBuilder.append(String.format("\n<Module Information>\n"));

            tmp = (Value_Module_all[FIX__ + 5] & 0xFF);
            stringBuilder.append(String.format("SW Version       :: %.1f\n",
                    (float) tmp / 10));
            // Serial 번호 표시
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, (FIX__ + 6), tmpStr, 0, 8);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Serial Number   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("HW Version       :: %d", tmp));
            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            // Model 명 표시
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, (FIX__ + 17), tmpStr, 0, 15);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Model Name     :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 39] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 38] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 37] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 36] & 0xFF));
                stringBuilder.append(String.format("Uptime :: [%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
            }

        }
        //mDataFieldRx.setText(stringBuilder.toString());


    }

    private void modDisplay_EDFA2(byte[] data) {
        int tmp;
        int tmp2;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            mtv_ModName.setText("EDFA2 Module");
            //stringBuilder.append(String.format("Receiving Data...\n"));
            return;
        } else {
            stringBuilder.append(String.format("\n< Alarm List >\n"));
            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String.format("Optical Input\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String.format("Optical Output\n"));
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String.format("LD1 Temperature\n"));
            }
            if ((tmp & (0x0001 << 4)) == (0x0001 << 4)) {
                stringBuilder.append(String.format("LD2 Temperature\n"));
            }
            if ((tmp & (0x0001 << 6)) == (0x0001 << 6)) {
                stringBuilder.append(String.format("Board Temperature\n"));
            }
            stringBuilder.append(String.format("\n< Status >\n"));

            tmp = Value_Module_all[STAT_ + 13] * 256;
            tmp |= (Value_Module_all[STAT_ + 12] & 0xFF);
            stringBuilder.append(String.format(
                    "Optical Input Power   :: %.1f dBm\n", (float) tmp / 10));
            if (Value_Module_all[STAT_ + 7] == 0x31) {
                stringBuilder.append(String
                        .format("Laser Key Switch Status   :: ON\n"));
            } else {
                stringBuilder.append(String
                        .format("Laser Key Switch Status   :: OFF\n"));
            }
            if (Value_Module_all[STAT_ + 8] == 0x31) {
                stringBuilder.append(String.format("Laser  Switch   :: ON\n"));
            } else {
                stringBuilder.append(String.format("Laser  Switch   :: OFF\n"));
            }

            stringBuilder.append(String.format("LD Status   :: LD ON\n"));
            tmp = Value_Module_all[STAT_ + 15] * 256;
            tmp |= (Value_Module_all[STAT_ + 14] & 0xFF);
            stringBuilder.append(String.format(
                    "Optical Output Power   :: %.1f dBm\n", (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 19] * 256;
            tmp |= (Value_Module_all[STAT_ + 18] & 0xFF);

            tmp2 = Value_Module_all[STAT_ + 25] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 24] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Bias Current   :: %.1f mA, %.1f mA\n",
                    (float) (tmp / 10), (float) (tmp2 / 10)));

            tmp = Value_Module_all[STAT_ + 17] * 256;
            tmp |= (Value_Module_all[STAT_ + 16] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 23] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 22] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Temperature   :: %.1f `C, %.1f `C\n", (float) tmp / 10,
                    (float) tmp2 / 10));

            tmp = Value_Module_all[STAT_ + 11] * 256;
            tmp |= (Value_Module_all[STAT_ + 10] & 0xFF);
            stringBuilder.append(String.format(
                    "Module Temperature   :: %.1f `C\n", (float) tmp / 10));

            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String
                        .format("\nInput Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("\nInput Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String
                        .format("Output Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("Output Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String
                        .format("LD Temper Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("LD Temper Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 5)) == (0x0001 << 5)) {
                stringBuilder.append(String
                        .format("B'd Temper Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("B'd Temper Alarm   :: Disable\n"));
            }

            tmp = Value_Module_all[STAT_ + 29] * 256;
            tmp |= (Value_Module_all[STAT_ + 28] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 31] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 30] & 0xFF);
            stringBuilder
                    .append(String
                            .format("\nInput Power Alarm Threshold MIN/MAX   :: %.1f dBm / %.1f dBm\n",
                                    (float) tmp2 / 10, (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 33] * 256;
            tmp |= (Value_Module_all[STAT_ + 32] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 35] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 34] & 0xFF);
            stringBuilder
                    .append(String
                            .format("Output Power Alarm Threshold MIN/MAX   :: %.1f dBm / %.1f dBm\n",
                                    (float) tmp2 / 10, (float) tmp / 10));
            stringBuilder.append(String.format("\n< Reference value >\n"));

            tmp = Value_Module_all[FIX__ + 33] * 256;
            tmp |= (Value_Module_all[FIX__ + 32] & 0xFF);
            stringBuilder
                    .append(String.format(
                            "Output Power Reference   :: %.1f dBm\n",
                            (float) tmp / 10));

            tmp = Value_Module_all[FIX__ + 37] * 256;
            tmp |= (Value_Module_all[FIX__ + 36] & 0xFF);
            tmp2 = Value_Module_all[FIX__ + 43] * 256;
            tmp2 |= (Value_Module_all[42] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Bias Current Reference   :: %.1f mA, %.1f mA\n",
                    (float) tmp / 10, (float) tmp2 / 10));

            tmp = Value_Module_all[FIX__ + 39] * 256;
            tmp |= (Value_Module_all[FIX__ + 38] & 0xFF);
            tmp2 = Value_Module_all[FIX__ + 45] * 256;
            tmp2 |= (Value_Module_all[FIX__ + 44] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Temperature  Reference   :: %.1f `C, %.1f `C\n",
                    (float) tmp / 10, (float) tmp2 / 10));

            stringBuilder.append(String.format("\n<Module Information>\n"));

            tmp = (Value_Module_all[FIX__ + 5] & 0xFF);
            stringBuilder.append(String.format("SW Version       :: %.1f\n",
                    (float) tmp / 10));
            // Serial 번호 표시
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, (FIX__ + 6), tmpStr, 0, 8);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Serial Number   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("HW Version       :: %d", tmp));
            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            // Model 명 표시
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, (FIX__ + 17), tmpStr, 0, 15);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Model Name   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 39] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 38] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 37] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 36] & 0xFF));
                stringBuilder.append(String.format("Uptime :: [%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
            }

        }
        mDataFieldRx.setText(stringBuilder.toString());
    }

    private void modDisplay_EDFA3(byte[] data) {
        int tmp;
        int tmp2;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            mtv_ModName.setText("EDFA3 Module");
            return;
            //stringBuilder.append(String.format("Receiving Data...\n"));
        } else {
            stringBuilder.append(String.format("\n< Alarm List >\n"));
            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String.format("Optical Input\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String.format("Optical Output\n"));
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String.format("LD1 Temperature\n"));
            }
            if ((tmp & (0x0001 << 4)) == (0x0001 << 4)) {
                stringBuilder.append(String.format("LD2 Temperature\n"));
            }
            if ((tmp & (0x0001 << 6)) == (0x0001 << 6)) {
                stringBuilder.append(String.format("Board Temperature\n"));
            }
            stringBuilder.append(String.format("\n< Status >\n"));

            tmp = Value_Module_all[STAT_ + 13] * 256;
            tmp |= (Value_Module_all[STAT_ + 12] & 0xFF);
            stringBuilder.append(String.format(
                    "Optical Input Power   :: %.1f dBm\n", (float) tmp / 10));
            if (Value_Module_all[STAT_ + 7] == 0x31) {
                stringBuilder.append(String
                        .format("Laser Key Switch Status   :: ON\n"));
            } else {
                stringBuilder.append(String
                        .format("Laser Key Switch Status   :: OFF\n"));
            }
            if (Value_Module_all[STAT_ + 8] == 0x31) {
                stringBuilder.append(String.format("Laser  Switch   :: ON\n"));
            } else {
                stringBuilder.append(String.format("Laser  Switch   :: OFF\n"));
            }

            stringBuilder.append(String.format("LD Status   :: LD ON\n"));
            tmp = Value_Module_all[STAT_ + 15] * 256;
            tmp |= (Value_Module_all[STAT_ + 14] & 0xFF);
            stringBuilder.append(String.format(
                    "Optical Output Power   :: %.1f dBm\n", (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 19] * 256;
            tmp |= (Value_Module_all[STAT_ + 18] & 0xFF);

            tmp2 = Value_Module_all[STAT_ + 25] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 24] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Bias Current   :: %.1f mA, %.1f mA\n",
                    (float) (tmp / 10), (float) (tmp2 / 10)));

            tmp = Value_Module_all[STAT_ + 17] * 256;
            tmp |= (Value_Module_all[STAT_ + 16] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 23] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 22] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Temperature   :: %.1f `C, %.1f `C\n", (float) tmp / 10,
                    (float) tmp2 / 10));

            tmp = Value_Module_all[STAT_ + 11] * 256;
            tmp |= (Value_Module_all[STAT_ + 10] & 0xFF);
            stringBuilder.append(String.format(
                    "Module Temperature   :: %.1f `C\n", (float) tmp / 10));

            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String
                        .format("\nInput Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("\nInput Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String
                        .format("Output Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("Output Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String
                        .format("LD Temper Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("LD Temper Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 5)) == (0x0001 << 5)) {
                stringBuilder.append(String
                        .format("B'd Temper Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("B'd Temper Alarm   :: Disable\n"));
            }

            tmp = Value_Module_all[STAT_ + 29] * 256;
            tmp |= (Value_Module_all[STAT_ + 28] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 31] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 30] & 0xFF);
            stringBuilder
                    .append(String
                            .format("\nInput Power Alarm Threshold MIN/MAX   :: %.1f dBm / %.1f dBm\n",
                                    (float) tmp2 / 10, (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 33] * 256;
            tmp |= (Value_Module_all[STAT_ + 32] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 35] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 34] & 0xFF);
            stringBuilder
                    .append(String
                            .format("Output Power Alarm Threshold MIN/MAX   :: %.1f dBm / %.1f dBm\n",
                                    (float) tmp2 / 10, (float) tmp / 10));
            stringBuilder.append(String.format("\n< Reference value >\n"));

            tmp = Value_Module_all[FIX__ + 33] * 256;
            tmp |= (Value_Module_all[FIX__ + 32] & 0xFF);
            stringBuilder
                    .append(String.format(
                            "Output Power Reference   :: %.1f dBm\n",
                            (float) tmp / 10));

            tmp = Value_Module_all[FIX__ + 37] * 256;
            tmp |= (Value_Module_all[FIX__ + 36] & 0xFF);
            tmp2 = Value_Module_all[FIX__ + 43] * 256;
            tmp2 |= (Value_Module_all[42] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Bias Current Reference   :: %.1f mA, %.1f mA\n",
                    (float) tmp / 10, (float) tmp2 / 10));

            tmp = Value_Module_all[FIX__ + 39] * 256;
            tmp |= (Value_Module_all[FIX__ + 38] & 0xFF);
            tmp2 = Value_Module_all[FIX__ + 45] * 256;
            tmp2 |= (Value_Module_all[FIX__ + 44] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Temperature  Reference   :: %.1f `C, %.1f `C\n",
                    (float) tmp / 10, (float) tmp2 / 10));

            stringBuilder.append(String.format("\n<Module Information>\n"));

            tmp = (Value_Module_all[FIX__ + 5] & 0xFF);
            stringBuilder.append(String.format("SW Version       :: %.1f\n",
                    (float) tmp / 10));
            // Serial 번호 표시
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, (FIX__ + 6), tmpStr, 0, 8);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Serial Number   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("HW Version       :: %d", tmp));
            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            // Model 명 표시
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, (FIX__ + 17), tmpStr, 0, 15);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Model Name   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 39] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 38] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 37] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 36] & 0xFF));
                stringBuilder.append(String.format("Uptime :: [%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
            }

        }
        mDataFieldRx.setText(stringBuilder.toString());

    }

    private void modDisplay_ONU(byte[] data) {
        int tmp;
        int tmp2;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            mtv_ModName.setText("ONU Module");
            return;
            //stringBuilder.append(String.format("Receiving Data...\n"));
        } else {
            stringBuilder.append(String.format("\n< Alarm List>\n"));
            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String.format("Input Power\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String.format("Output Power\n"));
            }
            stringBuilder.append(String.format("\n< Status >\n"));

            tmp = Value_Module_all[STAT_ + 8] * 256;
            tmp |= (Value_Module_all[STAT_ + 7] & 0xFF);
            stringBuilder.append(String.format("Input Power   :: %.1f dBm\n",
                    (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 10] * 256;
            tmp |= (Value_Module_all[STAT_ + 9] & 0xFF);
            stringBuilder.append(String.format("Output Power   :: %.1f dBm\n",
                    (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 16] * 256;
            tmp |= (Value_Module_all[STAT_ + 15] & 0xFF);
            stringBuilder.append(String.format(
                    "Module Temperature   :: %.1f `C\n", (float) tmp / 10));

            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String
                        .format("\nInput Power Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("\nInput Power Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String
                        .format("Output Power Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("Output Power Alarm   :: Enable\n"));
            }

            stringBuilder.append(String.format("\n<Module Information>\n"));
            tmp = (Value_Module_all[5] & 0xFF);
            stringBuilder.append(String.format("SW Version       :: %.1f\n",
                    (float) tmp / 10));
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, (FIX__ + 6), tmpStr, 0, 8);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Serial Number   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("HW Version       :: %d", tmp));
            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, (FIX__ + 17), tmpStr, 0, 15);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Model Name   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 14] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 13] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 12] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 11] & 0xFF));
                stringBuilder.append(String.format("Uptime :: [%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
            }
        }
        mDataFieldRx.setText(stringBuilder.toString());
    }

    private void modDisplay_OTX(byte[] data) {
        int tmp;
        int tmp2;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            mtv_ModName.setText("FTX Module");
            return;
            //stringBuilder.append(String.format("Receiving Data...\n"));
        } else {
            stringBuilder.append(String.format("< Alarm List>\n"));
            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String.format("Opt Output Power\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String.format("LASER Temperature\n"));
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String.format("RF Input Power\n"));
            }

            stringBuilder.append(String.format("\n< Status >\n"));

            if (Value_Module_all[STAT_ + 8] == 0x31) {
                stringBuilder
                        .append(String.format("RF Mode Status   :: MGC\n"));
            } else {
                stringBuilder
                        .append(String.format("RF Mode Status   :: AGC\n"));
            }
            if (Value_Module_all[STAT_ + 9] == 0x31) {
                stringBuilder.append(String.format("RF Type   :: CW\n"));
            } else {
                stringBuilder.append(String.format("RF Type   :: VIDEO\n"));
            }

            if (Value_Module_all[STAT_ + 11] == 0x31) {
                stringBuilder.append(String.format("RF Status   :: RF Low\n"));
            } else if (Value_Module_all[STAT_ + 11] == 0x32) {
                stringBuilder.append(String.format("RF Status   :: RF High\n"));
            } else {
                stringBuilder.append(String
                        .format("RF Status   :: RF Normal\n"));
            }

            tmp |= (Value_Module_all[STAT_ + 10] & 0xFF);
            stringBuilder.append(String.format(
                    "Input Attenuation   :: %.1f dB\n", (float) tmp / 10));

            if (Value_Module_all[STAT_ + 7] == 0x31) {
                stringBuilder.append(String.format("LD Key Status   :: ON\n"));
            } else {
                stringBuilder.append(String.format("LD Key Status   :: OFF\n"));
            }

            if (Value_Module_all[STAT_ + 12] == 0x31) {
                stringBuilder.append(String.format("Laser Switch   :: ON\n"));
            } else {
                stringBuilder.append(String.format("Laser Switch   :: OFF\n"));
            }
            tmp = Value_Module_all[STAT_ + 14] * 256;
            tmp |= (Value_Module_all[STAT_ + 13] & 0xFF);
            stringBuilder.append(String.format("LD Power (mW)   :: %.1f mW\n",
                    (float) tmp / 10));
            tmp = Value_Module_all[STAT_ + 16] * 256;
            tmp |= (Value_Module_all[STAT_ + 15] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Power (dBm)   :: %.1f dBm\n", (float) tmp / 10));
            tmp = Value_Module_all[STAT_ + 18] * 256;
            tmp |= (Value_Module_all[STAT_ + 17] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Bias Current   :: %.1f mA\n", (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 20] * 256;
            tmp |= (Value_Module_all[STAT_ + 19] & 0xFF);
            stringBuilder.append(String.format(
                    "LD Temperature   :: %.1f `C\n", (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 22] * 256;
            tmp |= (Value_Module_all[STAT_ + 21] & 0xFF);
            stringBuilder.append(String.format("OMI Status   :: %.1f dB\n",
                    (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 24] * 256;
            tmp |= (Value_Module_all[STAT_ + 23] & 0xFF);
            stringBuilder.append(String.format(
                    "OMI Reference Level   :: %.1f dB\n", (float) tmp / 10));

            tmp = Value_Module_all[STAT_ + 26] * 256;
            tmp |= (Value_Module_all[STAT_ + 25] & 0xFF);
            stringBuilder.append(String.format(
                    "Module Temperature   :: %.1f `C\n", (float) tmp / 10));

            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String
                        .format("\nOutput Power Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("\nOutput Power Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String
                        .format("LD Temperature Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("LD Temperature Alarm   :: Disable\n"));
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String
                        .format("RF Input Power Alarm   :: Enable\n"));
            } else {
                stringBuilder.append(String
                        .format("RF Input Power Alarm   :: Disable\n"));
            }

            stringBuilder.append(String.format("\n<Module Information>\n"));
            tmp = (Value_Module_all[5] & 0xFF);
            stringBuilder.append(String.format("SW Version       :: %.1f\n",
                    (float) tmp / 10));
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, 6, tmpStr, 0, 8);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Serial Number   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("HW Version       :: %d", tmp));
            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, 17, tmpStr, 0, 15);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Model Name   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 31] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 30] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 29] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 28] & 0xFF));
                stringBuilder.append(String.format("Uptime :: [%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
            }
        }
        mDataFieldRx.setText(stringBuilder.toString());
    }

    private void modDisplay_REPEATER1(byte[] data) {
        int tmp;
        int tmp2;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            mtv_ModName.setText("Repeater1 Module");
            return;
            //stringBuilder.append(String.format("Receiving Data...\n"));
        } else {
            stringBuilder.append(String.format("< Alarm List >\n"));

            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String
                        .format("ORX1 Alarm Status   :: Raised\n"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String
                        .format("ORX2 Alarm Status   :: Raised\n"));
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String
                        .format("ORX3 Alarm Status   :: Raised\n"));
            }
            if ((tmp & (0x0001 << 3)) == (0x0001 << 3)) {
                stringBuilder.append(String
                        .format("ORX4 Alarm Status   :: Raised\n"));
            }
            if ((tmp & (0x0001 << 4)) == (0x0001 << 4)) {
                stringBuilder.append(String
                        .format("ORX5 Alarm Status   :: Raised\n"));
            }
            if ((tmp & (0x0001 << 5)) == (0x0001 << 5)) {
                stringBuilder.append(String
                        .format("ORX6 Alarm Status   :: Raised\n"));
            }
            if ((tmp & (0x0001 << 6)) == (0x0001 << 6)) {
                stringBuilder.append(String
                        .format("ORX7 Alarm Status   :: Raised\n"));
            }
            if ((tmp & (0x0001 << 7)) == (0x0001 << 7)) {
                stringBuilder.append(String
                        .format("ORX8 Alarm Status   :: Raised\n"));
            }
            if ((tmp & (0x0001 << 8)) == (0x0001 << 8)) {
                stringBuilder.append(String
                        .format("OTX Alarm Status   :: Raised\n"));
            }

            stringBuilder.append(String.format("\n< Status >\n"));

            tmp = Value_Module_all[STAT_ + 9] * 256;
            tmp |= (Value_Module_all[STAT_ + 8] & 0xFF);
            stringBuilder.append(String.format("OTx Out Power   :: %.1f dBm\n",
                    (float) tmp / 10));
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 0)) == (0x0001 << 0)) {
                tmp = Value_Module_all[STAT_ + 11] * 256;
                tmp |= (Value_Module_all[STAT_ + 10] & 0xFF);
                stringBuilder.append(String.format(
                        "ORx1 Rx Power   :: %.1f dBm\n", (float) tmp / 10));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 1)) == (0x0001 << 1)) {
                tmp = Value_Module_all[STAT_ + 13] * 256;
                tmp |= (Value_Module_all[STAT_ + 12] & 0xFF);
                stringBuilder.append(String.format(
                        "ORx2 Rx Power   :: %.1f dBm\n", (float) tmp / 10));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 2)) == (0x0001 << 2)) {
                tmp = Value_Module_all[STAT_ + 15] * 256;
                tmp |= (Value_Module_all[STAT_ + 14] & 0xFF);
                stringBuilder.append(String.format(
                        "ORx3 Rx Power   :: %.1f dBm\n", (float) tmp / 10));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 3)) == (0x0001 << 3)) {
                tmp = Value_Module_all[STAT_ + 17] * 256;
                tmp |= (Value_Module_all[STAT_ + 16] & 0xFF);
                stringBuilder.append(String.format(
                        "ORx4 Rx Power   :: %.1f dBm\n", (float) tmp / 10));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 4)) == (0x0001 << 4)) {
                tmp = Value_Module_all[STAT_ + 19] * 256;
                tmp |= (Value_Module_all[STAT_ + 18] & 0xFF);
                stringBuilder.append(String.format(
                        "ORx5 Rx Power   :: %.1f dBm\n", (float) tmp / 10));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 5)) == (0x0001 << 5)) {
                tmp = Value_Module_all[STAT_ + 21] * 256;
                tmp |= (Value_Module_all[STAT_ + 20] & 0xFF);
                stringBuilder.append(String.format(
                        "ORx6 Rx Power   :: %.1f dBm\n", (float) tmp / 10));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 6)) == (0x0001 << 6)) {
                tmp = Value_Module_all[STAT_ + 23] * 256;
                tmp |= (Value_Module_all[STAT_ + 22] & 0xFF);
                stringBuilder.append(String.format(
                        "ORx7 Rx Power   :: %.1f dBm\n", (float) tmp / 10));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 7)) == (0x0001 << 7)) {
                tmp = Value_Module_all[STAT_ + 25] * 256;
                tmp |= (Value_Module_all[STAT_ + 24] & 0xFF);
                stringBuilder.append(String.format(
                        "ORx8 Rx Power   :: %.1f dBm\n", (float) tmp / 10));
            }

            tmp = Value_Module_all[STAT_ + 27] * 256;
            tmp |= (Value_Module_all[STAT_ + 26] & 0xFF);
            stringBuilder.append(String.format(
                    "Module Temperature   :: %.1f `C\n", (float) tmp / 10));

            stringBuilder.append(String.format("\n< Install ORx Module >\n"));

            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String.format("ORx1  "));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String.format("ORx2  "));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String.format("ORx3  "));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 3)) == (0x0001 << 3)) {
                stringBuilder.append(String.format("ORx4"));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x000f)) != (0x0000)) {
                stringBuilder.append(String.format("\n"));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 4)) == (0x0001 << 4)) {
                stringBuilder.append(String.format("ORx5  "));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 5)) == (0x0001 << 5)) {
                stringBuilder.append(String.format("ORx6  "));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 6)) == (0x0001 << 6)) {
                stringBuilder.append(String.format("ORx7  "));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 7)) == (0x0001 << 7)) {
                stringBuilder.append(String.format("ORx8"));
            }
            if ((Value_Module_all[STAT_ + 7] & (0x00F0)) != (0x0000)) {
                stringBuilder.append(String.format("\n"));
            }

            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 8)) == (0x0001 << 8)) {
                stringBuilder
                        .append(String.format("\nOTX Alarm   :: Enable\n"));
            } else {
                stringBuilder
                        .append(String.format("\nOTX Alarm   :: Enable\n"));
            }
            stringBuilder.append(String.format("ORX Alarm\n"));
            stringBuilder.append(String.format("  Eable   ::"));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                stringBuilder.append(String.format(" ORx1"));
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                stringBuilder.append(String.format(" ORx2"));
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                stringBuilder.append(String.format(" ORx3"));
            }
            if ((tmp & (0x0001 << 3)) == (0x0001 << 3)) {
                stringBuilder.append(String.format(" ORx4"));
            }
            if ((tmp & (0x0001 << 4)) == (0x0001 << 4)) {
                stringBuilder.append(String.format(" ORx5"));
            }
            if ((tmp & (0x0001 << 5)) == (0x0001 << 5)) {
                stringBuilder.append(String.format(" ORx6"));
            }
            if ((tmp & (0x0001 << 6)) == (0x0001 << 6)) {
                stringBuilder.append(String.format(" ORx7"));
            }
            if ((tmp & (0x0001 << 7)) == (0x0001 << 7)) {
                stringBuilder.append(String.format(" ORx8"));
            }
            if ((tmp & (0x00FF)) == 0) {
                stringBuilder.append(String.format("None"));
            }
            stringBuilder.append(String.format("\n  Disable ::"));
            if ((tmp & (0x0001 << 0)) == 0) {
                stringBuilder.append(String.format(" ORx1"));
            }
            if ((tmp & (0x0001 << 1)) == 0) {
                stringBuilder.append(String.format(" ORx2"));
            }
            if ((tmp & (0x0001 << 2)) == 0) {
                stringBuilder.append(String.format(" ORx3"));
            }
            if ((tmp & (0x0001 << 3)) == 0) {
                stringBuilder.append(String.format(" ORx4"));
            }
            if ((tmp & (0x0001 << 4)) == 0) {
                stringBuilder.append(String.format(" ORx5"));
            }
            if ((tmp & (0x0001 << 5)) == 0) {
                stringBuilder.append(String.format(" ORx6"));
            }
            if ((tmp & (0x0001 << 6)) == 0) {
                stringBuilder.append(String.format(" ORx7"));
            }
            if ((tmp & (0x0001 << 7)) == 0) {
                stringBuilder.append(String.format(" ORx8"));
            }
            if ((tmp & (0x00FF)) == 0x00FF) {
                stringBuilder.append(String.format("None"));
            }
            stringBuilder.append(String.format("\n"));

            stringBuilder.append(String.format("\n<Module Information>\n"));
            tmp = (Value_Module_all[5] & 0xFF);
            stringBuilder.append(String.format("SW Version       :: %.1f\n",
                    (float) tmp / 10));
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, 6, tmpStr, 0, 8);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Serial Number   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("HW Version       :: %d", tmp));
            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, 17, tmpStr, 0, 15);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Model Name   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 31] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 30] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 29] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 28] & 0xFF));
                stringBuilder.append(String.format("Uptime :: [%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
            }
        }
        mDataFieldRx.setText(stringBuilder.toString());
    }

    private void modDisplay_AMP(byte[] data) {
        int tmp;
        int tmp2;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            mtv_ModName.setText("RF Amp Module");
            //stringBuilder.append(String.format("Receiving Data...\n"));
            return;
        }
        mDataFieldRx.setText(stringBuilder.toString());
    }

    private void modDisplay_RMC(byte[] data) {
        int tmp;
        int tmp2;
        topLL.removeAllViews();
        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            mtv_ModName.setText("RMC Module");
            return;
            //stringBuilder.append(String.format("Receiving Data...\n"));
        } else {
            stringBuilder.append(String.format("\n<Module Information>\n"));
            tmp = (Value_Module_all[5] & 0xFF);
            makeTitleView(stringBuilder.toString());
            stringBuilder.setLength(0);

            //stringBuilder.append(String.format("SW Version       ::"));
            stringBuilder.setLength(0);
            stringBuilder.append(String.format("%.1f\n",(float) tmp / 10));

            makeDetailView("SW Version", stringBuilder.toString());

            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, 6, tmpStr, 0, 8);
                String Str = new String(tmpStr);
                //stringBuilder.append(String.format("Serial Number   :: "));

                //stringBuilder.setLength(0);

                //stringBuilder.append(String.valueOf(Str));
                makeDetailView("Serial Number", ""+String.valueOf(Str) + "\n");
                stringBuilder.setLength(0);
                //stringBuilder.append(String.format("\n"));
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            //stringBuilder.append(String.format("HW Version       :: %d", tmp));
            stringBuilder.append(""+tmp);


            //stringBuilder.setLength(0);

                byte[] tmpStr1 = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr1, 0, 1);
                String Str = new String(tmpStr1);
                stringBuilder.append(String.valueOf(Str));


            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            //makeTextView(stringBuilder.toString(),true);
            makeDetailView("HW Version", stringBuilder.toString());
            stringBuilder.setLength(0);
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, 17, tmpStr, 0, 15);
                String Str1 = new String(tmpStr);
                //stringBuilder.append(String.format("Model Name   :: "));
                stringBuilder.append(String.valueOf(Str1));
                stringBuilder.append(String.format("\n"));
                makeDetailView("Model Name", stringBuilder.toString());
                //makeTextView(stringBuilder.toString(),true);
                stringBuilder.setLength(0);
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 40] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 39] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 38] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 37] & 0xFF));
                //stringBuilder.append(String.format("Uptime :: [%d]", tmp));
                //makeTextView(stringBuilder.toString(),true);
                stringBuilder.setLength(0);
                stringBuilder.append(String.format("[%d][%d:%d.%d]",tmp, (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
                //makeTextView(stringBuilder.toString(),true);
                makeDetailView("Uptime", stringBuilder.toString());
                stringBuilder.setLength(0);
            }
        }
        String str = stringBuilder.toString();
        //mDataFieldRx.setText(str);

    }

    private void decodemodule(byte[] data) {
        // uptime
        uptimedisplay = UPTIME_ENABLE;

        switch (data[1]) {
            case MOD_EDFA1:
                modDisplay_EDFA1(data);
                break;
            case MOD_EDFA2:
                modDisplay_EDFA2(data);
                break;
            case MOD_EDFA3:
                modDisplay_EDFA3(data);
                break;
            case MOD_ONU:
                modDisplay_ONU(data);
                break;
            case MOD_OTX:
                modDisplay_OTX(data);
                break;
            case MOD_REPEAT1:
                modDisplay_REPEATER1(data);
                break;
            case MOD_RF_AMP:
                modDisplay_AMP(data);
                break;
            case MOD_RMC:
                modDisplay_RMC(data);
                break;
        }
    }

    // jw 데이터 수신
    private void dataRecv(byte[] data) {
        if (data != null) {
            if (data[0] == 0x11) {
                decodemodule(data);
            }
        }
    }

    // jw 데이터 송신
    private void dataSend(byte[] data) {
        List<BluetoothGattService> gattServices = mBluetoothLeService
                .getSupportedGattServices();
        UUID dataUuid = SampleGattAttributes.DATA_TX_UUID;

        for (BluetoothGattService gattService : gattServices) {
            BluetoothGattCharacteristic charac = gattService
                    .getCharacteristic(dataUuid);
            if (charac != null) {
                charac.setValue(data);
                mBluetoothLeService.writeCharacteristic(charac);
            }
        }
    }

	/*
	 * [ims] 지움.
	 * 
	 * // jw Send 버튼 눌렀을때 protected OnClickListener mDataSendClickListener = new
	 * OnClickListener() {
	 * 
	 * @Override public void onClick(View v) { byte [] txData =
	 * mDataFieldTx.getText().toString().getBytes(); dataSend(txData); } };
	 */

    private void sendtomoduleCMD() {
        byte[] txData = {CMD_MOD_VALUE_REQ, 0x03, MOD_NULL, (byte) 0xFE, 0};
        if (mModName != MOD_NULL) {
            txData[2] = (byte) mModName;

            txData[4] = SendChecksum(txData);
            dataSend(txData);
            mod_Block_stat = 0;
        }
    }

    private void setModulename(int name) {
        mModName = name;

        sendtomoduleCMD();
    }

    protected OnClickListener mButtonEDFA1ClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            setModulename(MOD_EDFA1);
        }
    };

    protected void makeTitleView(String str){
        LinearLayout.LayoutParams titleParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        //titleParam.gravity = Gravity.CENTER;
        LinearLayout titleLayout = new LinearLayout(DeviceControlActivity.this);
        TextView title = new TextView(DeviceControlActivity.this);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(titleParam);
        titleLayout.setLayoutParams(titleParam);
        //titleParam.setMargins(2, 2, 2, 0);
        title.setBackgroundColor(Color.parseColor("#000000"));
        //title.setPadding(10, 10, 10, 10);
        title.setTextColor(Color.parseColor("#FF7200"));
        title.setTextSize(20);
        title.setText(str);
        topLL.addView(titleLayout);
        titleLayout.addView(title);
    }

    protected void makeDetailView(String str1, String str2){

        LinearLayout.LayoutParams titleParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        titleParam.gravity = Gravity.CENTER;
        LinearLayout detailLayout = new LinearLayout(DeviceControlActivity.this);
        detailLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView detailTv = new TextView(DeviceControlActivity.this);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);



        //param2.setMargins(2, 2, 2, 0);
        detailTv.setBackgroundColor(Color.parseColor("#FFFFFF"));
        //detailTv.setPadding(10, 10, 10, 10);
        detailTv.setTextColor(Color.parseColor("#000000"));
        detailTv.setTextSize(13);
        detailTv.setText(str1);
        detailTv.setGravity(Gravity.CENTER);

        param2.weight = 1;
        detailTv.setLayoutParams(param2);



        Button detailBtn = new Button(DeviceControlActivity.this);
        //param2.setMargins(2, 2, 2, 0);
        detailBtn.setBackgroundColor(Color.parseColor("#EEEEEE"));
        //detailBtn.setPadding(10, 10, 10, 10);
        detailBtn.setTextColor(Color.parseColor("#000000"));
        detailBtn.setTextSize(13);
        detailBtn.setText(str2);
        detailBtn.setGravity(Gravity.CENTER);
        detailBtn.setOnClickListener(detailBtnClickListener);


        param2.weight = 1;
        detailBtn.setLayoutParams(param2);

        topLL.addView(detailLayout);
        detailLayout.addView(detailTv);
        detailLayout.addView(detailBtn);

    }

    protected OnClickListener mButtonEDFA2ClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            setModulename(MOD_EDFA2);
        }
    };

    protected OnClickListener mButtonONUClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            setModulename(MOD_ONU);
        }
    };

    protected OnClickListener mButtonFTXClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            setModulename(MOD_OTX);
        }
    };

    protected OnClickListener mButtonREPEAT1ClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            setModulename(MOD_REPEAT1);
        }
    };

    protected OnClickListener mButtonRMCClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConnected == false) {
                Toast.makeText(getApplicationContext(), "연결상태가 아닙니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            setModulename(MOD_RMC);
        }
    };

    protected OnClickListener btnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
           topLL.removeAllViews();

        }
    };

    protected OnClickListener detailBtnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(DeviceControlActivity.this,"clicked",Toast.LENGTH_SHORT).show();

        }
    };
}
