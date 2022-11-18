package com.oldjii.baseapp.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.ThumbnailUtils;
import android.support.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import java.io.ByteArrayOutputStream;

public class GlideUtil {

  public static void loadByteAsync(Context context, String url, Callback<byte[]> callback) {
    RxJavaUtil.asyncDo(() -> {
      Bitmap bitmap = Glide.with(context).asBitmap().apply(new RequestOptions().centerCrop())
          .load(url).submit().get();
      return getImageThumbByteArray(bitmap);
    }, callback::onCall);
  }

  private static byte[] getImageThumbByteArray(@Nullable Bitmap src) {
    if (src == null) {
      return null;
    } else {
      Bitmap bitmap;
      if (src.getWidth() <= 500 && src.getHeight() <= 500) {
        bitmap = src;
      } else {
        bitmap = ThumbnailUtils.extractThumbnail(src, 500, 500, 2);
      }
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
          bitmap.getWidth() * bitmap.getHeight());
      int options = 100;
      bitmap.compress(CompressFormat.JPEG, options, outputStream);
      while ((long) outputStream.size() > 32768L && options > 6) {
        outputStream.reset();
        options -= 6;
        bitmap.compress(CompressFormat.JPEG, options, outputStream);
      }
      bitmap.recycle();
      return outputStream.toByteArray();
    }
  }
}

interface Callback<T> {

  void onCall(T target);
}