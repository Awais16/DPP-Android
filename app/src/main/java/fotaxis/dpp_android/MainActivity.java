package fotaxis.dpp_android;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;

import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private DppGraphs dppAcceleroGraph;
    private DppGraphs dppCOGraph;
    private DppGraphs dppCO2Graph;
    private DppGraphs dppTempGraph;
    private DppGraphs dppComparisonGraph;

    private BluetoothDevice btDevice;

    private static int REQUEST_ENABLE_BT=1001;
    private final static int REQUEST_PERMISSION_LOCATION=1002;
    private static int REQUEST_LOCATION_SETTINGS=1003;

    private boolean mScanning;
    private Handler mHandler;

    TextView tv_status;

    @Bind(R.id.tv_discardCount) TextView tv_discardCount;
    @Bind(R.id.tv_acceleration) TextView tv_acceleration;
    @Bind(R.id.tv_co2) TextView tv_co2;
    @Bind(R.id.tv_co) TextView tv_co;
    @Bind(R.id.tv_temp) TextView tv_temp;
    @Bind(R.id.bt_start) Button bt_start;
    @Bind(R.id.tv_compare) TextView tv_compare;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private ExpandableListView mGattServicesList;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private void scanLeDevice(final boolean enable) {
        tv_status.setText("Scanning DPP BLE node");
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    //tv_status.setText("Finished Scanning...");
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            //debug(device.getName()+","+rssi);
            String btDeviceName=device.getName();
            if(btDeviceName.contains("DPP")){
                debug(btDeviceName);
                btDevice=device;
                mBluetoothAdapter.stopLeScan(this);
                connectToBTNode();
            }
        }
    };

    private void connectToBTNode(){
        tv_status.setText("Connecting to node: "+btDevice.getName()+":"+btDevice.getAddress());

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        tv_status=(TextView)findViewById(R.id.tv_status);

        dppAcceleroGraph= new DppGraphs(getApplication());
        dppAcceleroGraph.setView(findViewById(android.R.id.content));
        dppAcceleroGraph.setXleroLineChart(80f,200f,R.id.chart_acceleration);

        dppCO2Graph= new DppGraphs(getApplication());
        dppCO2Graph.setView(findViewById(android.R.id.content));
        dppCO2Graph.setXleroLineChart(600f,3000f,R.id.chart_co2);

        dppCOGraph= new DppGraphs(getApplication());
        dppCOGraph.setView(findViewById(android.R.id.content));
        dppCOGraph.setXleroLineChart(270f,600f,R.id.chart_co);

        dppTempGraph= new DppGraphs(getApplication());
        dppTempGraph.setView(findViewById(android.R.id.content));
        dppTempGraph.setXleroLineChart(20f,40f,R.id.chart_temp);

        dppComparisonGraph= new DppGraphs(getApplication());
        dppComparisonGraph.setView(findViewById(android.R.id.content));
        dppComparisonGraph.setXleroLineChart(20f,2200f,R.id.chart_compare);


        //dppGraph.feedMultiple(this);
        bluetoothManager=(BluetoothManager) getSystemService(this.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler= new Handler();



        //for blue

        //mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        //mGattServicesList.setOnChildClickListener(servicesListClickListner);



        /*
        Button bt_feed= (Button) findViewById(R.id.bt_feed);
        bt_feed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dppGraph.feedMultiple(MainActivity.this);
            }
        });*/

        bt_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*for(BluetoothGattCharacteristic chars:notificationChars){
                    listenToCharacteristic(chars);
                }*/
                if(mConnected && mBluetoothLeService!=null) {
                    listenToCharacteristic(notificationChars.get(1));
                    //i++; //wtf? why its listening to all three now
                }else{
                   Toast.makeText(getApplicationContext(),"Connect to sensor node first",Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    int i=0;
    void debug(String log){
        Log.d("dpp", log);
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    if(!isLocationEnabled()) {
                        Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        this.startActivityForResult(enableLocationIntent, REQUEST_LOCATION_SETTINGS);
                    } else{
                        scanLeDevice(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(),"Application needs location permission",Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }




    public boolean isLocationEnabled() {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }else{
            locationProviders = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    void enableBLE(){
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            //requestLocationPermission();
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_PERMISSION_LOCATION);
//            scanLeDevice(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_settings:
                break;
            case R.id.action_scan:
                 enableBLE();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private BluetoothLeService mBluetoothLeService;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("dpp", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(btDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private boolean mConnected;

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState(R.string.connected);
                tv_status.setText("Connected to:" + btDevice.getName() + "(" + btDevice.getAddress().toString() + ")");
                bt_start.setEnabled(true);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                bt_start.setEnabled(false);
                tv_status.setText("Disconnected from:" + btDevice.getName() + "(" + btDevice.getAddress().toString() + ")");
                invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if(BluetoothLeService.ACTION_DATA_ALL.equals(action)){
                String acc=intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                tv_compare.setText(acc);
                try{
                    String []datArra=acc.split(",");
                    float temp=Float.parseFloat(datArra[0]);
                    float co=Float.parseFloat(datArra[1]);
                    float co2=Float.parseFloat(datArra[2]);
                    dppComparisonGraph.add3Gases(co,co2,temp);
                }catch (Exception ex){

                }
            }else if(BluetoothLeService.ACTION_DATA_CO2.equals(action)){
                int co2=intent.getIntExtra(BluetoothLeService.EXTRA_DATA, -1);
                dppCO2Graph.addGasEntry(co2,"CO2",Color.GREEN);
                tv_co2.setText("CO2 (" + co2 + ")");
            }else if(BluetoothLeService.ACTION_DATA_CO.equals(action)){
                int co=intent.getIntExtra(BluetoothLeService.EXTRA_DATA,-1);
                dppCOGraph.addGasEntry(co,"CO",Color.RED);
                tv_co.setText("CO ("+co+")");
            }else if(BluetoothLeService.ACTION_DATA_TEMP.equals(action)){
                float temp=intent.getFloatExtra(BluetoothLeService.EXTRA_DATA, -1);
                dppTempGraph.addGasEntry(temp,"Temperature (Degrees C)",Color.BLUE);
                tv_temp.setText("Temperature ("+temp+") c");
            }else if(BluetoothLeService.ACTION_DATA_ACC.equals(action)){
                String acc=intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                tv_acceleration.setText("Acceleration("+acc+")");
                String []dataArr=acc.split(",");
                int x=Integer.parseInt(dataArr[0]);
                int y=Integer.parseInt(dataArr[1]);
                dppAcceleroGraph.addXleroEntry(x,y);
            }else if(BluetoothLeService.ACTION_DISCARD_COUNT.equals(action)){
                long discardCount=intent.getLongExtra(BluetoothLeService.EXTRA_DATA, 0);
                tv_discardCount.setText("Junk discarded: ~" + discardCount);
            }

        }
    };

    ArrayList<BluetoothGattCharacteristic> notificationChars=null;

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        notificationChars = new ArrayList<BluetoothGattCharacteristic>(3);

        for (BluetoothGattService gattService : gattServices) {
            String uuid = gattService.getUuid().toString();
            debug("service="+uuid + ":" + SampleGattAttributes.lookup(uuid, uuid));
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                String charUuid=gattCharacteristic.getUuid().toString();
                debug("char="+charUuid + ":" + SampleGattAttributes.lookup(charUuid, charUuid));
                if(uuid.compareToIgnoreCase(SampleGattAttributes.SENSOR_DATA_SERVICE)==0){
                    if(charUuid.equals(SampleGattAttributes.SENSOR_DATA_CO2)
                            ||charUuid.equals(SampleGattAttributes.SENSOR_DATA_CO)
                            ||charUuid.equals(SampleGattAttributes.SENSOR_DATA_TEMPERATURE)
                            ||charUuid.equals(SampleGattAttributes.SENSOR_DATA_ACCE)){
                        //debug("yes listen for co2 here");
                        //listenToCharacteristic(gattCharacteristic);
                        notificationChars.add(gattCharacteristic);
                    }
                }
            }

        }

    }

    private void listenToCharacteristic(BluetoothGattCharacteristic characteristic) {
        final int charaProp = characteristic.getProperties();


        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mBluetoothLeService.setCharacteristicNotification(
                    characteristic, true);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_CO2);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_CO);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_TEMP);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_ACC);

        intentFilter.addAction(BluetoothLeService.ACTION_DISCARD_COUNT);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_ALL);

        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(btDevice.getAddress());
            debug("Connect request result=" + result);
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
        if(mBluetoothLeService!=null){
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }

    }

}
