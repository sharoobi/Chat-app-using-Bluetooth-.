package com.sharoobi.blechat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog; // Import AlertDialog
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface; // Import DialogInterface
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList; // Import ArrayList
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    // UI Elements
    private TextView mChatMessages;
    private EditText mMessageInput;
    private Button mStartServerButton;
    private Button mConnectButton;
    private Button mSendButton;
    private ScrollView mScrollView;

    // Bluetooth Adapter
    private BluetoothAdapter mBluetoothAdapter;

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SPP UUID
    private static final String APP_NAME = "BluetoothChat";

    // Message types for the Handler
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_TOAST = 3;
    public static final int MESSAGE_CONNECTED = 4;

    // Request codes for intents
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVERABLE_BT = 2;
    private static final int REQUEST_PERMISSION_LOCATION = 3;
    private static final int REQUEST_PERMISSION_BLUETOOTH_CONNECT = 4;
    private static final int REQUEST_PERMISSION_BLUETOOTH_SCAN = 5;
    private static final int REQUEST_PERMISSION_BLUETOOTH_ADVERTISE = 6;


    // Bluetooth Threads
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    // Handler to update the UI from background threads
    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // Construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    appendMessage("Received: " + readMessage);
                    break;
                case MESSAGE_WRITE:
                    // byte[] writeBuf = (byte[]) msg.obj;
                    // String writeMessage = new String(writeBuf);
                    // appendMessage("Sent: " + writeMessage); // Optional: show sent message in log
                    break;
                case MESSAGE_TOAST:
                    showToast(msg.getData().getString("toast"));
                    break;
                case MESSAGE_CONNECTED:
                    appendMessage("Connected to: " + msg.getData().getString("device_name"));
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        mChatMessages = findViewById(R.id.chatMessages);
        mMessageInput = findViewById(R.id.messageInput);
        mStartServerButton = findViewById(R.id.startServerButton);
        mConnectButton = findViewById(R.id.connectButton);
        mSendButton = findViewById(R.id.sendMessageButton);
        mScrollView = findViewById(R.id.scrollView);

        // Get Bluetooth Adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            showToast("Bluetooth is not available on this device.");
            finish(); // Close the app if Bluetooth is not supported
            return;
        }

        // Request necessary Bluetooth permissions for Android 12+
        requestBluetoothPermissions();

        // Request location permissions (needed for Bluetooth scanning on Android 6.0+)
        requestLocationPermissions();

        // Enable Bluetooth if it's not already enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Set up button listeners
        mStartServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServer();
            }
        });

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call the updated connectToDevice method to show selection
                connectToDevice();
            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mMessageInput.getText().toString();
                if (!message.isEmpty()) {
                    sendMessage(message);
                    mMessageInput.setText(""); // Clear input field
                } else {
                    showToast("Message cannot be empty.");
                }
            }
        });
    }

    // Request Bluetooth permissions for Android 12 (API 31) and above
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 (API 31)
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSION_BLUETOOTH_CONNECT);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_PERMISSION_BLUETOOTH_SCAN);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, REQUEST_PERMISSION_BLUETOOTH_ADVERTISE);
            }
        }
    }

    // Request location permissions for Bluetooth scanning (Android 6.0+)
    private void requestLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0 (API 23)
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                showToast("Bluetooth has been enabled.");
            } else {
                showToast("Bluetooth was not enabled. Exiting app.");
                finish();
            }
        } else if (requestCode == REQUEST_DISCOVERABLE_BT) {
            if (resultCode == RESULT_CANCELED) { // User declined discoverability
                showToast("Device not discoverable. Server might not be found.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Location permission granted.");
            } else {
                showToast("Location permission denied. Bluetooth scanning may not work.");
            }
        } else if (requestCode == REQUEST_PERMISSION_BLUETOOTH_CONNECT ||
                   requestCode == REQUEST_PERMISSION_BLUETOOTH_SCAN ||
                   requestCode == REQUEST_PERMISSION_BLUETOOTH_ADVERTISE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Bluetooth permission granted.");
            } else {
                showToast("Bluetooth permission denied. App functionality may be limited.");
            }
        }
    }

    // Starts the server (AcceptThread)
    private void startServer() {
        // Ensure Bluetooth is discoverable for server mode
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); // Make discoverable for 300 seconds
            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_BT);
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
        appendMessage("Server started. Waiting for connections...");
    }

    // Initiates connection to a paired device by showing a selection dialog
    private void connectToDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // Create lists to hold device names and actual BluetoothDevice objects
            final ArrayList<String> deviceNames = new ArrayList<>();
            final ArrayList<BluetoothDevice> devices = new ArrayList<>();

            for (BluetoothDevice device : pairedDevices) {
                // Add device name to the list for display
                deviceNames.add(device.getName() + "\n" + device.getAddress());
                // Add the actual BluetoothDevice object
                devices.add(device);
            }

            // Convert ArrayList to a CharSequence array for AlertDialog
            final CharSequence[] items = deviceNames.toArray(new CharSequence[0]);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select a device to connect");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 'which' is the index of the selected item
                    BluetoothDevice selectedDevice = devices.get(which);
                    if (selectedDevice != null) {
                        // Cancel any existing connect thread
                        if (mConnectThread != null) {
                            mConnectThread.cancel();
                            mConnectThread = null;
                        }
                        // Start a new ConnectThread with the selected device
                        mConnectThread = new ConnectThread(selectedDevice);
                        mConnectThread.start();
                        appendMessage("Attempting to connect to: " + selectedDevice.getName());
                    } else {
                        showToast("Error: Could not get selected device.");
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    showToast("Connection cancelled.");
                }
            });
            builder.show(); // Show the dialog
        } else {
            showToast("No paired devices found. Pair a device first in system settings.");
        }
    }

    // Manages the connected BluetoothSocket (ConnectedThread)
    private void manageMyConnectedSocket(BluetoothSocket socket, BluetoothDevice device) {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Notify UI that connection is established
        Message msg = mHandler.obtainMessage(MESSAGE_CONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString("device_name", device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    // Sends a message through the ConnectedThread
    private void sendMessage(String message) {
        if (mConnectedThread != null) {
            mConnectedThread.write(message.getBytes());
            appendMessage("You: " + message);
        } else {
            showToast("Not connected to any device.");
        }
    }

    // Appends a message to the chat log TextView and scrolls to the bottom
    private void appendMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChatMessages.append(message + "\n");
                mScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        mScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    // Displays a short toast message
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel all threads when the activity is destroyed
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * or cancelled.
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(APP_NAME, "Socket's listen() method failed", e);
                Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Server socket listen failed: " + e.getMessage());
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(APP_NAME, "Socket's accept() method failed", e);
                    Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
                    Bundle bundle = new Bundle();
                    bundle.putString("toast", "Server socket accept failed: " + e.getMessage());
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                    break; // Exit loop on error
                }

                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageMyConnectedSocket(socket, socket.getRemoteDevice());
                    try {
                        mmServerSocket.close(); // Close the server socket once a connection is established
                    } catch (IOException e) {
                        Log.e(APP_NAME, "Could not close the connect socket", e);
                    }
                    break; // Exit loop after successful connection
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
                Log.d(APP_NAME, "AcceptThread cancelled.");
            } catch (IOException e) {
                Log.e(APP_NAME, "Could not close the connect socket", e);
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID, also used by the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(APP_NAME, "Socket's create() method failed", e);
                Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Client socket creation failed: " + e.getMessage());
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it slows down the connection.
            // This is important if you were doing active discovery,
            // but for paired devices, it's less critical but good practice.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(APP_NAME, "Could not close the client socket", closeException);
                }
                Log.e(APP_NAME, "Could not connect to device", connectException);
                Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Connection failed: " + connectException.getMessage());
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                return;
            }

            // The connection attempt succeeded. Perform work in a separate thread.
            manageMyConnectedSocket(mmSocket, mmDevice);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
                Log.d(APP_NAME, "ConnectThread cancelled.");
            } catch (IOException e) {
                Log.e(APP_NAME, "Could not close the client socket", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(APP_NAME, "Error getting streams", e);
                Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Error getting streams: " + e.getMessage());
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(APP_NAME, "Input stream was disconnected", e);
                    Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
                    Bundle bundle = new Bundle();
                    bundle.putString("toast", "Device disconnected: " + e.getMessage());
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                    break; // Exit loop on disconnection
                }
            }
        }

        // Call this from the main activity to send data to the remote device
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                // Share the sent message back to the UI activity
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, bytes)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(APP_NAME, "Error during write", e);
                Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Error sending message: " + e.getMessage());
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
        }

        // Call this from the main activity to shutdown the connection
        public void cancel() {
            try {
                mmSocket.close();
                Log.d(APP_NAME, "ConnectedThread cancelled.");
            } catch (IOException e) {
                Log.e(APP_NAME, "Could not close the connect socket", e);
            }
        }
    }
          }
