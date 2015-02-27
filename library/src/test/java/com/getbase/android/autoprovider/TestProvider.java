package com.getbase.android.autoprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class TestProvider extends ContentProvider {
  SQLiteOpenHelper mDatabase;

  @Override
  public boolean onCreate() {
    mDatabase = new TestDatabase(getContext());

    return true;
  }

  public SQLiteOpenHelper getDatabase() {
    return mDatabase;
  }

  @Override
  public Cursor query(Uri uri, String[] strings, String s, String[] strings2, String s2) {
    return null;
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues contentValues) {
    return null;
  }

  @Override
  public int delete(Uri uri, String s, String[] strings) {
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
    return 0;
  }
}
