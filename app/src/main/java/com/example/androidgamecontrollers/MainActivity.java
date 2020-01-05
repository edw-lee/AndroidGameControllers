package com.example.androidgamecontrollers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.widget.ImageButton;

public class MainActivity extends Activity {

    BluetoothManager btManager;
    JoystickManager joystickManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConstraintLayout main_layout = findViewById(R.id.main_layout);
        if(main_layout == null) {
            Log.w("LAYOUT", "Main layout is null");
        }

        ImageButton btButton = findViewById(R.id.bluetooth_btn);
        btManager = new BluetoothManager(this, btButton);

        joystickManager = new JoystickManager(this, btManager);
        main_layout.addView(joystickManager, 0);

        registerReceiver(btManager.getBroadcastReceiver(), btManager.getBtIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(btManager.getBroadcastReceiver());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        btManager.onActivityResult(requestCode, resultCode, data);
    }
}
