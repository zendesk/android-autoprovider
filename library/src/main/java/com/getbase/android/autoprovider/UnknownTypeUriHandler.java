package com.getbase.android.autoprovider;

import com.google.common.base.Objects;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class UnknownTypeUriHandler implements ContentUriHandler {

  private final DelegatingContentUriHandler mDelegatingContentUriHandler;
  private final String mAuthority;
  private final String mUnknownContentType;

  public UnknownTypeUriHandler(DelegatingContentUriHandler delegatingContentUriHandler, String authority, String contentTypePrefix) {
    mDelegatingContentUriHandler = delegatingContentUriHandler;
    mAuthority = authority;
    mUnknownContentType = "application/vnd." + contentTypePrefix + ".unknown";
  }

  @Override
  public boolean canHandle(Uri uri, ContentUriAction action) {
    return action == ContentUriAction.GET_TYPE;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    return fail();
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return fail();
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return fail();
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return fail();
  }

  @Override
  public String getType(Uri uri) {
    return (Objects.equal(uri.getAuthority(), mAuthority))
        ? mUnknownContentType
        : mDelegatingContentUriHandler.getDelegateHandler(uri, ContentUriAction.GET_TYPE, this).getType(uri);
  }

  private <T> T fail() {
    throw new UnsupportedOperationException();
  }
}
