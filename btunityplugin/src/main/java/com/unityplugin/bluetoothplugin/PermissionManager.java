package com.unityplugin.bluetoothplugin;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

/**
 * Created by Alex on 06.09.2017.
 */
public class PermissionManager
{
    private Activity activity;
    private FragmentManager fragment_manager;

    public PermissionManager(Activity cActivity) {
        activity = cActivity;

        // Create fragment PermissionFragment and add to our activity.
        fragment_manager = activity.getFragmentManager();
    }

    public void CheckPermissions(String[] sRequiredPermissions)
    {
        FragmentTransaction _fragmentTransaction = fragment_manager.beginTransaction();
        _fragmentTransaction.add(new PermissionFragment(sRequiredPermissions), "perm_fragment");
        _fragmentTransaction.commit();
    }
}