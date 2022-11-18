package com.oldjii.baseapp.helper.share

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import com.oldjii.baseapp.util.ContextUtil
import com.oldjii.baseapp.util.GlideUtil
import com.oldjii.baseapp.util.PackageUtil
import com.sina.weibo.sdk.api.WebpageObject
import com.sina.weibo.sdk.api.WeiboMultiMessage
import com.sina.weibo.sdk.auth.AuthInfo
import com.sina.weibo.sdk.common.UiError
import com.sina.weibo.sdk.openapi.IWBAPI
import com.sina.weibo.sdk.openapi.SdkListener
import com.sina.weibo.sdk.openapi.WBAPIFactory
import com.sina.weibo.sdk.share.WbShareCallback
import com.tencent.connect.share.QQShare
import com.tencent.connect.share.QzoneShare
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.tencent.tauth.DefaultUiListener
import com.tencent.tauth.Tencent
import org.json.JSONObject
import java.util.*


/**
 * 分享SDk帮助类
 *
 * 微信 SDK：https://developers.weixin.qq.com/doc/oplatform/Mobile_App/Share_and_Favorites/Android.html
 * 腾讯 SDK：https://wiki.connect.qq.com/分享功能存储权限适配
 * 微博 SDK：https://github.com/sinaweibosdk/weibo_android_sdk/blob/master/2019SDK/文档/微博Android%20SDK使用指南_v11.12.0.pdf
 */
