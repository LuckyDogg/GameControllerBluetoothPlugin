package com.unityplugin.bluetoothplugin;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

/**
 * Created by Aleksander Gorka on 06.09.2017.
 */
public class PermissionManager
{
    private Activity activity;
    private FragmentManager fragment_manager;

    public PermissionManager(Activity cActivity)
    {
        activity = cActivity;
        fragment_manager = activity.getFragmentManager();
    }

    public void CheckPermissions(String[] sRequiredPermissions)
    {
        FragmentTransaction fragmentTransaction = fragment_manager.beginTransaction();
        fragmentTransaction.add(new PermissionFragment(sRequiredPermissions), "perm_fragment");
        fragmentTransaction.commit();
    }
}