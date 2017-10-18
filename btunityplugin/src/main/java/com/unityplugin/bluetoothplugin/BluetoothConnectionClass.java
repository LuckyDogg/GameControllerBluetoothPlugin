package com.unityplugin.bluetoothplugin;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Debug;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by Alex on 07.09.2017.
 */
public class BluetoothConnectionClass
{
    private static final String TAG = "BluetoothConnectionClass";
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String APP_NAME = "MYAPP";
    private static final byte BEGINING_OF_RECEIVE_DATA = '<';
    private static final byte END_OF_RECEIVE_DATA= '>';

    private BluetoothAdapter bluetooth_adapter;
    private BluetoothDevice bluetooth_device;
    private ConnectedThread connected_thread;
    private ConnectThread connect_thread;
    private AcceptThread accept_thread;
    private UUID device_UUID;
    private ProgressDialog progress_dialog;

    private int readBufferPosition;;
    public String s_recived_data;

    Context context;

    public BluetoothConnectionClass(Context cContext)
    {
        context = cContext;
        bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        connected_thread=null;
    }

    public boolean isConnected()
    {
        if(connected_thread!=null)
            return true;
        return false;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start()
    {
        Log.d(TAG, "start");
        // Cancel any thread attempting to make a connection
        if (connect_thread != null)
        {
            connect_thread.cancel();
            connect_thread = null;
        }
        if (accept_thread == null)
        {
            accept_thread = new AcceptThread();
            accept_thread.start();
        }
    }

    /**
     * AcceptThread starts and sits waiting for a connection.
     * Then ConnectThread starts and attempts to make a connection with the other devices AcceptThread.
     **/
    public void startClient(BluetoothDevice device, UUID uuid)
    {
        Log.d(TAG, "startClient: Started.");
        //initprogress dialog
        progress_dialog = ProgressDialog.show(context, "Connecting Bluetooth"
                , "Please Wait...", true);

        connect_thread = new ConnectThread(device, uuid);
        connect_thread.start();
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice bluetooth_device)
    {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        connected_thread = new ConnectedThread(mmSocket);
        connected_thread.start();
    }

    /**
     * Write to the connected_thread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out)
    {
        Log.d(TAG, "write: Write Called.");
        connected_thread.write(out);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread
    {
        private final BluetoothServerSocket bluetooth_server_socket;

        public AcceptThread()
        {
            BluetoothServerSocket tmp = null;

            // Creat a new listening server socket
            try
            {
                tmp = bluetooth_adapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
            } catch (IOException e)
            {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }
            bluetooth_server_socket = tmp;
        }

        public void run()
        {
            Log.d(TAG, "run: AcceptThread Running.");
            BluetoothSocket socket = null;
            try
            {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start.....");
                socket = bluetooth_server_socket.accept();
                Log.d(TAG, "run: RFCOM server socket accepted connection.");
            } catch (IOException e)
            {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }
            //talk about this is in the 3rd
            if (socket != null)
                connected(socket, bluetooth_device);
            Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel()
        {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try
            {
                bluetooth_server_socket.close();
            } catch (IOException e)
            {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread
    {
        private BluetoothSocket bluetooth_socket;

        public ConnectThread(BluetoothDevice device, UUID uuid)
        {
            Log.d(TAG, "ConnectThread: started.");
            bluetooth_device = device;
            device_UUID = uuid;
        }

        public void run()
        {
            BluetoothSocket _tempSocket = null;
            Log.i(TAG, "RUN mConnectThread ");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try
            {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        + MY_UUID_INSECURE);
                _tempSocket = bluetooth_device.createRfcommSocketToServiceRecord(device_UUID);
            } catch (IOException e)
            {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            bluetooth_socket = _tempSocket;

            // Always cancel discovery because it will slow down a connection
            bluetooth_adapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try
            {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                bluetooth_socket.connect();

                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e)
            {
                // Close the socket
                try
                {
                    bluetooth_socket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1)
                {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE);
            }

            //will talk about this in the 3rd video
            connected(bluetooth_socket, bluetooth_device);
        }

        public void cancel()
        {
            try
            {
                Log.d(TAG, "cancel: Closing Client Socket.");
                bluetooth_socket.close();
            } catch (IOException e)
            {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }

    /**
     * Finally the ConnectedThread which is responsible for maintaining the BTConnection, Sending the data, and
     * receiving incoming data through input/output streams respectively.
     **/
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG, "ConnectedThread: Starting.");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss the progressdialog when connection is established
            try
            {
                progress_dialog.dismiss();
            } catch (NullPointerException e)
            {
                e.printStackTrace();
            }

            try
            {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] readBuffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            readBufferPosition = 0;
            boolean stopWorker = false;

            String _sTemp = "";
            // Keep listening to the InputStream until an exception occurs
            while (!Thread.currentThread().isInterrupted() && !stopWorker)
            {
                try
                {
                    int bytesAvailable = mmInStream.available();
                    if (bytesAvailable > 0)
                    {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mmInStream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++)
                        {
                            byte b = packetBytes[i];
                            if(b == BEGINING_OF_RECEIVE_DATA);
                            else if (b == END_OF_RECEIVE_DATA)
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;

                                Log.d(TAG, "InputStream: " + data);
                                s_recived_data = data;
                            }
                            else
                                readBuffer[readBufferPosition++] = b;
                        }
                    }
                } catch (IOException ex)
                {
                    stopWorker = true;
                }
            }
        }

        //Call this from the main activity to send data to the remote device
        public void write(byte[] bytes)
        {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try
            {
            //    for(int i = 0 ;i<text.length();i++)
             //       mmOutStream.write(text.charAt(i));
                mmOutStream.write(bytes);
            } catch (IOException e)
            {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel()
        {
            try
            {
                mmSocket.close();
            } catch (IOException e)
            {
            }
        }
    }
}