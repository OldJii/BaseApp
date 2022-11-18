package com.oldjii.baseapp.media

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.widget.Toast
import com.oldjii.baseapp.util.ThreadUtils
import com.oldjii.baseapp.util.permission.OnPermissionCallback
import com.oldjii.baseapp.util.permission.Permission
import com.oldjii.baseapp.util.permission.XXPermissions
import java.io.File
import java.io.IOException

class PhotoHelper {

  companion object {
    private const val IMAGE_UNSPECIFIED = "image/*"
    private const val REQUEST_CAMERA = 201
    private const val REQUEST_ALBUM = 202
    private const val REQUEST_CROP = 203

    val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      PhotoHelper()
    }
  }

  private var mImageFile: File? = null
  private var mCallback: Callback? = null
  private var mNeedCrop: Boolean = false
  private var mMultiple: Boolean = false

  /**
   * 拍照
   */
  fun selectCamera(activity: Activity, needCrop: Boolean, callback: Callback) {
    mNeedCrop = needCrop
    mCallback = callback
    ThreadUtils.post {
      if (XXPermissions.isGranted(
          activity, arrayOf(
            Permission.READ_EXTERNAL_STORAGE,
            Permission.WRITE_EXTERNAL_STORAGE,
            Permission.CAMERA
          )
        )
      ) {
        selectCameraImpl(activity)
      } else {
        XXPermissions.with(activity)
          .permission(Permission.Group.STORAGE)
          .request(object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>, all: Boolean) {
              selectCameraImpl(activity)
            }

            override fun onDenied(permissions: MutableList<String>, never: Boolean) {
              if (never) {
                Toast.makeText(activity, "被永久拒绝授权，请手动授予相机权限", Toast.LENGTH_SHORT).show()
                XXPermissions.startPermissionActivity(activity, permissions)
              } else {
                Toast.makeText(activity, "获取相机权限失败", Toast.LENGTH_SHORT).show()
              }
            }
          })
      }
    }
  }

  /**
   * 选择单张照片
   */
  fun selectPhoto(activity: Activity, needCrop: Boolean, callback: Callback) {
    mMultiple = false
    mNeedCrop = needCrop
    mCallback = callback
    ThreadUtils.post {
      if (XXPermissions.isGranted(activity, Permission.Group.STORAGE)) {
        selectPhotoImpl(activity)
      } else {
        XXPermissions.with(activity)
          .permission(Permission.Group.STORAGE)
          .request(object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>, all: Boolean) {
              selectPhotoImpl(activity)
            }

            override fun onDenied(permissions: MutableList<String>, never: Boolean) {
              if (never) {
                Toast.makeText(activity, "被永久拒绝授权，请手动授予存储权限", Toast.LENGTH_SHORT).show()
                XXPermissions.startPermissionActivity(activity, permissions)
              } else {
                Toast.makeText(activity, "获取存储限失败", Toast.LENGTH_SHORT).show()
              }
            }
          })
      }
    }
  }

  /**
   * 选择多张照片
   *
   * （受限于 EXTRA_ALLOW_MULTIPLE 无法限制最大选择数量）
   */
  fun selectPhotos(activity: Activity, callback: Callback) {
    mMultiple = true
    mCallback = callback
    ThreadUtils.post {
      if (XXPermissions.isGranted(activity, Permission.Group.STORAGE)) {
        selectPhotosImpl(activity)
      } else {
        XXPermissions.with(activity)
          .permission(Permission.Group.STORAGE)
          .request(object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>, all: Boolean) {
              selectPhotosImpl(activity)
            }

            override fun onDenied(permissions: MutableList<String>, never: Boolean) {
              if (never) {
                Toast.makeText(activity, "被永久拒绝授权，请手动授予存储权限", Toast.LENGTH_SHORT).show()
                XXPermissions.startPermissionActivity(activity, permissions)
              } else {
                Toast.makeText(activity, "获取存储限失败", Toast.LENGTH_SHORT).show()
              }
            }
          })
      }
    }
  }

  /**
   * 处理内部逻辑，需要在调启方Activity.onActivityResult()中调用
   */
  fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
    if (Activity.RESULT_OK != resultCode) {
      return
    }
    when (requestCode) {
      // 拍照
      REQUEST_CAMERA -> {
        if (mNeedCrop) {
          // 需要裁剪
          cropImage(activity, Uri.fromFile(mImageFile))
        } else {
          // 无需裁剪
          val list: ArrayList<Uri> = ArrayList()
          list.add(Uri.fromFile(mImageFile))
          onFinish(list)
        }
      }
      // 选择照片
      REQUEST_ALBUM -> {
        if (mMultiple) {
          // 多选
          val list: ArrayList<Uri> = ArrayList()
          val clipData: ClipData? = data?.clipData
          if (clipData == null) {
            // 选择了一张图片
            val uri = data?.data
            uri?.let {
              list.add(uri)
              onFinish(list)
            }
          } else {
            // 选择了多张图片
            for (i in 0 until clipData.itemCount) {
              list.add(clipData.getItemAt(i).uri)
            }
            onFinish(list)
          }
        } else {
          if (mNeedCrop) {
            // 需要裁剪
            createImageFile(activity)
            if (mImageFile == null || !mImageFile!!.exists()) {
              return
            }
            val uri = data?.data
            uri?.let { cropImage(activity, it) }
          } else {
            // 不需裁剪
            val uri = data?.data
            uri?.let {
              val list: ArrayList<Uri> = ArrayList()
              list.add(uri)
              onFinish(list)
            }
          }
        }
      }
      // 裁剪
      REQUEST_CROP -> {
        // 裁剪
        if (mImageFile == null || !mImageFile!!.exists()) {
          return
        }
        val list: ArrayList<Uri> = ArrayList()
        list.add(Uri.fromFile(mImageFile))
        onFinish(list)
      }
    }
  }

  private fun selectCameraImpl(activity: Activity) {
    createImageFile(activity)
    if (!mImageFile!!.exists()) {
      return
    }
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    val photoUri: Uri =
      FileProvider.getUriForFile(activity, activity.packageName + ".provider", mImageFile!!)
    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
    safeStartActivityForResult(activity, intent, REQUEST_CAMERA)
  }

  private fun selectPhotoImpl(activity: Activity) {
    val intent = Intent(Intent.ACTION_PICK)
    intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    safeStartActivityForResult(activity, intent, REQUEST_ALBUM)
  }

  private fun selectPhotosImpl(activity: Activity) {
    // EXTRA_ALLOW_MULTIPLE 目前支持 ACTION_GET_CONTENT、ACTION_OPEN_DOCUMENT
    // 详情参考 https://developer.android.com/reference/android/content/Intent#EXTRA_ALLOW_MULTIPLE
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_UNSPECIFIED)
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    safeStartActivityForResult(activity, intent, REQUEST_ALBUM)
  }

  private fun cropImage(activity: Activity, uri: Uri) {
    val intent = Intent("com.android.camera.action.CROP")
    intent.setDataAndType(uri, IMAGE_UNSPECIFIED)
    intent.putExtra("crop", "true")
    intent.putExtra("aspectX", 1)
    intent.putExtra("aspectY", 1)
    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mImageFile))
    safeStartActivityForResult(activity, intent, REQUEST_CROP)
  }

  private fun createImageFile(activity: Activity) {
    mImageFile = File(
      Environment.getExternalStorageDirectory(),
      System.currentTimeMillis().toString() + ".jpg"
    )
    try {
      mImageFile?.createNewFile()
    } catch (e: IOException) {
      e.printStackTrace()
      Toast.makeText(activity, "出错啦", Toast.LENGTH_SHORT).show()
    }
  }

  private fun safeStartActivityForResult(activity: Activity, intent: Intent, requestCode: Int) {
    if (intent.resolveActivity(activity.packageManager) != null) {
      activity.startActivityForResult(intent, requestCode)
    } else {
      Toast.makeText(activity, "没有安装目标应用", Toast.LENGTH_SHORT).show()
    }
  }

  private fun onFinish(list: ArrayList<Uri>) {
    mCallback?.onComplete(list)
    mImageFile = null
    mCallback = null
    mNeedCrop = false
  }
}

interface Callback {
  fun onComplete(list: ArrayList<Uri>)
}
