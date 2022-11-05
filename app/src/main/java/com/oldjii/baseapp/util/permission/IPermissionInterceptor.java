package com.oldjii.baseapp.util.permission;

import android.support.v4.app.FragmentActivity;
import java.util.ArrayList;
import java.util.List;

public interface IPermissionInterceptor {
  default void requestPermissions(
      FragmentActivity activity, OnPermissionCallback callback, List<String> permissions) {
    PermissionFragment.beginRequest(activity, new ArrayList(permissions), callback);
  }

  default void grantedPermissions(FragmentActivity activity, OnPermissionCallback callback, List<String> permissions, boolean all) {
    callback.onGranted(permissions, all);
  }

  default void deniedPermissions(FragmentActivity activity, OnPermissionCallback callback, List<String> permissions, boolean never) {
    callback.onDenied(permissions, never);
  }
}
