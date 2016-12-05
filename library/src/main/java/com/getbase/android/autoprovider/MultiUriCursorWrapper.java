package com.getbase.android.autoprovider;

import com.google.common.collect.Iterables;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Build;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MultiUriCursorWrapper extends CursorWrapper {

  public MultiUriCursorWrapper(Cursor cursor) {
    super(cursor);
  }

  protected boolean mClosed;
  protected ContentResolver mContentResolver;

  private final Set<Uri> mNotifyUris = new CopyOnWriteArraySet<>();

  private final Object mSelfObserverLock = new Object();
  private ContentObserver mSelfObserver;
  private boolean mSelfObserverRegistered;

  private final LinkedHashSet<Uri> mChangedByUris = new LinkedHashSet<>();

  private final ContentObservable mContentObservable = new ContentObservable();

  @Override
  public void deactivate() {
    onDeactivateOrClose();
    super.deactivate();
  }

  protected void onDeactivateOrClose() {
    if (mSelfObserver != null) {
      mContentResolver.unregisterContentObserver(mSelfObserver);
      mSelfObserverRegistered = false;
    }
  }

  @Override
  public boolean requery() {
    if (mSelfObserver != null && !mSelfObserverRegistered) {
      for (Uri notifyUri : mNotifyUris) {
        mContentResolver.registerContentObserver(notifyUri, true, mSelfObserver);
      }
      mSelfObserverRegistered = true;
    }

    boolean success = super.requery();
    if (success) {
      mChangedByUris.clear();
    }
    return success;
  }

  @Override
  public boolean isClosed() {
    return super.isClosed() && mClosed;
  }

  @Override
  public void close() {
    super.close();

    mClosed = true;
    mContentObservable.unregisterAll();
    onDeactivateOrClose();
  }

  @SuppressWarnings("deprecation")
  @Override
  public void registerContentObserver(ContentObserver observer) {
    mContentObservable.registerObserver(observer);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      dispatchChangeForUris(observer);
    } else {
      if (!mChangedByUris.isEmpty()) {
        observer.dispatchChange(false);
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private void dispatchChangeForUris(ContentObserver observer) {
    for (Uri changedByUri : new ArrayList<>(mChangedByUris)) {
      observer.dispatchChange(false, changedByUri);
    }
  }

  @Override
  public void unregisterContentObserver(ContentObserver observer) {
    // cursor will unregister all observers when it close
    if (!mClosed) {
      mContentObservable.unregisterObserver(observer);
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @SuppressWarnings("deprecation")
  private void onChange(boolean selfChange, Uri uri) {
    synchronized (mSelfObserverLock) {

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        mContentObservable.dispatchChange(selfChange, uri);
      } else {
        mContentObservable.dispatchChange(selfChange);
      }

      if (selfChange) {
        for (Uri notifyUri : mNotifyUris) {
          mContentResolver.notifyChange(notifyUri, mSelfObserver);
        }
      }

      if (!selfChange) {
        mChangedByUris.add(uri);
      }
    }
  }

  public MultiUriCursorWrapper withNotificationUris(ContentResolver cr, Collection<Uri> uris) {
    synchronized (mSelfObserverLock) {
      mNotifyUris.addAll(uris);
      mContentResolver = cr;
      if (mSelfObserver == null) {
        mSelfObserver = new SelfContentObserver(this);
      }

      for (Uri uri : uris) {
        mContentResolver.registerContentObserver(uri, true, mSelfObserver);
      }

      mSelfObserverRegistered = true;
    }

    return this;
  }

  public MultiUriCursorWrapper withNotificationUri(ContentResolver cr, Uri uri) {
    return withNotificationUris(cr, Collections.singletonList(uri));
  }

  public Set<Uri> getNotificationUris() {
    return mNotifyUris;
  }

  @Override
  public void setNotificationUri(ContentResolver cr, Uri notifyUri) {
    withNotificationUri(cr, notifyUri);
  }

  @Override
  public Uri getNotificationUri() {
    synchronized (mSelfObserverLock) {
      return Iterables.getFirst(mNotifyUris, null);
    }
  }

  @Override
  protected void finalize() {
    try {
      super.finalize();

      if (mSelfObserver != null && mSelfObserverRegistered) {
        mContentResolver.unregisterContentObserver(mSelfObserver);
      }

      if (!mClosed) close();
    } catch (Throwable e) {
      // ignored
    }
  }

  private static class SelfContentObserver extends ContentObserver {
    WeakReference<MultiUriCursorWrapper> mCursor;

    public SelfContentObserver(MultiUriCursorWrapper cursor) {
      super(null);
      mCursor = new WeakReference<>(cursor);
    }

    @Override
    public boolean deliverSelfNotifications() {
      return false;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
      MultiUriCursorWrapper cursor = mCursor.get();
      if (cursor != null) {
        cursor.onChange(false, uri);
      }
    }

    @Override
    public void onChange(boolean selfChange) {
      onChange(selfChange, null);
    }
  }
}