class ShareHelper {
  companion object {
    // WeChat App Key
    private const val APP_KEY_WECHAT = ""

    // QQ App ID
    // 与AndroidManifest中AuthActivity的scheme属性保持一致
    private const val APP_ID_QQ = ""

    // QQ App Key
    private const val APP_KEY_QQ = ""

    // 与AndroidManifest中provider的authorities属性保持一致
    private const val QQ_AUTHORITIES = "com.oldjii.baseapp.provider"

    // WeiBo App Key
    private const val APP_KEY_WeiBo = ""

    // Weibo Callback page
    const val WEIBO_REDIRECT_URL = "https://api.weibo.com/oauth2/default.html"

    // Weibo Scope
    // todo: https://open.weibo.com/wiki/微博API#.E5.BE.AE.E5.8D.9A
    // 文档表示单纯分享功能不需要进行scope授权
    // const val WEIBO_SCOPE = ("email,direct_messages_read,direct_messages_write,"
    //     + "friendships_groups_read,friendships_groups_write,statuses_to_me_read,"
    //     + "follow_app_official_microblog," + "invitation_write")

    const val WEIBO_SCOPE = ""

    val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      ShareHelper()
    }
  }

  private var qqApi: Tencent? = null
  private var wechatApi: IWXAPI? = null
  private var weiboApi: IWBAPI? = null

  /**
   * 初始化分享SDK
   */
  fun initSDK() {
    if (isWechatInstalled()) {
      initWechatApi()
    }
    if (isQQInstalled()) {
      initQQApi()
    }
    if (isWeiboInstalled()) {
      initWeiBoApi()
    }
  }

  /**
   * 获取当前设备可分享渠道
   */
  fun getShareChannel(): String {
    val channels = ArrayList<String>()
    if (isWechatInstalled()) {
      channels.add("mo")
      channels.add("wx")
    }
    if (isQQInstalled()) {
      channels.add("qq")
      channels.add("qz")
    }
    if (isWeiboInstalled()) {
      channels.add("wb")
    }
    val sb = StringBuilder()
    sb.append("[")
    for (channel in channels) {
      sb.append("\"").append(channel).append("\",")
    }
    if (sb[sb.length - 1] == ',') {
      sb.setLength(sb.length - 1)
    }
    sb.append("]")
    return sb.toString()
  }

  /**
   * 分享
   */
  fun share(activity: Activity?, jsonObject: JSONObject?) {
    val url = jsonObject?.optString("url")
    val title = jsonObject?.optString("title")
    val description = jsonObject?.optString("description")
    val imgUrl = jsonObject?.optString("imgUrl")
    val channel = jsonObject?.optString("channel")
    val shareInfo = ShareInfo(title, description, url, imgUrl, channel)

    when (shareInfo.channel) {
      "mo" -> shareWechat(activity, shareInfo, true)
      "wx" -> shareWechat(activity, shareInfo, false)
      "qz" -> shareQQ(activity, shareInfo, true)
      "qq" -> shareQQ(activity, shareInfo, false)
      "wb" -> shareWeiBo(activity, shareInfo)
      "more" -> shareMore(activity, shareInfo)
    }
  }

  /**
   * 微博分享回调
   */
  fun doResultIntent(data: Intent?) {
    getWeiboApi()?.doResultIntent(data, object : WbShareCallback {
      override fun onComplete() {
        // 分享成功
      }

      override fun onError(uiError: UiError) {
        // 分享失败
      }

      override fun onCancel() {
        // 分享取消
      }
    })
  }

  /**
   * 微信分享回调
   */
  fun handleIntent(handle: IWXAPIEventHandler, data: Intent?) {
    getWechatApi()?.handleIntent(data, handle)
  }

  private fun initWechatApi() {
    wechatApi = WXAPIFactory.createWXAPI(ContextUtil.context(), APP_KEY_WECHAT, true)
    wechatApi?.registerApp(APP_KEY_WECHAT)
    ContextUtil.context().registerReceiver(object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        wechatApi?.registerApp(APP_KEY_WECHAT)
      }
    }, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))
  }

  private fun initQQApi() {
    qqApi = Tencent.createInstance(APP_ID_QQ, ContextUtil.context(), QQ_AUTHORITIES)
  }

  private fun initWeiBoApi() {
    val authinfo = AuthInfo(
      ContextUtil.context(),
      APP_KEY_WeiBo,
      WEIBO_REDIRECT_URL,
      WEIBO_SCOPE
    )
    weiboApi = WBAPIFactory.createWBAPI(ContextUtil.context())
    weiboApi?.registerApp(ContextUtil.context(), authinfo, object : SdkListener {
      override fun onInitSuccess() {
        // success
      }

      override fun onInitFailure(e: Exception) {
        // fail
      }
    })
  }

  private fun getWechatApi(): IWXAPI? {
    wechatApi ?: initWechatApi()
    return wechatApi
  }

  private fun getQQApi(): Tencent? {
    qqApi ?: initQQApi()
    return qqApi
  }

  private fun getWeiboApi(): IWBAPI? {
    weiboApi ?: initWeiBoApi()
    return weiboApi
  }

  private fun shareWechat(context: Context?, info: ShareInfo, isFriend: Boolean) {
    // 未安装
    if (!isWechatInstalled()) {
      Toast.makeText(ContextUtil.context(), "未安装应用", Toast.LENGTH_SHORT).show()
      return
    }
    val webpage = WXWebpageObject()
    webpage.webpageUrl = info.url
    val msg = WXMediaMessage(webpage)
    msg.title = info.title
    msg.description = info.description

    val callback = Runnable {
      val req = SendMessageToWX.Req()
      req.transaction = System.currentTimeMillis().toString()
      req.message = msg
      req.scene =
        if (isFriend) SendMessageToWX.Req.WXSceneTimeline else SendMessageToWX.Req.WXSceneSession
      getWechatApi()?.sendReq(req)
    }

    if (info.imgUrl != null && info.imgUrl.isNotEmpty()) {
      GlideUtil.loadByteAsync(context, info.imgUrl) {
        msg.thumbData = it
        callback.run()
      }
    } else {
      callback.run()
    }
  }

  private fun shareQQ(activity: Activity?, info: ShareInfo, isZone: Boolean) {
    // 未安装
    if (!isQQInstalled()) {
      Toast.makeText(ContextUtil.context(), "未安装应用", Toast.LENGTH_SHORT).show()
      return
    }
    val listener: DefaultUiListener = object : DefaultUiListener() {
      override fun onComplete(o: Any) {
        super.onComplete(o)
        // completes
      }
    }
    if (isZone) {
      val params = Bundle()
      if (info.imgUrl != null && info.imgUrl.isNotEmpty()) {
        // 有图片
        params.putInt(
          QzoneShare.SHARE_TO_QZONE_KEY_TYPE,
          QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT
        )
        val imageList = ArrayList<String?>()
        imageList.add(info.imgUrl)
        params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, imageList)
      } else {
        // 没有图片
        params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzoneShare.SHARE_TO_QZONE_TYPE_NO_TYPE);
      }
      params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN)
      params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, info.url)
      params.putString(QzoneShare.SHARE_TO_QQ_TITLE, info.title)
      params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, info.description)
      getQQApi()?.shareToQzone(activity, params, listener)
    } else {
      val params = Bundle()
      params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT)
      params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, info.url)
      params.putString(QQShare.SHARE_TO_QQ_TITLE, info.title)
      params.putString(QQShare.SHARE_TO_QQ_SUMMARY, info.description)
      params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, info.imgUrl)
      getQQApi()?.shareToQQ(activity, params, listener)
    }
  }

  private fun shareWeiBo(activity: Activity?, info: ShareInfo) {
    // 未安装
    if (!isWeiboInstalled()) {
      Toast.makeText(ContextUtil.context(), "未安装应用", Toast.LENGTH_SHORT).show()
      return
    }
    val message = WeiboMultiMessage()
    val webpage = WebpageObject()
    webpage.identify = UUID.randomUUID().toString()
    webpage.title = info.title
    webpage.description = info.description
    webpage.actionUrl = info.url

    val callback = Runnable {
      message.mediaObject = webpage
      getWeiboApi()?.shareMessage(activity, message, false)
    }

    if (info.imgUrl != null && info.imgUrl.isNotEmpty()) {
      GlideUtil.loadByteAsync(activity, info.imgUrl) {
        webpage.thumbData = it
        callback.run()
      }
    } else {
      callback.run()
    }
  }

  private fun shareMore(context: Context?, info: ShareInfo) {
    val intent = Intent()
    intent.action = Intent.ACTION_SEND
    intent.putExtra(Intent.EXTRA_TEXT, info.description)
    intent.type = "text/plain"
    context?.startActivity(intent)
  }

  private fun isWechatInstalled(): Boolean {
    return PackageUtil.checkPackageInstalled(arrayOf("com.tencent.mm"))
  }

  private fun isQQInstalled(): Boolean {
    return PackageUtil.checkPackageInstalled(
      arrayOf(
        "com.tencent.tim",
        "com.tencent.mobileqq",
        "com.tencent.mobileqqi"
      )
    )
  }

  private fun isWeiboInstalled(): Boolean {
    return PackageUtil.checkPackageInstalled(
      arrayOf(
        "com.sina.weibo",
        "com.sina.weibog3",
        "com.sina.weibolite"
      )
    )
  }
}