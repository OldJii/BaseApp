package com.oldjii.baseapp.helper.share.wxapi

import android.app.Activity
import android.os.Bundle
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.oldjii.baseapp.helper.share.ShareHelper

class WXEntryActivity : Activity(), IWXAPIEventHandler {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ShareHelper.instance.handleIntent(this, intent)
  }

  override fun onReq(baseReq: BaseReq) {}
  override fun onResp(baseResp: BaseResp) {}
}