package com.example.androidgamecontrollers;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

public class BluetoothManager implements PopupMenu.OnMenuItemClickListener {

    private final int REQUEST_ENABLE_BT = 0;

    private Context context;
    private Activity activity;
    private BluetoothAdapter bluetoothAdapter;
    private ImageButton btButton;

    private BroadcastReceiver broadcastReceiver;

    public BluetoothManager(Context context, ImageButton btButton) {
        this.context = context;
        this.activity = (Activity) context;
        this.btButton = btButton;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(getBluetoothAvailability()) {
            updateBtStateUI();
        } else {
            btButton.setImageResource(R.drawable.ic_bluetooth_disabled);
        }

        //To check for bluetooth state. eg, when changes is done externally
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            updateBtStateUI();
                            showToast(context.getString(R.string.bt_off_text));
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            //showToast("Turning off bluetooth...");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            updateBtStateUI();
                            showToast(context.getString(R.string.bt_on_text));
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            //showToast("Turning on bluetooth...");
                            break;
                    }
                }
            }
        };

        setBtButtonOnClick();
    }

    public BroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }

    public void updateBtStateUI(){
        if(bluetoothAdapter.isEnabled()) {
            btButton.setImageResource(R.drawable.ic_bluetooth_on);
        } else {
            btButton.setImageResource(R.drawable.ic_bluetooth_off);
        }
    }

    public void setBtButtonOnClick() {
        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getBluetoothAvailability()) {
                    toggleBluetooth();
                } else {
                    showToast(context.getString(R.string.bt_unavailable_text));
                }
            }
        });

        btButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showBluetoothMenu(v);
                return true;
            }
        });
    }

    public void showBluetoothMenu(View v) {
        if(bluetoothAdapter == null) {
            showToast(context.getString(R.string.bt_unavailable_text));
            return;
        } else  {
            if(!bluetoothAdapter.isEnabled()) {
                turnOnBluetooth();
                return;
            }
        }

        PopupMenu btPopUpMenu = new PopupMenu(v.getContext(), v);
        btPopUpMenu.setOnMenuItemClickListener(this);
        btPopUpMenu.inflate(R.menu.bluetooth_menu);
        btPopUpMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bt_turn_off:
                turnOffBluetooth();
                return true;
            case R.id.bt_change_paired:
                //displayBluetoothDevices();
                return true;
            default:
                return false;
        }
    }

    public boolean getBluetoothAvailability() {
        return bluetoothAdapter != null;
    }

    public void toggleBluetooth() {
        if(bluetoothAdapter.isEnabled()) {
            turnOffBluetooth();
        } else {
            turnOnBluetooth();
        }
    }

    public void turnOnBluetooth() {
        if(!bluetoothAdapter.isEnabled()) {
            Intent turnOnBtIntent = new Intent(bluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(turnOnBtIntent, REQUEST_ENABLE_BT);
        } else {
            showToast("Bluetooth is already on");
        }
    }

    public void turnOffBluetooth() {
        if(bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            updateBtStateUI();
        } else {
            showToast("Bluetooth is already off");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT: {
                if(resultCode != activity.RESULT_OK) {
                    showToast("Permission to turn on bluetooth denied");
                }
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
