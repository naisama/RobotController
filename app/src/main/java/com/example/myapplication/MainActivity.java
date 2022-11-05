package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //TODO indicar a que conectarse
    private final String bluetoothDevice = "Kami Beats Solo³";
            //"ROBOTIS_210_D4";
    private BluetoothDevice robotis = null;

    public static Boolean bluetoothActive = false;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();

    //General
    TextView statusLabel;


    //Actions
    Button connectButton;
    Button disconnectButton;

    Button forwardButton;
    Button backwardButton;
    Button leftButton;
    Button rightButton;

    Button stopButton;


    //Speed
    Button increaseSpeed;
    Button decreaseSpeed;
    TextView speedLabel;
    private int speed;


    //Bluetooth
    BluetoothAdapter bluetooth;
    BluetoothSocket btSocket;
    private InputStream inputStream;
    private OutputStream outputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectButton = (Button) findViewById(R.id.connectButton);
        disconnectButton = (Button) findViewById(R.id.disconnectButton);

        forwardButton = (Button) findViewById(R.id.forwardButton);
        backwardButton = (Button) findViewById(R.id.backwardButton);
        leftButton = (Button) findViewById(R.id.leftButton);
        rightButton = (Button) findViewById(R.id.rightButton);

        stopButton = (Button) findViewById(R.id.stopButton);

        speedLabel = (TextView) findViewById(R.id.speedText);
        increaseSpeed = (Button) findViewById(R.id.incSpeedButton);
        decreaseSpeed = (Button) findViewById(R.id.decSpeedButton);

        statusLabel = (TextView) findViewById(R.id.statusLabel);

        //BLUETOOTH CONFIG
        bluetooth = BluetoothAdapter.getDefaultAdapter();

        //Action listeners association
        disconnectButton.setOnClickListener(v -> disconnect());
        forwardButton.setOnClickListener(v -> forward());
        backwardButton.setOnClickListener(v -> backward());
        leftButton.setOnClickListener(v -> moveLeft());
        rightButton.setOnClickListener(v -> moveRight());

        stopButton.setOnClickListener(v -> stop());

        increaseSpeed.setOnClickListener(v -> increaseSpeed());
        decreaseSpeed.setOnClickListener(v -> decreaseSpeed());

        //Initial button status
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);


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

            if (remoteDeviceName != null && remoteDeviceName.equals(bluetoothDevice)) { //TODO comprobar que funciona
                Log.d("MyFirstApp", "Discovered " + bluetoothDevice + ":connecting");
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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            btSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btSocket.connect();
            Log.d("MyFirstApp", "Client connected");
            inputStream = btSocket.getInputStream();
            outputStream = btSocket.getOutputStream();

            statusLabel.setText(String.format("Connected to %s successfully.", bluetoothDevice));
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);

        }catch (Exception e) {
            Log.e("ERROR: connect", ">>", e);
        }
    }

    protected void disconnect() {
        statusLabel.setText("Disconnect pressed");
        if (bluetooth != null && bluetooth.isEnabled()) {
            if (btSocket != null && btSocket.isConnected()) {
                try {
                    btSocket.close();
                    connectButton.setEnabled(true);
                    disconnectButton.setEnabled(false);
                    statusLabel.setText(String.format("Disconnected succesfully from %s", bluetoothDevice));
                    Log.d("MyFirstApp", "Client disconnected");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Movement format
    /*
    Example= "w\n" + speed + "\n"
    w = Forward
    s = Backwards
    q = MoveLeft
    a = Left
    e= MoveRight
    d = Right
    Speed = [1-9]
    Stop = 0*/


    public void forward() {
        try {
            speed = Integer.parseInt(speedLabel.getText().toString());
            String tmpStr = "w\n" + speed + "\n";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
            Log.d("MyFirstApp", "Forward sent");
            statusLabel.setText("Moving forward at speed " + speed);
        } catch (Exception e) {
            Log.e("MyFirstApp", "FORWARD ERROR:" + e);
        }
    }

    public void backward() {
        try {
            speed = Integer.parseInt(speedLabel.getText().toString());
            String tmpStr = "s\n" + speed + "\n";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
            Log.d("MyFirstApp", "Backward sent");
            statusLabel.setText("Moving backwards at speed " + speed);
        } catch (Exception e) {
            Log.e("MyFirstApp", "BACKWARD ERROR:" + e);
        }
    }

    public void moveLeft() {
        try {
            speed = Integer.parseInt(speedLabel.getText().toString());
            String tmpStr = "q\n" + speed + "\n";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
            Log.d("MyFirstApp", "Move left sent");
            statusLabel.setText("Moving left at speed " + speed);
        } catch (Exception e) {
            Log.e("MyFirstApp", "MOVE LEFT ERROR:" + e);
        }
    }

    public void left() {
        try {
            speed = Integer.parseInt(speedLabel.getText().toString());
            String tmpStr = "a\n" + speed + "\n";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
            Log.d("MyFirstApp", "Left sent");
            statusLabel.setText("Left at speed " + speed);
        } catch (Exception e) {
            Log.e("MyFirstApp", "LEFT ERROR:" + e);
        }
    }

    public void moveRight() {
        try {
            speed = Integer.parseInt(speedLabel.getText().toString());
            String tmpStr = "e\n" + speed + "\n";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
            Log.d("MyFirstApp", "Move right sent");
            statusLabel.setText("Moving right at speed " + speed);
        } catch (Exception e) {
            Log.e("MyFirstApp", "MOVE RIGHT ERROR:" + e);
        }
    }

    public void right() {
        try {
            speed = Integer.parseInt(speedLabel.getText().toString());
            String tmpStr = "d\n" + speed + "\n";
            byte bytes[] = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
            Log.d("MyFirstApp", "Right sent");
            statusLabel.setText("Right at speed " + speed);
        } catch (Exception e) {
            Log.e("MyFirstApp", "RIGHT ERROR:" + e);
        }
    }

    public void stop() {
        try {
            String tmpStr = "w\n0\n";
            byte[] bytes = tmpStr.getBytes();
            if (outputStream != null) outputStream.write(bytes);
            if (outputStream != null) outputStream.flush();
            Log.d("MyFirstApp", "Stop sent");
            statusLabel.setText("Stopping robot");

        } catch (Exception e) {
            Log.e("MyFirstApp", "STOP ERROR:" + e);
        }
    }

    public void increaseSpeed(){
        String currentSpeed = speedLabel.getText().toString();
        speed = Integer.parseInt(currentSpeed);

        if (speed < 9) speed++;
        else {
            Log.d("MyFirstApp", "Maximum speed reached");
            Toast.makeText(getApplicationContext(), "Maximum speed reached", Toast.LENGTH_SHORT).show();
        }

        speedLabel.setText(String.valueOf(speed));

    }

    public void decreaseSpeed(){
        String currentSpeed = speedLabel.getText().toString();
        speed = Integer.parseInt(currentSpeed);

        if (speed > 1) speed--;
        else {
            Log.d("MyFirstApp", "Minimum speed reached");
            Toast.makeText(getApplicationContext(), "Minimun speed reached", Toast.LENGTH_SHORT).show();
        }

        speedLabel.setText(String.valueOf(speed));

    }



}