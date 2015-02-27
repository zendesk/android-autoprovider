package com.getbase.android.autoprovider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public interface ContentUriHandler {
  public boolean canHandle(Uri uri, ContentUriAction action);
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);
  public Uri insert(Uri uri, ContentValues values);
  public int delete(Uri uri, String selection, String[] selectionArgs);
  public String getType(Uri uri);

  public static enum ContentUriAction {
    QUERY,
    UPDATE,
    INSERT,
    DELETE,
    GET_TYPE
  }
}
