package com.berry_med.monitordemo.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.berry_med.monitordemo.R;
import com.berry_med.monitordemo.bluetooth.BTController;
import com.berry_med.monitordemo.data.DataParser;
import com.berry_med.monitordemo.data.ECG;
import com.berry_med.monitordemo.data.MyDatabaseHelper;
import com.berry_med.monitordemo.data.NIBP;
import com.berry_med.monitordemo.data.SpO2;
import com.berry_med.monitordemo.data.Temp;
import com.berry_med.monitordemo.dialog.BluetoothDeviceAdapter;
import com.berry_med.monitordemo.dialog.SearchDevicesDialog;
import com.berry_med.monitordemo.view.WaveformView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class Main2Activity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener ,BTController.Listener, DataParser.onPackageReceivedListener{

    private BTController mBtController;

    //UI
    private Button btnBtCtr;
    //private TextView tvBtinfo;
    private TextView tvECGinfo;
    private TextView tvSPO2info;
    private TextView tvTEMPinfo;
    private TextView tvNIBPinfo;
    private TextView tvFWVersion;
    private TextView tvHWVersion;
    private WaveformView wfSpO2;
    private WaveformView wfECG;


    //Bluetooth
    BluetoothDeviceAdapter mBluetoothDeviceAdapter;
    SearchDevicesDialog mSearchDialog;
    ProgressDialog mConnectingDialog;
    ArrayList<BluetoothDevice> mBluetoothDevices;

    //data
    DataParser mDataParser;
    MyDatabaseHelper dbHelper;
    SQLiteDatabase db;
    String iuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        myRequetPermission();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        dbHelper=new MyDatabaseHelper(
                this,
                "monitordemo.db",
                null,
                2
        );
        db=dbHelper.getWritableDatabase();
        iuid=getUUID();
        initData();
        initView();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter BA=BluetoothAdapter.getDefaultAdapter ();
                BA.startDiscovery();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                BluetoothDevice device=BA.getRemoteDevice("8C:DE:52:C5:A3:37");
                BA.cancelDiscovery();
                mBtController.connect(Main2Activity.this, device);
            }
        }).start();

    }

    private void myRequetPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }else {
            Toast.makeText(this,"您已经申请了权限!", Toast.LENGTH_SHORT).show();
        }
    }
    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();
        String uuidStr = str.replace("-", "");
        return uuidStr;
    }

    private void initData() {
        // enable the Bluetooth Adapter
        mBtController = BTController.getDefaultBTController(this);
        mBtController.registerBroadcastReceiver(this);
        mBtController.enableBtAdpter();

        mDataParser = new DataParser(this);
        mDataParser.start();
    }
    private void initView() {
        //UI widgets
        btnBtCtr  = new Button(this);
        btnBtCtr.setId(R.id.btnBtCtr);
        //tvBtinfo = (TextView) findViewById(R.id.tvbtinfo);
        tvECGinfo = (TextView) findViewById(R.id.tvECGinfo);
        tvSPO2info = (TextView) findViewById(R.id.tvSPO2info);
        tvTEMPinfo = (TextView) findViewById(R.id.tvTEMPinfo);
        tvNIBPinfo = (TextView) findViewById(R.id.tvNIBPinfo);
        tvFWVersion = (TextView) findViewById(R.id.tvFWverison);
        tvHWVersion = (TextView) findViewById(R.id.tvHWverison);

        //Bluetooth Search Dialog
        mBluetoothDevices = new ArrayList<>();
        mBluetoothDeviceAdapter = new BluetoothDeviceAdapter(Main2Activity.this,mBluetoothDevices);
        mSearchDialog = new SearchDevicesDialog(Main2Activity.this,mBluetoothDeviceAdapter) {
            @Override
            public void onStartSearch() {
                mBtController.startScan(true);
            }

            @Override
            public void onClickDeviceItem(int pos) {
                BluetoothDevice device = mBluetoothDevices.get(pos);
                mBtController.startScan(false);
                mBtController.connect(Main2Activity.this,device);
               // tvBtinfo.setText(device.getName() +": " + device.getAddress());
                mConnectingDialog.show();
                mSearchDialog.dismiss();
            }
        };
        mSearchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mBtController.startScan(false);
            }
        });

        mConnectingDialog = new ProgressDialog(Main2Activity.this);
        mConnectingDialog.setMessage("Connecting...");


        //SpO2 & ECG waveform
        wfSpO2 = (WaveformView) findViewById(R.id.wfSpO2);
        wfECG = (WaveformView) findViewById(R.id.wfECG);

    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.btnBtCtr:
                if(!mBtController.isBTConnected()){
                    mSearchDialog.show();
                    mSearchDialog.startSearch();
                    mBtController.startScan(true);
                }
                else {
                    mBtController.disconnect();
                    mSearchDialog.show();
                    mSearchDialog.startSearch();
                    mBtController.startScan(true);
                  //  tvBtinfo.setText("");
                }
                break;
            case R.id.btnNIBPStart:
                mBtController.write(DataParser.CMD_START_NIBP);
                break;
            case R.id.btnNIBPStop:
                mBtController.write(DataParser.CMD_STOP_NIBP);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        mBtController.disconnect();
        super.onDestroy();
        finish();
        System.exit(0); //for release "mBluetoothDevices" on key_back down
        mBtController.unregisterBroadcastReceiver(this);
    }


    //BTController implements
    @Override
    public void onFoundDevice(BluetoothDevice device) {
        if(mBluetoothDevices.contains(device))
            return;
        mBluetoothDevices.add(device);
        mBluetoothDeviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStopScan() {
        mSearchDialog.stopSearch();
    }

    @Override
    public void onStartScan() {
        mBluetoothDevices.clear();
        mBluetoothDeviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onConnected() {
        mConnectingDialog.setMessage("Connected √");
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mConnectingDialog.dismiss();
                    }
                });
            }
        },800);

        btnBtCtr.setText("Disconnect");
    }

    @Override
    public void onDisconnected() {
        btnBtCtr.setText("Search Devices");
    }

    @Override
    public void onReceiveData(byte[] dat) {
        mDataParser.add(dat);
    }


    long begintime = System.currentTimeMillis();


    long endtinme=System.currentTimeMillis();

    int count=0;
    int spo2count=0;
    int spo2wavecount=0;

    //DataParser implements
    @Override
    public void onSpO2WaveReceived(int dat) {

        wfSpO2.addAmp(dat);
        db.execSQL("INSERT INTO SPO2WAVE "
                        +"(Iid,number,data)"
                        +"VALUES(?,?,?)",
                new String[]{iuid, String.valueOf(spo2wavecount), String.valueOf(dat)}
        );
        spo2wavecount++;
    }

    @Override
    public void onSpO2Received(final SpO2 spo2) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvSPO2info.setText(spo2.toString());
            }
        });
    }

    @Override
    public void onECGWaveReceived(int dat) {
        endtinme=System.currentTimeMillis();
        if(endtinme - begintime>1000)
        {
            begintime = System.currentTimeMillis();
            Log.i("count",String.valueOf(count));
            count=0;
        }
        else{
            count++;
        }
        wfECG.addAmp(dat);
    }

    @Override
    public void onECGReceived(final ECG ecg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvECGinfo.setText(ecg.toString());
            }
        });
    }

    @Override
    public void onTempReceived(final Temp temp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvTEMPinfo.setText(temp.toString());
            }
        });
    }

    @Override
    public void onNIBPReceived(final NIBP nibp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvNIBPinfo.setText(nibp.toString());
            }
        });
    }


    @Override
    public void onFirmwareReceived(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvFWVersion.setText("Firmware Version:" +str);
            }
        });
    }

    @Override
    public void onHardwareReceived(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvHWVersion.setText("Hardware Version:" +str);
            }
        });
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            onClick(btnBtCtr);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_tools) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
