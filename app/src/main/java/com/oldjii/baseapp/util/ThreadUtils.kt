package com.oldjii.baseapp.util

import android.os.Handler
import android.os.Looper
import android.os.Message

object ThreadUtils {

  private var handler: Handler? = null

  /**
   * 使用全局Main Thread handler来post一直Runnable
   *
   * @param runnable
   */
  fun post(runnable: Runnable?) {
    requireNotNull(runnable) { "runnable is null" }
    getHandler()!!.post(runnable)
  }

  fun postDelayed(tag: Any?, runnable: Runnable?, delayMill: Long) {
    var tag = tag ?: throw IllegalArgumentException("tag is null")
    requireNotNull(runnable) { "runnable is null" }
    require(delayMill > 0) { "delayMill <= 0" }
    if (tag is Number || tag is CharSequence) {
      tag = tag.toString().intern()
    }
    val message = Message.obtain(getHandler(), runnable)
    message.obj = tag
    getHandler()!!.sendMessageDelayed(message, delayMill)
  }

  fun cancelAllRunnables(tag: Any?) {
    var tag = tag ?: throw IllegalArgumentException("tag is null")
    if (tag is Number || tag is CharSequence) {
      tag = tag.toString().intern()
    }
    getHandler()!!.removeCallbacksAndMessages(tag)
  }

  private fun getHandler(): Handler? {
    if (handler == null) {
      synchronized(ThreadUtils::class.java) {
        if (handler == null) {
          handler = Handler(Looper.getMainLooper())
        }
      }
    }
    return handler
  }
}