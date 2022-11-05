package com.example.myapplication;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static Boolean bluetoothActive = false;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();

    Button connectButton;
    Button disconnectButton;
    Button forwardButton;
    Button backwardButton;
    Button leftButton;
    Button rightButton;

    TextView statusLabel;

    BluetoothAdapter bluetooth;

    Socket btSocket;
    InputStream inputStream;
    OutputStream outputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectButton = (Button) findViewById(R.id.connectButton);
        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        forwardButton = (Button) findViewById(R.id.forwardButton);
        backwardButton= (Button) findViewById(R.id.backwardButton);
        leftButton= (Button) findViewById(R.id.leftButton);
        rightButton= (Button) findViewById(R.id.rightButton);

        statusLabel = (TextView) findViewById(R.id.statusLabel);

        //BLUETOOTH CONFIG
        bluetooth = BluetoothAdapter.getDefaultAdapter();


    }

    public void onClickConnect(View view) {
        statusLabel.setText("Connect pressed");

        if (bluetooth.isEnabled()) {
            bluetoothActive = true;
            String address = bluetooth.getAddress();
            String name = bluetooth.getName(); //Mostramos la datos en pantalla (The information is shown in the screen)
            Toast.makeText(getApplicationContext(), "Bluetooth ENABLED:" + name + ":" + address, Toast.LENGTH_SHORT).show();
            startDiscovery();
        } else {
            bluetoothActive = false;
            //Toast.makeText(getApplicationContext(),"Bluetooth NOT enabled", Toast.LENGTH_SHORT).show();
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) //Bluetooth permission request code
            if (resultCode == RESULT_OK) {
                bluetoothActive = true;
                Toast.makeText(getApplicationContext(), "User Enabled Bluetooth", Toast.LENGTH_SHORT).show();
            } else {
                bluetoothActive = false;
                Toast.makeText(getApplicationContext(), "User Did not enable Bluetooth", Toast.LENGTH_SHORT).show();
            }
    }

    private void startDiscovery() {
        if (bluetoothActive) { //We delete the previous device list
            deviceList.clear();
            //Activate an Android Intent to notify when a device is found
            // NOTE: <<discoveryResult>> is a << callback >> class that we will describe in the next step
            registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            //We put the bluetooth adapter in <<Discovery>> mode
            checkBTPermissions();
            bluetooth.startDiscovery();
        }
    }

    BroadcastReceiver discoveryResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Guardamos el nombre del dispositivo descubierto
            String remoteDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            //Guardamos el objeto Java del dispositivo descubierto, para poder conectar.
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //Leemos la intensidad de la radio con respecto a este dispositivo bluetooth
            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
            //Guardamos el dispositivo encontrado en la lista
            deviceList.add(remoteDevice);
            //Mostramos el evento en el Log.
            Log.d("MyFirstApp", "Discovered " + remoteDeviceName);
            Log.d("MyFirstApp", "RSSI " + rssi + "dBm");

            if (remoteDeviceName.equals("SUM_SCH3")) {
                Log.d("onReceive", "Discovered SUM_SCH3:connecting");
                connect(remoteDevice);
            }
        }
    };

    public void checkBTPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
                    }
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    break;
            }
        }
    }

    protected void connect(BluetoothDevice device) {
        try {
            btSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btSocket.connect();
            Log.d("connect", "Client connected");
            inputStream = btSocket.getInputStream();
            outputStream = btSocket.getOutputStream();
        }catch (Exception e) {
            Log.e("ERROR: connect", ">>", e);
        }
    }

    public void forward() {
        try {
            String tmpStr = "WHEELS+70+110";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
        } catch (Exception e) {
            Log.e("forward", "ERROR:" + e);
        }
    }
}