package com.getbase.android.autoprovider;

import com.getbase.android.db.provider.CrudHandler;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import java.util.ArrayList;

public class ContentUriCrudHandler implements CrudHandler {

  public static ContentUriCrudHandler wrap(ContentUriHandler handler) {
    return new ContentUriCrudHandler(handler);
  }

  private final ContentUriHandler mContentUriHandler;

  public ContentUriCrudHandler(ContentUriHandler contentUriHandler) {
    mContentUriHandler = contentUriHandler;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) throws RemoteException {
    return mContentUriHandler.query(uri, projection, selection, selectionArgs, orderBy);
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) throws RemoteException {
    return mContentUriHandler.delete(uri, selection, selectionArgs);
  }

  @Override
  public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) throws RemoteException {
    return mContentUriHandler.update(uri, contentValues, selection, selectionArgs);
  }

  @Override
  public Uri insert(Uri uri, ContentValues contentValues) throws RemoteException {
    return mContentUriHandler.insert(uri, contentValues);
  }

  @Override
  public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations) throws RemoteException, OperationApplicationException {
    throw new UnsupportedOperationException("applyBatch is unsupported for ContentUriHandler");
  }
}
