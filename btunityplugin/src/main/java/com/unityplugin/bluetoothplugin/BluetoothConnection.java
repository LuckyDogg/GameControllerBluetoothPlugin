package com.unityplugin.bluetoothplugin;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by Aleksander Górka on 07.09.2017.
 */
public class BluetoothConnection
{
    private static final String TAG = "BluetoothConnection";
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final byte BEGIN_OF_RECEIVE_DATA = '<';
    private static final byte END_OF_RECEIVE_DATA= '>';

    private BluetoothAdapter bluetooth_adapter;
    private BluetoothDevice bluetooth_device;
    private ConnectedThread connected_thread;
    private ConnectThread connect_thread;
    private UUID device_UUID;
    private boolean b_connection_in_progress;
    private Context context;

    public String s_received_data;

    public BluetoothConnection(Context cContext)
    {
        b_connection_in_progress = false;
        context = cContext;
        bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        connected_thread = null;
    }

    public boolean isConnected()
    {
        return connected_thread!=null;
    }

    public boolean isTheConnectionInProgress()
    {
        return b_connection_in_progress;
    }

    /**
     * Funkcja tworzy ConnectThread który próbuje nawiązać połączenie z danym urządzeniem
     * @param device Urządzenie z którym ma nastąpić połączenie
     **/
    public void startClient(BluetoothDevice device, UUID uuid)
    {
        Log.d(TAG, "startClient: Started.");        //initprogress dialog
        b_connection_in_progress = true;

        connect_thread = new ConnectThread(device, uuid);
        connect_thread.start();
    }

    private void connected(BluetoothSocket bluetooth_socket, BluetoothDevice bluetooth_device)
    {
        Log.d(TAG, "connected: Starting.");

        // Uruchamia wątek zarządzający połączeniem oraz transmisją danych
        connected_thread = new ConnectedThread(bluetooth_socket);
        connected_thread.start();
    }

    /**
     * Wysyła pakiet danych przez connected_thread
     * @param out Dane do wysłania
     */
    public void write(byte[] out)
    {
        Log.d(TAG, "write: Write Called.");
        connected_thread.write(out);
    }

    /**
     * Wątek uruchamia się gdy próbuje nawiązać połączenie wychodzące
     * z urządzeniem.
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

            // Stwórz gniazdo do połączenia z urządzeniem.
            try
            {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcobluetooth_socket using UUID: "
                        + MY_UUID_INSECURE);
                _tempSocket = bluetooth_device.createRfcommSocketToServiceRecord(device_UUID);
            } catch (IOException e)
            {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcobluetooth_socket " + e.getMessage());
            }

            bluetooth_socket = _tempSocket;

            // Anuluje wyszukiwanie urządzenie ponieważ to spowalnia połączenie
            bluetooth_adapter.cancelDiscovery();

            // Tworzy połączenie z urządzeniem
            try
            {
                bluetooth_socket.connect();
                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e)
            {
                // Zamyka gniazdo jeżeli nie udało sie nawiązać połączenia
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
                Log.e(TAG, "cancel: close() of bluetooth_socket in Connectthread failed. " + e.getMessage());
            }
        }
    }

    /**
     * Wątek odpowiedzialny za utrzymanie połączenia oraz wysyłanie i odbieranie danych
     **/
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket bluetooth_socket;
        private final InputStream in_stream;
        private final OutputStream out_stream;

        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG, "ConnectedThread: Starting.");
            bluetooth_socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                b_connection_in_progress = false;
            } catch (NullPointerException e)
            {
                e.printStackTrace();
            }

            try
            {
                tmpIn = bluetooth_socket.getInputStream();
                tmpOut = bluetooth_socket.getOutputStream();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            in_stream = tmpIn;
            out_stream = tmpOut;
        }

        public void run()
        {
            byte[] readBuffer = new byte[1024];  // magazyn bufora dla strumienia
            int readBufferPosition = 0;
            boolean stopWorker = false;

            // Odbieraj dane, aż pojawi się wyjątek
            while (!Thread.currentThread().isInterrupted() && !stopWorker)
            {
                try
                {
                    int bytesAvailable = in_stream.available();
                    if (bytesAvailable > 0)
                    {
                        byte[] packetBytes = new byte[bytesAvailable];
                        in_stream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++)
                        {
                            byte b = packetBytes[i];
                            if(b == BEGIN_OF_RECEIVE_DATA);
                            else if (b == END_OF_RECEIVE_DATA)
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;

                                Log.d(TAG, "InputStream: " + data);
                                s_received_data = data;
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

        // Wysłanie danych
        public void write(byte[] bytes)
        {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try
            {
            //    for(int i = 0 ;i<text.length();i++)
             //       out_stream.write(text.charAt(i));
                out_stream.write(bytes);
            } catch (IOException e)
            {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
        }

        // Zamknięcie połączenia
        public void cancel()
        {
            try
            {
                bluetooth_socket.close();
            } catch (IOException e){}
        }
    }
}