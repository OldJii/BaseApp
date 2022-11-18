package com.oldjii.baseapp.util;

import android.app.Application;
import android.content.Context;

public class ContextUtil {
  public static boolean hadInit = false;
  private static Application context;

  public ContextUtil() {}

  public static void init(Context context) {
    if (context instanceof Application) {
      ContextUtil.context = (Application) context;
    } else {
      ContextUtil.context = (Application) context.getApplicationContext();
    }

    hadInit = true;
  }

  public static Application context() {
    if (hadInit) {
      return context;
    } else {
      throw new RuntimeException("you should call init first!");
    }
  }
}
