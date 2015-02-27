package com.getbase.android.autoprovider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class FailingUriHandler implements ContentUriHandler {
  @Override
  public boolean canHandle(Uri uri, ContentUriAction action) {
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    return fail(uri);
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return fail(uri);
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return fail(uri);
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return fail(uri);
  }

  @Override
  public String getType(Uri uri) {
    return fail(uri);
  }

  private <T> T fail(Uri uri) {
    throw new UnsupportedOperationException("Unsupported uri: " + uri);
  }
}
