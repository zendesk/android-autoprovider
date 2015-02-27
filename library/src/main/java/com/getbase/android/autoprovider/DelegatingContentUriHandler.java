package com.getbase.android.autoprovider;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

public class DelegatingContentUriHandler implements ContentUriHandler {
  private static final String TAG = DelegatingContentUriHandler.class.getSimpleName();

  private final LinkedList<ContentUriHandler> mHandlers = Lists.newLinkedList();

  private ContentUriHandler getHandler(Uri uri, ContentUriAction action) {
    return getContentUriHandler(mHandlers.iterator(), uri, action);
  }

  private ContentUriHandler getContentUriHandler(Iterator<ContentUriHandler> iterator, Uri uri, ContentUriAction action) {
    Preconditions.checkState(iterator.hasNext());

    while (iterator.hasNext()) {
      ContentUriHandler handler = iterator.next();
      if (handler.canHandle(uri, action)) {
        Log.d(TAG, handler.getClass().getSimpleName() + " will handle " + action + " on " + Uri.decode(uri.toString()));
        return handler;
      }
    }

    return null;
  }

  public void addHandlers(ContentUriHandler... handlers) {
    Collections.addAll(mHandlers, handlers);
  }

  public ContentUriHandler getDelegateHandler(Uri uri, ContentUriAction action, ContentUriHandler skipPast) {
    return getContentUriHandler(advancePast(mHandlers.iterator(), skipPast), uri, action);
  }

  private static <T> Iterator<T> advancePast(Iterator<T> iterator, T element) {
    while (iterator.hasNext()) {
      if (iterator.next() == element) {
        break;
      }
    }

    return iterator;
  }

  @Override
  public boolean canHandle(Uri uri, ContentUriAction action) {
    return getHandler(uri, action) != null;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    return getHandler(uri, ContentUriAction.QUERY).query(uri, projection, selection, selectionArgs, sortOrder);
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return getHandler(uri, ContentUriAction.UPDATE).update(uri, values, selection, selectionArgs);
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return getHandler(uri, ContentUriAction.INSERT).insert(uri, values);
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return getHandler(uri, ContentUriAction.DELETE).delete(uri, selection, selectionArgs);
  }

  @Override
  public String getType(Uri uri) {
    return getHandler(uri, ContentUriAction.GET_TYPE).getType(uri);
  }
}
