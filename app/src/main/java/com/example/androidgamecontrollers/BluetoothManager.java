package com.example.androidgamecontrollers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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
    private final UUID HC_05_UUID;
    private final int REQUEST_ENABLE_BT = 0;

    private Context context;
    private Activity activity;
    private BluetoothAdapter btAdapter;
    private ImageButton btButton;

    private boolean isBtConnected;

    private PopupWindow btDevicesListPopup;
    private View btPopupView;
    private LinearLayout bt_available_list_ll;

    private ArrayList<BluetoothDevice> discoveredBtDevices;

    private BluetoothDevice selectedBtDevice;
    private ConnectedThread btConnectedThread;
    private ConnectThread btConnectingThread;
    private String btDeviceName;
    private IntentFilter btIntentFilter;

    private interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 0;
    }

    public BluetoothManager(Context context, ImageButton btButton) {
        this.context = context;
        this.activity = (Activity) context;
        this.btButton = btButton;
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        //Initialize constants
        APP_NAME = context.getResources().getString(R.string.app_name);
        APP_UUID = UUID.fromString(context.getResources().getString(R.string.app_uuid));
        HC_05_UUID = UUID.fromString(context.getResources().getString(R.string.hc_05_uuid));

        //Update bluetooth state UI
        isBtConnected = getBtConnectedState();
        updateBtUIState();

        setBtButtonOnClick();

        //Create popup windows for bluetooth devices list
        btDevicesListPopup = createBtDevicesPopupWindow();

        //Initialize discovered devices set
        discoveredBtDevices = new ArrayList<>();

        btIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        btIntentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        btIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        btIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
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
                            disconnectBtDevice();
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
                            btConnectedThread = null;
                            updateBtUIState(context.getString(R.string.bt_disconnected_text));
                            break;
                    }
                    break;
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    isBtConnected = true;
                    updateBtUIState(context.getString(R.string.bt_connected_text));
                    showToast("Connected to " + btDeviceName);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    isBtConnected = false;
                    updateBtUIState(context.getString(R.string.bt_disconnected_text));
                    showToast("Disconnected from " + btDeviceName);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    addDiscoveredBtDevice((BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    switch (state) {
                        case BluetoothDevice.BOND_BONDED:
                            showToast("Successfully paired with " + btDeviceName);
                            connectBtDevice();
                            break;
                    }
                    break;
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            /*switch (msg.what) {
                case MessageConstants.MESSAGE_WRITE:
                    String writtenMsg = new String((byte[])msg.obj);
                    showToast("Message sent: " + writtenMsg);
                    break;
            }*/
        }
    };

    public BroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }

    public IntentFilter getBtIntentFilter() {
        return btIntentFilter;
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

    private void connectBtDevice() {
        if(btConnectingThread != null) {
            btConnectingThread.cancel();
        }

        disconnectBtDevice();
        updateBtUIState();

        btConnectingThread = new ConnectThread(selectedBtDevice);
        btConnectingThread.start();
        showToast("Connecting to " + btDeviceName + "...");
    }

    private void disconnectBtDevice() {
        if(btConnectedThread != null) {
            btConnectedThread.cancel();
            btConnectedThread = null;
            isBtConnected = false;
        }
    }

    private void manageConnectedBtSocket(BluetoothSocket btSocket) {
        btConnectedThread = new ConnectedThread(btSocket);
    }

    private Button createBtDeviceBtn(final String btnDeviceName, final BluetoothDevice btDevice) {
        Button btDeviceBtn = new Button(context);
        btDeviceBtn.setText(btnDeviceName);

        btDeviceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btDeviceName = btDevice.getName() != null?
                        btDevice.getName() : btDevice.getAddress();

                //Pair bluetooth
                if(btDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        btDevice.createBond();
                        selectedBtDevice = btDevice;
                    }
                } else {
                    selectedBtDevice = btDevice;
                    connectBtDevice();
                }

                //Dismiss the pop up window
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

    public void write(byte[] data) {
        if(btConnectedThread != null) {
            btConnectedThread.write(data);
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice btDevice;
        private final BluetoothSocket btSocket;

        public ConnectThread(BluetoothDevice btDevice) {
            this.btDevice = btDevice;
            BluetoothSocket temp = null;

            try {
                temp = btDevice.createRfcommSocketToServiceRecord(HC_05_UUID);
            } catch (IOException e) {
                Log.e("Bluetooth Socket", "Failed to create socket.", e);
            }

            btSocket = temp;
        }

        public void run() {
            btAdapter.cancelDiscovery();

            //Connect to remote device through the socket.
            //connect() function call blocks until it succeeds or throw exception
            try {
                btSocket.connect();
            } catch (IOException connectException) {
                Log.e("Socket connect", "Unable to connect to bluetooth socket.", connectException);
                try {
                    btSocket.close();
                } catch (IOException closeException) {
                    Log.e("Socket close", "Unable to close bluetooth socket.", closeException);
                }

                return;
            }

            //Manage the connected socket
            manageConnectedBtSocket(btSocket);
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
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private byte[] streamBuffer;

        public ConnectedThread(BluetoothSocket btSocket) {
            this.btSocket = btSocket;
            InputStream tempInputStream = null;
            OutputStream tempOutputStream = null;

            try {
                tempInputStream = btSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                tempOutputStream = btSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempInputStream;
            outputStream = tempOutputStream;
        }

        public void run() {
            streamBuffer = new byte[1024];
            int numBytes; //bytes returned from read()

            //Listens to input stream until an exception occurs
            while (btSocket.isConnected()) {
                try {
                    numBytes = inputStream.read(streamBuffer);

                    Message readMsg = handler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1, streamBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.e("InputStream Error: ", "Input stream disconnected.", e);
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);

                Message writtenMsg = handler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, bytes);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e("OutputStream Error: ", "Error occured when sending data.", e);
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
