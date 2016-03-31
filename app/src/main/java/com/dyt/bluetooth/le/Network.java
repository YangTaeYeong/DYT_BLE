package com.dyt.bluetooth.le;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by user on 2016-03-30.
 */
public class Network {
    public Context context;
    public ArrayList<String[]> al;
    public MyAdapter adapter;

    public Network(Context context, ArrayList<String[]> al, MyAdapter adapter){
        this.context = context;
        this.al = al;
        this.adapter = adapter;
    }

    public final static String TAG = BluetoothLeService.class.getName();
    public int mod_Block_stat;
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
    public static final byte CMD_MOD_VALUE_REQ = (byte) 0x91;
    public static final byte CMD_MOD_VALUE_RES = (byte) 0x11;

    public static  BluetoothLeService mBluetoothLeService;
    public int asciitobin;
    public byte[] Value_Module_all = new byte[128];
    public byte[] Value_fixed = new byte[64];
    public byte[] Value_status = new byte[64];
    public int mModName;
    public int uptimedisplay;

    // jw:여기서 송수신 Enable 처리
    public void setDataNotify() {
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

    public byte SendChecksum(byte[] data) {
        int chksum = 0;
        byte len = data[1];

        for (int i = 1; i <= (len); i++) {
            chksum += data[i];
        }
        chksum = ~chksum;
        return (byte) chksum;
    }

    public byte CalChecksum(byte[] data) {
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
    public boolean moduledata_RecieveCheck(byte[] data) {
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

    // jw 데이터 수신
    public void dataRecv(byte[] data) {
        if (data != null) {
            if (data[0] == 0x11) {
                decodemodule(data);
            }
        }
    }

    public void decodemodule(byte[] data) {
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
    // jw 데이터 송신
    public void dataSend(byte[] data) {
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

    protected void sendtomoduleCMD() {
        byte[] txData = {CMD_MOD_VALUE_REQ, 0x03, MOD_NULL, (byte) 0xFE, 0};
        if (mModName != MOD_NULL) {
            txData[2] = (byte) mModName;

            txData[4] = SendChecksum(txData);
            dataSend(txData);
            mod_Block_stat = 0;
        }
    }

    protected void setModulename(int name) {
        mModName = name;

        sendtomoduleCMD();
    }

    protected void modDisplay_EDFA1(byte[] data) {
        int tmp = 1;
        int tmp2;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            DeviceControlActivity.mtv_ModName.setText("EDFA1 Module");
            return;
        } else {
            al.clear();
            adapter.notifyDataSetChanged();
            al.add(new String[]{"< Alarm List >",""});
            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                al.add(new String[]{"Optical Input", ""});
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                al.add(new String[]{"Optical Output", ""});
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                al.add(new String[]{"LD1 Temperature\n", ""});
            }
            if ((tmp & (0x0001 << 4)) == (0x0001 << 4)) {
                al.add(new String[]{"LD2 Temperature\n", ""});
            }
            if ((tmp & (0x0001 << 6)) == (0x0001 << 6)) {
                al.add(new String[]{"Board Temperature\n", ""});
            }
            al.add(new String[]{"< Status >\n", ""});

            tmp = Value_Module_all[STAT_ + 13] * 256;
            tmp |= (Value_Module_all[STAT_ + 12] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm\n", (float) tmp / 10));
            al.add(new String[]{"Optical Input Power\n", stringBuilder.toString()});
            stringBuilder.setLength(0);
            if (Value_Module_all[STAT_ + 7] == 0x31) {
                al.add(new String[]{"Laser Key Switch Status\n", "ON"});
            } else {
                al.add(new String[]{"Laser Key Switch Status\n", "OFF"});
            }
            if (Value_Module_all[STAT_ + 8] == 0x31) {
                al.add(new String[]{"Laser  Switch\n", "ON"});
            } else {
                al.add(new String[]{"Laser  Switch\n", "OFF"});
            }
            al.add(new String[]{"LD Status\n", "ON"});
            tmp = Value_Module_all[STAT_ + 15] * 256;
            tmp |= (Value_Module_all[STAT_ + 14] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm\n", (float) tmp / 10));
            al.add(new String[]{"Optical Output Power\n", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 19] * 256;
            tmp |= (Value_Module_all[STAT_ + 18] & 0xFF);

            tmp2 = Value_Module_all[STAT_ + 25] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 24] & 0xFF);
            stringBuilder.append(String.format("%.1f mA, %.1f mA\n", (float) (tmp / 10), (float) (tmp2 / 10)));
            al.add(new String[]{"LD Bias Current\n", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 17] * 256;
            tmp |= (Value_Module_all[STAT_ + 16] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 23] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 22] & 0xFF);
            stringBuilder.append(String.format("%.1f `C, %.1f `C\n", (float) tmp / 10, (float) tmp2 / 10));
            al.add(new String[]{"LD Temperature\n", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 11] * 256;
            tmp |= (Value_Module_all[STAT_ + 10] & 0xFF);
            stringBuilder.append(String.format("%.1f `C\n", (float) tmp / 10));
            al.add(new String[]{"Module Temperature\n", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                al.add(new String[]{"Input Alarm\n", "Enable"});
            } else {
                al.add(new String[]{"Input Alarm\n", "Disable"});
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                al.add(new String[]{"Output Alarm\n", "Enable"});
            } else {
                al.add(new String[]{"Output Alarm\n", "Disable"});
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                al.add(new String[]{"LD Temper Alarm\n", "Enable"});
            } else {
                al.add(new String[]{"LD Temper Alarm\n", "Disable"});
            }
            if ((tmp & (0x0001 << 5)) == (0x0001 << 5)) {
                al.add(new String[]{"B'd Temper Alarm\n", "Enable"});
            } else {
                al.add(new String[]{"B'd Temper Alarm\n", "Disable"});
            }

            tmp = Value_Module_all[STAT_ + 29] * 256;
            tmp |= (Value_Module_all[STAT_ + 28] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 31] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 30] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm / %.1f dBm\n",(float) tmp2 / 10, (float) tmp / 10));
            al.add(new String[]{"Input Power Alarm Threshold MIN/MAX\n", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 33] * 256;
            tmp |= (Value_Module_all[STAT_ + 32] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 35] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 34] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm / %.1f dBm\n",(float) tmp2 / 10, (float) tmp / 10));
            al.add(new String[]{"Output Power Alarm Threshold MIN/MAX\n", stringBuilder.toString()});
            stringBuilder.setLength(0);
            al.add(new String[]{"< Reference value >", ""});

            tmp = Value_Module_all[FIX__ + 33] * 256;
            tmp |= (Value_Module_all[FIX__ + 32] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm\n",(float) tmp / 10));
            al.add(new String[]{"Output Power Reference\n", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[FIX__ + 37] * 256;
            tmp |= (Value_Module_all[FIX__ + 36] & 0xFF);
            tmp2 = Value_Module_all[FIX__ + 43] * 256;
            tmp2 |= (Value_Module_all[42] & 0xFF);
            stringBuilder.append(String.format("%.1f mA, %.1f mA\n",(float) tmp / 10, (float) tmp2 / 10));
            al.add(new String[]{"LD Bias Current Reference\n", stringBuilder.toString()});
            stringBuilder.setLength(0);
            tmp = Value_Module_all[FIX__ + 39] * 256;
            tmp |= (Value_Module_all[FIX__ + 38] & 0xFF);
            tmp2 = Value_Module_all[FIX__ + 45] * 256;
            tmp2 |= (Value_Module_all[FIX__ + 44] & 0xFF);
            stringBuilder.append(String.format("%.1f `C, %.1f `C\n",(float) tmp / 10, (float) tmp2 / 10));
            al.add(new String[]{"LD Temperature  Reference\n", stringBuilder.toString()});
            stringBuilder.setLength(0);
            al.add(new String[]{"<Module Information>", ""});
            stringBuilder.setLength(0);

            tmp = (Value_Module_all[FIX__ + 5] & 0xFF);
            stringBuilder.append(String.format("%.1f\n", (float) tmp / 10));
            al.add(new String[]{"SW Version", stringBuilder.toString()});
            stringBuilder.setLength(0);
            // Serial 번호 표시
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, (FIX__ + 6), tmpStr, 0, 8);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Serial Number   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Serial Number", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("%d", tmp));

            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            al.add(new String[]{"HW Version", stringBuilder.toString()});
            stringBuilder.setLength(0);

            // Model 명 표시
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, (FIX__ + 17), tmpStr, 0, 15);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Model Name", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 39] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 38] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 37] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 36] & 0xFF));
                stringBuilder.append(String.format("[%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Uptime", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
        }
        adapter.notifyDataSetChanged();
    }

    protected void modDisplay_EDFA2(byte[] data) {
        int tmp;
        int tmp2;
        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            DeviceControlActivity.mtv_ModName.setText("EDFA2 Module");
            return;
        } else {
            al.clear();
            adapter.notifyDataSetChanged();
            al.add(new String[]{"< Alarm List >",""});
            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                al.add(new String[]{"Optical Input\n", ""});
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                al.add(new String[]{"Optical Output\n", ""});
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                al.add(new String[]{"LD1 Temperature\n", ""});
            }
            if ((tmp & (0x0001 << 4)) == (0x0001 << 4)) {
                al.add(new String[]{"LD2 Temperature\n", ""});
            }
            if ((tmp & (0x0001 << 6)) == (0x0001 << 6)) {
                al.add(new String[]{"Board Temperature\n", ""});
            }
            al.add(new String[]{"< Status >", ""});

            tmp = Value_Module_all[STAT_ + 13] * 256;
            tmp |= (Value_Module_all[STAT_ + 12] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm\n", (float) tmp / 10));
            al.add(new String[]{"Optical Input Power", stringBuilder.toString()});
            stringBuilder.setLength(0);
            if (Value_Module_all[STAT_ + 7] == 0x31) {
                stringBuilder.append(String.format("ON\n"));
                al.add(new String[]{"Laser Key Switch Status", "ON"});
                stringBuilder.setLength(0);
            } else {
                al.add(new String[]{"Laser Key Switch Status", "OFF"});
            }
            if (Value_Module_all[STAT_ + 8] == 0x31) {
                al.add(new String[]{"Laser  Switch", "ON"});
            } else {
                al.add(new String[]{"Laser  Switch", "OFF"});
            }
            al.add(new String[]{"LD Status", "ON"});
            tmp = Value_Module_all[STAT_ + 15] * 256;
            tmp |= (Value_Module_all[STAT_ + 14] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm\n", (float) tmp / 10));
            al.add(new String[]{"Optical Output Power", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 19] * 256;
            tmp |= (Value_Module_all[STAT_ + 18] & 0xFF);

            tmp2 = Value_Module_all[STAT_ + 25] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 24] & 0xFF);
            stringBuilder.append(String.format("%.1f mA, %.1f mA\n",(float) (tmp / 10), (float) (tmp2 / 10)));
            al.add(new String[]{"LD Bias Current", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 17] * 256;
            tmp |= (Value_Module_all[STAT_ + 16] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 23] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 22] & 0xFF);
            stringBuilder.append(String.format("%.1f `C, %.1f `C\n", (float) tmp / 10, (float) tmp2 / 10));
            al.add(new String[]{"LD Temperature", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 11] * 256;
            tmp |= (Value_Module_all[STAT_ + 10] & 0xFF);
            stringBuilder.append(String.format("%.1f `C\n", (float) tmp / 10));
            al.add(new String[]{"Module Temperature", stringBuilder.toString()});
            stringBuilder.setLength(0);
            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                al.add(new String[]{"Input Alarm", "Enable"});
            } else {
                al.add(new String[]{"Input Alarm", "Disable"});
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                al.add(new String[]{"Output Alarm", "Enable"});
            } else {
                al.add(new String[]{"Output Alarm", "Disable"});
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                al.add(new String[]{"LD Temper Alarm", "Enable"});
            } else {
                al.add(new String[]{"LD Temper Alarm", "Disable"});
            }
            if ((tmp & (0x0001 << 5)) == (0x0001 << 5)) {
                al.add(new String[]{"B'd Temper Alarm", "Enable"});
            } else {
                al.add(new String[]{"B'd Temper Alarm", "Disable"});
            }

            tmp = Value_Module_all[STAT_ + 29] * 256;
            tmp |= (Value_Module_all[STAT_ + 28] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 31] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 30] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm / %.1f dBm\n",(float) tmp2 / 10, (float) tmp / 10));
            al.add(new String[]{"Input Power Alarm Threshold MIN/MAX", stringBuilder.toString()});
            stringBuilder.setLength(0);
            tmp = Value_Module_all[STAT_ + 33] * 256;
            tmp |= (Value_Module_all[STAT_ + 32] & 0xFF);
            tmp2 = Value_Module_all[STAT_ + 35] * 256;
            tmp2 |= (Value_Module_all[STAT_ + 34] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm / %.1f dBm\n",(float) tmp2 / 10, (float) tmp / 10));
            al.add(new String[]{"Output Power Alarm Threshold MIN/MAX", stringBuilder.toString()});
            stringBuilder.setLength(0);
            al.add(new String[]{"< Reference value >", ""});

            tmp = Value_Module_all[FIX__ + 33] * 256;
            tmp |= (Value_Module_all[FIX__ + 32] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm\n",(float) tmp / 10));
            al.add(new String[]{"Output Power Reference", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[FIX__ + 37] * 256;
            tmp |= (Value_Module_all[FIX__ + 36] & 0xFF);
            tmp2 = Value_Module_all[FIX__ + 43] * 256;
            tmp2 |= (Value_Module_all[42] & 0xFF);
            stringBuilder.append(String.format("%.1f mA, %.1f mA\n", (float) tmp / 10, (float) tmp2 / 10));
            al.add(new String[]{"LD Bias Current Reference", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[FIX__ + 39] * 256;
            tmp |= (Value_Module_all[FIX__ + 38] & 0xFF);
            tmp2 = Value_Module_all[FIX__ + 45] * 256;
            tmp2 |= (Value_Module_all[FIX__ + 44] & 0xFF);
            stringBuilder.append(String.format("%.1f `C, %.1f `C\n",(float) tmp / 10, (float) tmp2 / 10));
            al.add(new String[]{"LD Temperature  Reference", stringBuilder.toString()});
            stringBuilder.setLength(0);

            al.add(new String[]{"<Module Information>", ""});

            tmp = (Value_Module_all[FIX__ + 5] & 0xFF);
            stringBuilder.append(String.format("%.1f\n", (float) tmp / 10));
            al.add(new String[]{"SW Version", stringBuilder.toString()});
            stringBuilder.setLength(0);
            // Serial 번호 표시
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, (FIX__ + 6), tmpStr, 0, 8);
                String Str = new String(tmpStr);

                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Serial Number", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("%d", tmp));
            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            al.add(new String[]{"HW Version", stringBuilder.toString()});
            stringBuilder.setLength(0);
            // Model 명 표시
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, (FIX__ + 17), tmpStr, 0, 15);
                String Str = new String(tmpStr);

                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Model Name", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 39] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 38] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 37] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 36] & 0xFF));
                stringBuilder.append(String.format("[%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Uptime", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
        }
        adapter.notifyDataSetChanged();
    }

    protected void modDisplay_EDFA3(byte[] data) {
        int tmp;
        int tmp2;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            DeviceControlActivity.mtv_ModName.setText("EDFA3 Module");
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
            stringBuilder.append(String.format("< Reference value >"));

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

            stringBuilder.append(String.format("<Module Information>"));

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
        adapter.notifyDataSetChanged();
    }

    protected void modDisplay_ONU(byte[] data) {
        int tmp;
        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            DeviceControlActivity.mtv_ModName.setText("ONU Module");
            return;
        } else {
            al.clear();
            adapter.notifyDataSetChanged();
            al.add(new String[]{"< Alarm List>", ""});
            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                al.add(new String[]{"Input Power\n", ""});
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                al.add(new String[]{"Output Power\n", ""});
            }
            al.add(new String[]{"< Status >", ""});

            tmp = Value_Module_all[STAT_ + 8] * 256;
            tmp |= (Value_Module_all[STAT_ + 7] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm\n", (float) tmp / 10));
            al.add(new String[]{"Input Power", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 10] * 256;
            tmp |= (Value_Module_all[STAT_ + 9] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm\n", (float) tmp / 10));
            al.add(new String[]{"Input Power", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 16] * 256;
            tmp |= (Value_Module_all[STAT_ + 15] & 0xFF);
            stringBuilder.append(String.format("%.1f `C\n", (float) tmp / 10));
            al.add(new String[]{"Module Temperature", stringBuilder.toString()});
            stringBuilder.setLength(0);
            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                al.add(new String[]{"Input Power Alarm", "Enable"});
            } else {
                al.add(new String[]{"Input Power Alarm", "Disable"});
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                al.add(new String[]{"Output Power Alarm", "Enable"});
            } else {
                al.add(new String[]{"Output Power Alarm", "Disable"});
            }
            al.add(new String[]{"<Module Information>",""});
            tmp = (Value_Module_all[5] & 0xFF);
            stringBuilder.append(String.format("%.1f\n", (float) tmp / 10));
            al.add(new String[]{"SW Version", stringBuilder.toString()});
            stringBuilder.setLength(0);
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, (FIX__ + 6), tmpStr, 0, 8);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"\nSerial Numbe\n", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("%d", tmp));

            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            al.add(new String[]{"\nHW Version\n", stringBuilder.toString()});
            stringBuilder.setLength(0);
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, (FIX__ + 17), tmpStr, 0, 15);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Model Name", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 14] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 13] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 12] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 11] & 0xFF));
                stringBuilder.append(String.format("[%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Uptime", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
        }
        adapter.notifyDataSetChanged();
    }

    protected void modDisplay_OTX(byte[] data) {
        int tmp;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            DeviceControlActivity.mtv_ModName.setText("FTX Module");
            return;
        } else {
            al.clear();
            adapter.notifyDataSetChanged();
            al.add(new String[]{"< Alarm List>\n", ""});
            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                al.add(new String[]{"Opt Output Power\n",""});
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                al.add(new String[]{"LASER Temperature\n", ""});
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                al.add(new String[]{"RF Input Power\n", ""});
            }

            al.add(new String[]{"< Status >", ""});

            if (Value_Module_all[STAT_ + 8] == 0x31) {
                al.add(new String[]{"RF Mode Status", "MGC"});
            } else {
                al.add(new String[]{"RF Mode Status", "AGC"});
            }
            if (Value_Module_all[STAT_ + 9] == 0x31) {
                al.add(new String[]{"RF Type", "CW"});
            } else {
                al.add(new String[]{"RF Type", "VIDEO"});
            }

            if (Value_Module_all[STAT_ + 11] == 0x31) {
                al.add(new String[]{"RF Status", "RF Low"});
            } else if (Value_Module_all[STAT_ + 11] == 0x32) {
                al.add(new String[]{"RF Status", "RF High"});
            } else {
                al.add(new String[]{"RF Status", "RF Normal"});
            }

            tmp |= (Value_Module_all[STAT_ + 10] & 0xFF);
            stringBuilder.append(String.format("%.1f dB\n", (float) tmp / 10));
            al.add(new String[]{"Input Attenuation", stringBuilder.toString()});
            stringBuilder.setLength(0);

            if (Value_Module_all[STAT_ + 7] == 0x31) {
                al.add(new String[]{"LD Key Status", "ON"});
            } else {
                al.add(new String[]{"LD Key Status", "OFF"});
            }

            if (Value_Module_all[STAT_ + 12] == 0x31) {
                al.add(new String[]{"Laser Switch", "ON"});
            } else {
                al.add(new String[]{"Laser Switch", "OFF"});
            }
            tmp = Value_Module_all[STAT_ + 14] * 256;
            tmp |= (Value_Module_all[STAT_ + 13] & 0xFF);
            stringBuilder.append(String.format("%.1f mW\n", (float) tmp / 10));
            al.add(new String[]{"LD Power (mW)", stringBuilder.toString()});
            stringBuilder.setLength(0);
            tmp = Value_Module_all[STAT_ + 16] * 256;
            tmp |= (Value_Module_all[STAT_ + 15] & 0xFF);
            stringBuilder.append(String.format("%.1f dBm\n", (float) tmp / 10));
            al.add(new String[]{"LD Power (dBm)", stringBuilder.toString()});
            stringBuilder.setLength(0);
            tmp = Value_Module_all[STAT_ + 18] * 256;
            tmp |= (Value_Module_all[STAT_ + 17] & 0xFF);
            stringBuilder.append(String.format("%.1f mA\n", (float) tmp / 10));
            al.add(new String[]{"LD Bias Current", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 20] * 256;
            tmp |= (Value_Module_all[STAT_ + 19] & 0xFF);
            stringBuilder.append(String.format("%.1f `C\n", (float) tmp / 10));
            al.add(new String[]{"LD Temperature", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 22] * 256;
            tmp |= (Value_Module_all[STAT_ + 21] & 0xFF);
            stringBuilder.append(String.format("%.1f dB\n", (float) tmp / 10));
            al.add(new String[]{"OMI Status", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 24] * 256;
            tmp |= (Value_Module_all[STAT_ + 23] & 0xFF);
            stringBuilder.append(String.format("%.1f dB\n", (float) tmp / 10));
            al.add(new String[]{"OMI Reference Level", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = Value_Module_all[STAT_ + 26] * 256;
            tmp |= (Value_Module_all[STAT_ + 25] & 0xFF);
            stringBuilder.append(String.format("%.1f `C\n", (float) tmp / 10));
            al.add(new String[]{"Module Temperature", stringBuilder.toString()});
            stringBuilder.setLength(0);

            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                al.add(new String[]{"Output Power Alarm", "Enable"});

            } else {
                al.add(new String[]{"Output Power Alarm", "Disable"});
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                al.add(new String[]{"LD Temperature Alarm", "Enable"});
            } else {
                al.add(new String[]{"LD Temperature Alarm", "Disable"});
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                al.add(new String[]{"RF Input Power Alarm", "Enable"});
            } else {
                al.add(new String[]{"RF Input Power Alarm", "Disable"});
            }
            al.add(new String[]{"\n<Module Information>\n", ""});
            tmp = (Value_Module_all[5] & 0xFF);
            stringBuilder.append(String.format("%.1f\n", (float) tmp / 10));
            al.add(new String[]{"SW Version", stringBuilder.toString()});
            stringBuilder.setLength(0);
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, 6, tmpStr, 0, 8);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Serial Number", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("%d", tmp));
            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            al.add(new String[]{"HW Version", stringBuilder.toString()});
            stringBuilder.setLength(0);
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, 17, tmpStr, 0, 15);
                String Str = new String(tmpStr);

                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Model Name", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 31] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 30] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 29] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 28] & 0xFF));
                stringBuilder.append(String.format("[%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Uptime", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
        }
    }

    protected void modDisplay_REPEATER1(byte[] data) {
        int tmp;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            DeviceControlActivity.mtv_ModName.setText("Repeater1 Module");
            return;
        } else {
            al.clear();
            adapter.notifyDataSetChanged();
            al.add(new String[]{"< Alarm List >\n", ""});

            tmp = ((Value_Module_all[STAT_ + 4] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 3] & 0xFF));
            if ((tmp & (0x0001 << 0)) == (0x0001 << 0)) {
                al.add(new String[]{"ORX1 Alarm Status", "Raised"});
            }
            if ((tmp & (0x0001 << 1)) == (0x0001 << 1)) {
                al.add(new String[]{"ORX2 Alarm Status", "Raised"});
            }
            if ((tmp & (0x0001 << 2)) == (0x0001 << 2)) {
                al.add(new String[]{"ORX3 Alarm Status", "Raised"});
            }
            if ((tmp & (0x0001 << 3)) == (0x0001 << 3)) {
                al.add(new String[]{"ORX4 Alarm Status", "Raised"});
            }
            if ((tmp & (0x0001 << 4)) == (0x0001 << 4)) {
                al.add(new String[]{"ORX5 Alarm Status", "Raised"});
            }
            if ((tmp & (0x0001 << 5)) == (0x0001 << 5)) {
                al.add(new String[]{"ORX6 Alarm Status", "Raised"});
            }
            if ((tmp & (0x0001 << 6)) == (0x0001 << 6)) {
                al.add(new String[]{"ORX7 Alarm Status", "Raised"});
            }
            if ((tmp & (0x0001 << 7)) == (0x0001 << 7)) {
                al.add(new String[]{"ORX8 Alarm Status", "Raised"});
            }
            if ((tmp & (0x0001 << 8)) == (0x0001 << 8)) {
                al.add(new String[]{"OTX Alarm Status", "Raised"});
            }
            al.add(new String[]{"< Status >", ""});

            tmp = Value_Module_all[STAT_ + 9] * 256;
            tmp |= (Value_Module_all[STAT_ + 8] & 0xFF);
            al.add(new String[]{"OTx Out Power", String.format("%.1f dBm\n",(float)tmp/10).toString()});

            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 0)) == (0x0001 << 0)) {
                tmp = Value_Module_all[STAT_ + 11] * 256;
                tmp |= (Value_Module_all[STAT_ + 10] & 0xFF);
                al.add(new String[]{"ORx1 Rx Power", String.format("%.1f dBm\n",(float)tmp/10).toString()});
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 1)) == (0x0001 << 1)) {
                tmp = Value_Module_all[STAT_ + 13] * 256;
                tmp |= (Value_Module_all[STAT_ + 12] & 0xFF);
                al.add(new String[]{"ORx2 Rx Power", String.format("%.1f dBm\n",(float)tmp/10).toString()});
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 2)) == (0x0001 << 2)) {
                tmp = Value_Module_all[STAT_ + 15] * 256;
                tmp |= (Value_Module_all[STAT_ + 14] & 0xFF);
                al.add(new String[]{"ORx3 Rx Power", String.format("%.1f dBm\n", (float) tmp / 10).toString()});
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 3)) == (0x0001 << 3)) {
                tmp = Value_Module_all[STAT_ + 17] * 256;
                tmp |= (Value_Module_all[STAT_ + 16] & 0xFF);
                al.add(new String[]{"ORx4 Rx Power", String.format("%.1f dBm\n",(float)tmp/10).toString()});
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 4)) == (0x0001 << 4)) {
                tmp = Value_Module_all[STAT_ + 19] * 256;
                tmp |= (Value_Module_all[STAT_ + 18] & 0xFF);
                al.add(new String[]{"ORx5 Rx Power", String.format("%.1f dBm\n",(float)tmp/10).toString()});
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 5)) == (0x0001 << 5)) {
                tmp = Value_Module_all[STAT_ + 21] * 256;
                tmp |= (Value_Module_all[STAT_ + 20] & 0xFF);
                al.add(new String[]{"ORx6 Rx Power", String.format("%.1f dBm\n",(float)tmp/10).toString()});
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 6)) == (0x0001 << 6)) {
                tmp = Value_Module_all[STAT_ + 23] * 256;
                tmp |= (Value_Module_all[STAT_ + 22] & 0xFF);
                al.add(new String[]{"ORx7 Rx Power", String.format("%.1f dBm\n",(float)tmp/10).toString()});
            }
            if ((Value_Module_all[STAT_ + 7] & (0x0001 << 7)) == (0x0001 << 7)) {
                tmp = Value_Module_all[STAT_ + 25] * 256;
                tmp |= (Value_Module_all[STAT_ + 24] & 0xFF);
                al.add(new String[]{"ORx8 Rx Power", String.format("%.1f dBm\n",(float)tmp/10).toString()});
            }

            tmp = Value_Module_all[STAT_ + 27] * 256;
            tmp |= (Value_Module_all[STAT_ + 26] & 0xFF);
            al.add(new String[]{"Module Temperature", String.format("%.1f `C\n", (float) tmp / 10).toString()});
            al.add(new String[]{"< Install ORx Module >",""});
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
            al.add(new String[]{stringBuilder.toString(), ""});
            stringBuilder.setLength(0);

            tmp = ((Value_Module_all[STAT_ + 6] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 5] & 0xFF));
            if ((tmp & (0x0001 << 8)) == (0x0001 << 8)) {
                al.add(new String[]{"OTX Alarm","Enable"});
            } else {
                al.add(new String[]{"OTX Alarm","Disable"});
            }
            //stringBuilder.append(String.format("ORX Alarm\n"));
            al.add(new String[]{"ORX Alarm\n", ""});

            //stringBuilder.append(String.format("  Eable   ::"));
            al.add(new String[]{"Enable\n",""});
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
            al.add(new String[]{stringBuilder.toString(),""});
            stringBuilder.setLength(0);
            stringBuilder.append(String.format("Disable ::"));
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
            al.add(new String[]{stringBuilder.toString(), ""});
            stringBuilder.setLength(0);
            al.add(new String[]{"<Module Information>", ""});
            tmp = (Value_Module_all[5] & 0xFF);
            al.add(new String[]{"SW Version", String.format("%.1f\n",(float)tmp/10).toString()});
            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, 6, tmpStr, 0, 8);
                String Str = new String(tmpStr);
                stringBuilder.append(String.format("Serial Number   :: "));
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Serial Numbe", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append(String.format("%d", tmp));
            {
                byte[] tmpStr = new byte[1];
                System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr, 0, 1);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
            }
            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));
            al.add(new String[]{"HW Version", stringBuilder.toString()});
            stringBuilder.setLength(0);
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, 17, tmpStr, 0, 15);
                String Str = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str));
                stringBuilder.append(String.format("\n"));

                al.add(new String[]{"Model Name", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 31] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 30] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 29] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 28] & 0xFF));
                stringBuilder.append(String.format("[%d]", tmp));
                stringBuilder.append(String.format("[%d:%d.%d]", (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Model Name", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
        }
        adapter.notifyDataSetChanged();
    }

    protected void modDisplay_AMP(byte[] data) {

        int tmp;

        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            DeviceControlActivity.mtv_ModName.setText("RF Amp Module");
            //stringBuilder.append(String.format("Receiving Data...\n"));
            al.clear();
            adapter.notifyDataSetChanged();
            al.add(new String[]{"RF Amp Module", ""});
            return;
        }
        //mDataFieldRx.setText(stringBuilder.toString());
    }

    protected void modDisplay_RMC(byte[] data) {
        int tmp;
        final StringBuilder stringBuilder = new StringBuilder(2048); // data.length);

        if (moduledata_RecieveCheck(data) == false) {
            DeviceControlActivity.mtv_ModName.setText("RMC Module");
            return;
        } else {
            al.clear();
            adapter.notifyDataSetChanged();
            tmp = (Value_Module_all[5] & 0xFF);
            al.add(new String[]{"<Module Information>", ""});

            stringBuilder.setLength(0);

            stringBuilder.append(String.format("%.1f\n", (float) tmp / 10));
            al.add(new String[]{"SW Version", stringBuilder.toString()});

            {
                byte[] tmpStr = new byte[8];
                System.arraycopy(Value_Module_all, 6, tmpStr, 0, 8);
                String Str = new String(tmpStr);
                al.add(new String[]{"Serial Number", "" + String.valueOf(Str) + "\n"});

                stringBuilder.setLength(0);
            }
            // Hw version 표시
            tmp = (Value_Module_all[FIX__ + 14] & 0xFF);
            stringBuilder.append("" + tmp);

            byte[] tmpStr1 = new byte[1];
            System.arraycopy(Value_Module_all, (FIX__ + 15), tmpStr1, 0, 1);
            String Str = new String(tmpStr1);
            stringBuilder.append(String.valueOf(Str));

            tmp = (Value_Module_all[FIX__ + 16] & 0xFF);
            stringBuilder.append(String.format(".%d\n", tmp));

            al.add(new String[]{"HW Version", stringBuilder.toString()});

            stringBuilder.setLength(0);
            {
                byte[] tmpStr = new byte[15];
                System.arraycopy(Value_Module_all, 17, tmpStr, 0, 15);
                String Str1 = new String(tmpStr);
                stringBuilder.append(String.valueOf(Str1));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Model Name", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }

            //uptime display
            if (uptimedisplay == UPTIME_ENABLE) {
                tmp = ((Value_Module_all[STAT_ + 40] << 24 & 0xFF000000)
                        | (Value_Module_all[STAT_ + 39] << 16 & 0xFF0000)
                        | (Value_Module_all[STAT_ + 38] << 8 & 0xFF00) | (Value_Module_all[STAT_ + 37] & 0xFF));
                stringBuilder.setLength(0);
                stringBuilder.append(String.format("[%d][%d:%d.%d]", tmp, (tmp / 3600), (tmp % 3600) / 60, (tmp % 60)));
                stringBuilder.append(String.format("\n"));
                al.add(new String[]{"Uptime", stringBuilder.toString()});

                al.add(new String[]{"Uptime", stringBuilder.toString()});

                al.add(new String[]{"Uptime", stringBuilder.toString()});
                stringBuilder.setLength(0);
            }
        }
        String str = stringBuilder.toString();
        //adapter.notifyDataSetChanged();
    }
}
