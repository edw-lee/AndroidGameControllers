package com.example.androidgamecontrollers;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.widget.ImageButton;

public class MainActivity extends Activity {

    BluetoothManager bluetoothManager;
    JoystickManager joystickManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConstraintLayout main_layout = findViewById(R.id.main_layout);
        if(main_layout == null) {
            Log.w("LAYOUT", "Main lyaout is null");
        }

        joystickManager = new JoystickManager(this);
        main_layout.addView(joystickManager, 0);

        ImageButton btButton = findViewById(R.id.bluetooth_btn);
        bluetoothManager = new BluetoothManager(this, btButton);

        IntentFilter btIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        btIntentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothManager.getBroadcastReceiver(), btIntentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothManager.getBroadcastReceiver());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bluetoothManager.onActivityResult(requestCode, resultCode, data);
    }
}
