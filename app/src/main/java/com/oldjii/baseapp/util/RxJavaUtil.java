package com.oldjii.baseapp.util;

import android.util.Log;
import com.sina.weibo.BuildConfig;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class RxJavaUtil {

  private RxJavaUtil() {}

  /**
   * 带回调的异步任务
   *
   * @param callable 任务
   * @param successCall 成功的回调
   * @param <T> 回调时的参数类型
   */
  public static <T> void asyncDo(final Callable<T> callable, Consumer<T> successCall) {
    Disposable subscribe =
        Observable.unsafeCreate(
                (ObservableSource<T>)
                    observer -> {
                      try {
                        T call = callable.call();
                        observer.onNext(call);
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                      observer.onComplete();
                    })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                successCall,
                throwable -> {
                  throwable.printStackTrace();
                  if (BuildConfig.DEBUG) {
                    throw new RuntimeException(throwable.getMessage());
                  }
                  Log.e("TAG", "rxJava 抛出异常了，请检查调用代码或者检查数据库升级是否正确!");
                });
  }

  public static <T> void asyncDelayDo(
      long delay, final Callable<T> callable, Consumer<T> successCall) {
    Disposable subscribe =
        Observable.unsafeCreate(
                (ObservableSource<T>)
                    observer -> {
                      try {
                        T call = callable.call();
                        observer.onNext(call);
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                      observer.onComplete();
                    })
            .delay(200, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                successCall,
                throwable -> {
                  throwable.printStackTrace();
                  if (BuildConfig.DEBUG) {
                    throw new RuntimeException(throwable.getMessage());
                  }
                  Log.e("TAG", "rxJava 抛出异常了，请检查调用代码或者检查数据库升级是否正确!");
                });
  }

  /**
   * 不需要回调的异步任务
   *
   * @param runnable 任务
   */
  public static void asyncDo(final Runnable runnable) {
    Disposable subscribe =
        Observable.unsafeCreate(
                observer -> {
                  try {
                    runnable.run();
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                  observer.onComplete();
                })
            .subscribeOn(Schedulers.io())
            .subscribe(
                o -> {},
                throwable -> {
                  throwable.printStackTrace();
                  if (BuildConfig.DEBUG) {
                    throw new RuntimeException(throwable.getMessage());
                  }
                  Log.e("TAG", "rxJava 抛出异常了，请检查调用代码或者检查数据库升级是否正确!");
                });
  }
}
