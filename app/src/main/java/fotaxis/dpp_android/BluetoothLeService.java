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

package fotaxis.dpp_android;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_DISCARD_COUNT =
            "DPP_ACTION_DATA_DISCARD";

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static String ACTION_DATA_CO2 =
            "com.example.bluetooth.le.ACTION_DATA_CO2";
    public final static String ACTION_DATA_CO =
            "com.example.bluetooth.le.ACTION_DATA_CO";
    public final static String ACTION_DATA_TEMP =
            "com.example.bluetooth.le.ACTION_DATA_TEMP";
    public final static String ACTION_DATA_ACC =
            "com.example.bluetooth.le.ACTION_DATA_ACC";

    public final static String ACTION_DATA_ALL =
            "com.example.bluetooth.le.ACTION_DATA_ALL";





    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            if(SampleGattAttributes.SENSOR_DATA_CO2.equals(characteristic.getUuid().toString())){
                broadcastUpdate(ACTION_DATA_CO2, characteristic);
            }else if(SampleGattAttributes.SENSOR_DATA_CO.equals(characteristic.getUuid().toString())){
                broadcastUpdate(ACTION_DATA_CO, characteristic);
            }else if(SampleGattAttributes.SENSOR_DATA_TEMPERATURE.equals(characteristic.getUuid().toString())){
                broadcastUpdate(ACTION_DATA_TEMP, characteristic);
            }else if(SampleGattAttributes.SENSOR_DATA_ACCE.equals(characteristic.getUuid().toString())){
                broadcastUpdate(ACTION_DATA_ACC, characteristic);
            }else{
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }

        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    StringBuilder sb;

    @Override
    public void onCreate() {
        sb=new StringBuilder();
        super.onCreate();
    }

    long discardCount=0;

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        String acceleration="";
        String otherData="";

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
        //    intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else if(SampleGattAttributes.SENSOR_DATA_CO2.equals(characteristic.getUuid().toString())){
            //String val=characteristic.getStringValue(0);
            //int val= characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,0);
            //intent.putExtra(EXTRA_DATA, val+"");


            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);

                for(byte byteChar : data) {
                    stringBuilder.append((char) byteChar);
                }
                sb.append(stringBuilder.toString());
                if(sb.length()>30){
                    //do string operation you already have data;
                    acceleration=parseAccleration();
                    otherData=parseOtherData();
                }

                if(sb.length()>50){ //clear if too much junk is accomulated
                    sb.delete(0, sb.length());
                }

            }
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                //intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }


        if(acceleration.trim().length()>0){
            if(acceleration.trim().length()==7){
                //it can't be more than 7, discard junk
                Intent intent= new Intent(ACTION_DATA_ACC);
                intent.putExtra(EXTRA_DATA, acceleration);
                sendBroadcast(intent);
            }else{
                sendDiscardCount();
            }

        }
        otherData=otherData.trim();
        if(otherData.length()>0){
            //TODO: clean this code later;

            Intent intent= new Intent(ACTION_DATA_ALL);
            intent.putExtra(EXTRA_DATA, otherData);
            sendBroadcast(intent);

            String []dataArray=otherData.split(",");
            try {
                float temp = Float.parseFloat(dataArray[0].trim());
                intent= new Intent(ACTION_DATA_TEMP);
                intent.putExtra(EXTRA_DATA, temp);
                sendBroadcast(intent);
            }catch (NumberFormatException ex){
                sendDiscardCount();
                Log.d("DPP","Temp exception "+ex.getMessage());
            }catch (IndexOutOfBoundsException ex){
                sendDiscardCount();
                Log.d("DPP","Temp exception "+ex.getMessage());
            }

            try {
                int co = Integer.parseInt(dataArray[1].trim());
                intent= new Intent(ACTION_DATA_CO);
                intent.putExtra(EXTRA_DATA, co);
                sendBroadcast(intent);
            }catch (NumberFormatException ex){
                sendDiscardCount();
                Log.d("DPP","CO exception "+ex.getMessage());
            }catch (IndexOutOfBoundsException ex){
                sendDiscardCount();
                Log.d("DPP","CO exception "+ex.getMessage());
            }

            try {
                int co2 = Integer.parseInt(dataArray[2].trim());
                intent= new Intent(ACTION_DATA_CO2);
                intent.putExtra(EXTRA_DATA, co2);
                sendBroadcast(intent);
            }catch (NumberFormatException ex){
                sendDiscardCount();
                Log.d("DPP","Co2 exception "+ex.getMessage());
            }catch (IndexOutOfBoundsException ex){
                sendDiscardCount();
                Log.d("DPP","Co2 exception "+ex.getMessage());
            }
        }
        //sendBroadcast(intent);
    }

    public void sendDiscardCount(){
        discardCount++;
        Intent intent= new Intent(ACTION_DISCARD_COUNT);
        intent.putExtra(EXTRA_DATA, discardCount);
        sendBroadcast(intent);
    }
    public String parseOtherData(){
        String otherData="";
        int s=sb.indexOf("s");
        int semi=sb.indexOf(";");
        if(s>=0){
            if(semi>s){
                otherData=sb.substring(s+1,semi);
                sb.delete(s,semi+1);
            }else if(semi<s){
                if(semi!=-1){
                    sb.delete(semi,s);
                }
            }
        }
        return otherData;
    }

    public String parseAccleration(){
        String acceleration="";
        int a=sb.indexOf("a");
        int semi=sb.indexOf("e");
        if(a>=0){
            if(semi>a){
                //normal case;
                acceleration=sb.substring(a+1,semi);
                sb.delete(a,semi+1);
            }else{
                if(semi!=-1){
                    sb.delete(semi,a);
                }
            }
        }
        return acceleration;
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        if (SampleGattAttributes.SENSOR_DATA_CO2.equals(characteristic.getUuid().toString())
                ||SampleGattAttributes.SENSOR_DATA_CO.equals(characteristic.getUuid().toString())
                ||SampleGattAttributes.SENSOR_DATA_TEMPERATURE.equals(characteristic.getUuid().toString())
                ||SampleGattAttributes.SENSOR_DATA_ACCE.equals(characteristic.getUuid().toString())) {

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);


    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
