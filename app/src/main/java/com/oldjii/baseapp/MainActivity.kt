package com.oldjii.baseapp

import android.app.Activity
import android.os.Bundle
import com.oldjii.baseapp.helper.share.ShareHelper
import com.oldjii.baseapp.util.ContextUtil

class MainActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    // 全局 Context
    ContextUtil.init(applicationContext)
    // 分享 SDK
    ShareHelper.instance.initSDK()
  }
}