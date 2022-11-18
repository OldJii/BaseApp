package com.oldjii.baseapp.util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Pair;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PackageUtil {

  private static int targetSdkVersion = 0;

  public PackageUtil() {
  }

  public static String currentProcessName() {
    BufferedReader cmdlineReader = null;

    int pid;
    try {
      cmdlineReader = new BufferedReader(new FileReader("/proc/" + Process.myPid() + "/cmdline"));
      StringBuilder processName = new StringBuilder();

      while ((pid = cmdlineReader.read()) > 0) {
        processName.append((char) pid);
      }

      String var3 = processName.toString();
      return var3;
    } catch (FileNotFoundException var15) {
    } catch (IOException var16) {
    } finally {
      try {
        if (cmdlineReader != null) {
          cmdlineReader.close();
        }
      } catch (IOException var14) {
      }
    }

    pid = Process.myPid();
    ActivityManager activityManager =
        (ActivityManager) ContextUtil.context().getSystemService(Context.ACTIVITY_SERVICE);
    Iterator var19 = activityManager.getRunningAppProcesses().iterator();

    RunningAppProcessInfo appProcess;
    do {
      if (!var19.hasNext()) {
        return null;
      }

      appProcess = (RunningAppProcessInfo) var19.next();
    } while (appProcess.pid != pid);

    return appProcess.processName;
  }

  public static List<PackageInfo> getAllPackageInfo(@NonNull Context context, int flag) {
    return context.getPackageManager().getInstalledPackages(flag);
  }

  public static Pair<List<AppInfo>, List<AppInfo>> getAppList(
      @NonNull Context context) {
    ArrayList<AppInfo> appList1 = new ArrayList();
    ArrayList<AppInfo> appList2 = new ArrayList();
    PackageManager pm = context.getPackageManager();
    List<PackageInfo> packages = getAllPackageInfo(context, 0);
    Iterator var5 = packages.iterator();

    while (var5.hasNext()) {
      PackageInfo packageInfo = (PackageInfo) var5.next();
      AppInfo info;
      if ((packageInfo.applicationInfo.flags & 1) == 0) {
        info = new AppInfo();
        info.appName = packageInfo.applicationInfo.loadLabel(pm).toString();
        info.packageName = packageInfo.packageName;
        appList1.add(info);
      } else {
        info = new AppInfo();
        info.appName = packageInfo.applicationInfo.loadLabel(pm).toString();
        info.packageName = packageInfo.packageName;
        appList2.add(info);
      }
    }

    return new Pair(appList1, appList2);
  }

  public static Pair<List<PackageInfo>, List<PackageInfo>> getPackageInfo(
      @NonNull Context context) {
    ArrayList<PackageInfo> appList1 = new ArrayList();
    ArrayList<PackageInfo> appList2 = new ArrayList();
    PackageManager pm = context.getPackageManager();
    List<PackageInfo> packages = getAllPackageInfo(context, 0);
    Iterator var5 = packages.iterator();

    while (var5.hasNext()) {
      PackageInfo packageInfo = (PackageInfo) var5.next();
      if ((packageInfo.applicationInfo.flags & 1) == 0) {
        appList1.add(packageInfo);
      } else {
        appList2.add(packageInfo);
      }
    }

    return new Pair(appList1, appList2);
  }

  public static boolean checkPackageInstalled(String packageName) {
    try {
      ContextUtil.context().getPackageManager().getApplicationInfo(packageName, 0);
      return true;
    } catch (NameNotFoundException var2) {
      return false;
    }
  }

  public static boolean checkPackageInstalled(String[] packageNames) {
    String[] var1 = packageNames;
    int var2 = packageNames.length;

    for (int var3 = 0; var3 < var2; ++var3) {
      String p = var1[var3];
      if (checkPackageInstalled(p)) {
        return true;
      }
    }

    return false;
  }

  public static Intent getLaunchIntentForPackage(@NonNull Context context, String tag) {
    return context.getPackageManager().getLaunchIntentForPackage(tag);
  }

  public static List<ResolveInfo> queryIntentActivities(
      @NonNull Context context, @NonNull Intent intent, int flag) {
    return context.getPackageManager().queryIntentActivities(intent, flag);
  }

  public static int targetSdkVersion() {
    if (targetSdkVersion <= 0) {
      try {
        ApplicationInfo info =
            ContextUtil.context()
                .getPackageManager()
                .getApplicationInfo(ContextUtil.context().getPackageName(), 0);
        targetSdkVersion = info.targetSdkVersion;
      } catch (NameNotFoundException var1) {
      }
    }

    return targetSdkVersion;
  }

  public static class AppInfo {

    public String appName;
    public String packageName;
    public String processName;
    public int pid;

    public AppInfo() {
    }
  }
}
