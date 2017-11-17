package com.unityplugin.bluetoothplugin;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Aleksander Gorka on 06.09.2017.
 */
public class BluetoothManager
{
    private static final String TAG = "BluetoothManager";
    private static final String[] REQUIRED_PERMISSIONS =
            {
                    "android.permission.BLUETOOTH",
                    "android.permission.BLUETOOTH_ADMIN",
                    "android.permission.ACCESS_COARSE_LOCATION"
            };

    private PermissionManager permission_manager;
    private Activity activity;
    private BluetoothAdapter bluetooth_adapter;
    private BluetoothConnection bluetooth_connection;
    private static boolean b_devices_found;
    private Set<BluetoothDevice> bt_paired_devices;
    private List<BluetoothDevice> bt_discovered_devices;

    public BluetoothManager(Activity cActivity)
    {
        b_devices_found = false;
        activity = cActivity;
        bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        permission_manager = new PermissionManager(cActivity);
        bluetooth_connection = new BluetoothConnection(activity);
    }

    /**
     * Zwraca listę sparowanych urządzeń
     */
    public String getPairedDevices()
    {
        bt_paired_devices = bluetooth_adapter.getBondedDevices();
        String _sBtPairedDevices = "";
        for (BluetoothDevice _btD : bt_paired_devices)
            _sBtPairedDevices +="Name: " + _btD.getName()+"\nAddress: " + _btD.getAddress()+", ";
        return _sBtPairedDevices;
    }

    /**
     * Zwraca listę znalezionych urządzeń
     */
    public String getDiscoveredDevices()
    {
        String _sBtNewDevices = "";
        for (BluetoothDevice _btD : bt_discovered_devices)
            _sBtNewDevices +="Name: " + _btD.getName()+"\nAddress: " + _btD.getAddress()+", ";
        return _sBtNewDevices;
    }

    /**
     * Włacza/wyłącza Bluetooth w smartfonie
     */
    public void EnableDisableBluetooth()
    {
        if (bluetooth_adapter != null)
        {
            if (!bluetooth_adapter.isEnabled())
            {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivity(intent);

                IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                activity.registerReceiver(mBroadcastReciver1, BTIntent);
            } else
            {
                bluetooth_adapter.disable();
                IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                activity.registerReceiver(mBroadcastReciver1, BTIntent);
            }
        } else
            Log.d("BT ADAPTER ERROR!", "Bluetooth adapter is missing!");
    }

    /**
     * Umożliwia odnalezienie smartfonu przez inne urządzenia
     * iDiscaverableDuration - czas przez jaki urządzenie ma być widoczne
     * w sekundach
     */
    public void MakeDeviceDiscoverable(int iDiscaverableDuration)
    {
        CheckPermissions();
        if (!bluetooth_adapter.isDiscovering())
        {
            Intent _discaverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            if (iDiscaverableDuration != 0)
                _discaverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, iDiscaverableDuration);
            activity.startActivity(_discaverableIntent);
        }
    }

    /**
     * Szuka urządzeń z Bluetooth znajdujących się w pobliżu
     */
    public void DiscoverDevices()
    {
        CheckPermissions();
        bt_discovered_devices = new ArrayList<>();

        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");
        if (bluetooth_adapter.isDiscovering())
        {
            bluetooth_adapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");
        }
        bluetooth_adapter.startDiscovery();
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(mBroadcastReceiverNewDevices, discoverDevicesIntent);
    }

    /**
     * Broadcast Receiver do nasłuchiwania niesparowanych urządzeń
     */
    private BroadcastReceiver mBroadcastReceiverNewDevices = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                b_devices_found=true;
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                bt_discovered_devices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
            }
        }
    };

    /**
     * Broadcast Receiver do sprawdzania stanu adaptera Bluetooth
     */
    private final BroadcastReceiver mBroadcastReciver1 = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(bluetooth_adapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetooth_adapter.ERROR);

                switch (state)
                {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "onReceive: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onReceive: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "onReceive: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Powiązuje urządzenie ze smartfonem
     */
    public void BindWithDevice(int iDeviceNumber)
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            Log.d(TAG, "Trying to pair with device number " + iDeviceNumber);
            bt_discovered_devices.get(iDeviceNumber).createBond();
        }
    }

    /**
     * Sprawdza czy aplikacja dostała wszystkie potrzebne pozwolenia
     * jeżeli nie to pyta o te pozwolenia poprzez permission_menager'a
     */
    private void CheckPermissions()
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            int _permissionCheck = 0;
            for (String _permission:REQUIRED_PERMISSIONS)
            {
                _permissionCheck += activity.checkSelfPermission(_permission);
                Log.d(TAG,_permissionCheck + " : " + _permission);
            }
            if (_permissionCheck != 0)
                permission_manager.CheckPermissions(REQUIRED_PERMISSIONS);
        }
    }

    /**
     * Nawiązanie połączenia z urządzeniem jeżeli zostało ono wcześniej
     * znalezione przez smartfona
     */
    public void StartBtConnection(String sDeviceName, String sUuid)
    {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
        int i = 0;
        for (BluetoothDevice a:bt_discovered_devices)
        {
            if(a.getName().equals(sDeviceName))
            {
                bluetooth_connection.startClient(bt_discovered_devices.get(i), UUID.fromString(sUuid));
                return;
            }
            i++;
        }
    }

    /**
     * Wysłanie danych w postaci tekstowej do urządzenia, z którym
     * jest połączony smartfon przez protokół Bluetooth
     */
    public void Send(String sMessage)
    {
        byte[] bytes = sMessage.getBytes(Charset.defaultCharset());
        bluetooth_connection.write(bytes);
    }

    /**
     * Funkcja zwraca dane jakie zostały odebrane przez protokół
     * Bluetooth z urządzenia, z którym jest on połaczony
     */
    public String sReceiveData()
    {
        return bluetooth_connection.s_received_data;
    }

    /**
     * Zwraca informację o tym czy smartfon nawiązał połączenie
     * z jakimś urządzeniem
     */
    public boolean bIsConnected()
    {
        return bluetooth_connection.isConnected();
    }

    /**
     * Zwraca informacje o tym czy zostało znalezione jakieś urządzenie
     */
    public static boolean bDeviceHasBeenFound()
    {
        return b_devices_found;
    }
}