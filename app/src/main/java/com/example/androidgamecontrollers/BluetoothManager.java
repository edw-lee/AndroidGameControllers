package com.example.androidgamecontrollers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager implements PopupMenu.OnMenuItemClickListener {

    private final String APP_NAME;
    private final UUID APP_UUID;
    private final int REQUEST_ENABLE_BT = 0;

    private Context context;
    private Activity activity;
    private BluetoothAdapter btAdapter;
    private ImageButton btButton;

    private boolean isBtConnected = false;

    private PopupWindow btDevicesListPopup;
    private View btPopupView;
    private LinearLayout bt_available_list_ll;

    private ArrayList<BluetoothDevice> discoveredBtDevices;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
    }

    public BluetoothManager(Context context, ImageButton btButton) {
        this.context = context;
        this.activity = (Activity) context;
        this.btButton = btButton;
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        //Initialize constants
        APP_NAME = context.getResources().getString(R.string.app_name);
        APP_UUID = UUID.fromString(context.getResources().getString(R.string.app_uuid));

        //Update bluetooth state UI
        isBtConnected = getBtConnectedState();
        updateBtUIState();

        setBtButtonOnClick();

        //Create popup windows for bluetooth devices list
        btDevicesListPopup = createBtDevicesPopupWindow();

        //Initialize discovered devices set
        discoveredBtDevices = new ArrayList<>();
    }

    //To check for bluetooth state. eg, when changes is done externally
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int state;

            assert action != null;
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            updateBtUIState(context.getString(R.string.bt_off_text));
                            break;
                        case BluetoothAdapter.STATE_ON:
                            updateBtUIState(context.getString(R.string.bt_on_text));
                            showBtDevices();
                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                    state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED);
                    switch (state) {
                        case BluetoothAdapter.STATE_CONNECTED:
                            isBtConnected = true;
                            updateBtUIState(context.getString(R.string.bt_connected_text));
                            break;
                        case BluetoothAdapter.STATE_DISCONNECTED:
                            isBtConnected = false;

                            updateBtUIState(context.getString(R.string.bt_disconnected_text));
                            break;
                    }
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    addDiscoveredBtDevice((BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    showToast("Bluetooth discovery started");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    showToast("Bluetooth discovery ended");
                    break;
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler btHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

        }
    }; //To get info from Bluetooth Service


    public BroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }

    private void updateBtUIState() {
        updateBtUIState("");
    }

    private void updateBtUIState(String message) {
        if (getBtAvailability()) {
            if (btAdapter.isEnabled()) {
                if (isBtConnected) {
                    btButton.setImageResource(R.drawable.ic_bluetooth_connected);
                } else {
                    btButton.setImageResource(R.drawable.ic_bluetooth_on);
                }
            } else {
                btButton.setImageResource(R.drawable.ic_bluetooth_off);
            }
        } else {
            btButton.setImageResource(R.drawable.ic_bluetooth_disabled);
        }

        if (message.length() > 0) {
            showToast(message);
        }
    }

    private void setBtButtonOnClick() {
        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBtAvailability()) {
                    toggleBluetooth();
                } else {
                    showToast(context.getString(R.string.bt_unavailable_text));
                }
            }
        });

        btButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showBtMenu(v);
                return true;
            }
        });
    }

    private void showBtMenu(View v) {
        if (btAdapter == null) {
            showToast(context.getString(R.string.bt_unavailable_text));
            return;
        } else {
            if (!btAdapter.isEnabled()) {
                turnOnBluetooth();
                return;
            }
        }

        PopupMenu btPopUpMenu = new PopupMenu(v.getContext(), v);
        btPopUpMenu.setOnMenuItemClickListener(this);
        btPopUpMenu.inflate(R.menu.bluetooth_menu);
        MenuItem btConnectItem = btPopUpMenu.getMenu().findItem(R.id.bt_menu_connect);

        if (isBtConnected) {
            btConnectItem.setTitle(R.string.bt_change_device_text);
        } else {
            btConnectItem.setTitle(R.string.bt_connect_device_text);
        }

        btPopUpMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bt_menu_turn_off:
                turnOffBluetooth();
                return true;
            case R.id.bt_menu_connect:
                showBtDevices();
                return true;
            default:
                return false;
        }
    }

    private boolean getBtAvailability() {
        return btAdapter != null;
    }

    private boolean getBtConnectedState() {
        int[] profiles = {BluetoothProfile.A2DP, BluetoothProfile.HEADSET, BluetoothProfile.HEALTH};

        for (int profile : profiles) {
            if (btAdapter.getProfileConnectionState(profile) == BluetoothProfile.STATE_CONNECTED) {
                return true;
            }
        }

        return false;
    }

    @SuppressLint("InflateParams")
    private PopupWindow createBtDevicesPopupWindow() {
        //Create view object
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        btPopupView = inflater.inflate(R.layout.bluetooth_list, null);

        //Specify length and width
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;

        //Make items outside of popup inactive
        boolean focusable = true;

        //Crete a popup window
        PopupWindow popupWindow = new PopupWindow(btPopupView, width, height, focusable);

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                btAdapter.cancelDiscovery();
            }
        });

        return popupWindow;
    }

    private void showBtDevices() {
        //Get bluetooth list linear layouts
        LinearLayout bt_paired_list_ll = btDevicesListPopup.getContentView().findViewById(R.id.bt_paired_scroll_list);

        if(bt_available_list_ll == null) {
            bt_available_list_ll = btDevicesListPopup.getContentView().findViewById(R.id.bt_avaliable_scroll_list);
        }

        //Clear linear layouts
        bt_paired_list_ll.removeAllViews();
        bt_available_list_ll.removeAllViews();

        //Get paired devices
        Set<BluetoothDevice> bondedDevices = btAdapter.getBondedDevices();

        //Clear previously discovered devices
        discoveredBtDevices.clear();
        TextView noAvailableDeviceFoundTV = new TextView(context);
        noAvailableDeviceFoundTV.setText(context.getString(R.string.bt_no_available_text));
        bt_available_list_ll.addView(noAvailableDeviceFoundTV);

        //Get available devices
        //Attempt to discover available devices
        btAdapter.startDiscovery();

        if (bondedDevices.size() > 0) {
            for(BluetoothDevice device : bondedDevices) {
                Button btDeviceBtn = createBtDeviceBtn(device.getName(), device);
                bt_paired_list_ll.addView(btDeviceBtn);
            }
        } else {
            TextView noPairedDeviceTV = new TextView(context);
            noPairedDeviceTV.setText(context.getString(R.string.bt_no_paired_text));
            bt_paired_list_ll.addView(noPairedDeviceTV);
        }

        btDevicesListPopup.showAtLocation(btPopupView, Gravity.CENTER, 0, 0);
    }

    private void addDiscoveredBtDevice(BluetoothDevice btDevice) {
        if(discoveredBtDevices.contains(btDevice)) {
            return;
        }

        //Remove "No available device found." text
        if(discoveredBtDevices.size() == 0) {
            bt_available_list_ll.removeViewAt(0);
        }

        String btDeviceBtnName;
        if(btDevice.getName() != null && btDevice.getName().length() > 0) {
            btDeviceBtnName = btDevice.getName();
        } else {
            btDeviceBtnName = btDevice.getAddress();
        }

        Button btDeviceBtn = createBtDeviceBtn(btDeviceBtnName, btDevice);

        bt_available_list_ll.addView(btDeviceBtn);
        discoveredBtDevices.add(btDevice);
    }

    private Button createBtDeviceBtn(final String btnDeviceName, final BluetoothDevice btDevice) {
        Button btDeviceBtn = new Button(context);
        btDeviceBtn.setText(btnDeviceName);

        btDeviceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectThread connectThread = new ConnectThread(btDevice);
                connectThread.start();

                btDevicesListPopup.dismiss();
            }
        });

        return btDeviceBtn;
    }

    private void toggleBluetooth() {
        if (btAdapter.isEnabled()) {
            turnOffBluetooth();
        } else {
            turnOnBluetooth();
        }
    }

    private void turnOnBluetooth() {
        if (!btAdapter.isEnabled()) {
            Intent turnOnBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(turnOnBtIntent, REQUEST_ENABLE_BT);
        } else {
            showToast("Bluetooth is already on");
        }
    }

    private void turnOffBluetooth() {
        if (btAdapter.isEnabled()) {
            btAdapter.disable();
        } else {
            showToast("Bluetooth is already off");
        }
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT: {
                if (resultCode != Activity.RESULT_OK) {
                    showToast("Permission to turn on bluetooth denied");
                }
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        ConnectedThread connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    //Act as a server that waits for connection to be made
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket btServerSocket;

        public AcceptThread(String name, UUID uuid) {
            //Initialize temp to assign to btServerSocket later because it is a final variable
            BluetoothServerSocket temp = null;
            try {
                temp = btAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            btServerSocket = temp;
        }

        @Override
        public void run() {
            super.run();

            BluetoothSocket btSocket = null;

            while(true) {
                //Listens until socket is returned
                try {
                    btSocket = btServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                if(btSocket != null) {
                    //Manage connected socket in another thread
                    manageConnectedSocket(btSocket);

                    try {
                        btServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                btServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Connect as a client
    private class ConnectThread extends Thread {
        private final BluetoothSocket btSocket;

        private ConnectThread(BluetoothDevice btDevice) {

            //Use temp because btSocket is final
            BluetoothSocket temp = null;

            try {
                temp = btDevice.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            btSocket = temp;
        }

        @Override
        public void run() {
            super.run();

            //Cancel discovery to prevent connection slow down
            if(btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
            }

            try {
                btSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();

                //Unable to connect, close it
                try {
                    btSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                return;
            }

            //Manage connected socket in another thread
            manageConnectedSocket(btSocket);
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket btSocket;
        private final InputStream btInStream;
        private final OutputStream btOutStream;
        private byte[] buffer; //Buffer to store stream

        public ConnectedThread(BluetoothSocket btSocket) {
            this.btSocket = btSocket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = btSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                tempOut = btSocket.getOutputStream();
            }catch (IOException e) {
                e.printStackTrace();
            }

            btInStream = tempIn;
            btOutStream = tempOut;
        }

        public void run() {
            buffer = new byte[1024];
            int numBytes; //bytes returned from read();

            while(true) {
                try {
                    //Read from input stream
                    numBytes = btInStream.read(buffer);

                    //Send the obtained bytes to UI activity
                    Message readMsg = btHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1, buffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        //Call this function to send data to remote device
        public void write(byte[] bytes) {
            try {
                btOutStream.write(bytes);

                //Share the sent message with the UI activity
                Message writtenMsg = btHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, buffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();

                //Send failure message back to activity
                Message writeErrorMsg = btHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Unable to send data to the other device");
                writeErrorMsg.setData(bundle);
                btHandler.sendMessage(writeErrorMsg);
            }
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
