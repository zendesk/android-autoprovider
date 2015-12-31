package com.getbase.android.autoprovider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public interface ContentUriHandler {
  boolean canHandle(Uri uri, ContentUriAction action);
  Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);
  int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);
  Uri insert(Uri uri, ContentValues values);
  int delete(Uri uri, String selection, String[] selectionArgs);
  String getType(Uri uri);

  enum ContentUriAction {
    QUERY,
    UPDATE,
    INSERT,
    DELETE,
    GET_TYPE
  }
}
