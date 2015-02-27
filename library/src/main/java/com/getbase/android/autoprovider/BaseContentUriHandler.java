package com.getbase.android.autoprovider;

import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class BaseContentUriHandler implements ContentUriHandler {
  private final SQLiteOpenHelper mDatabase;
  private final ContentResolver mContentResolver;

  public BaseContentUriHandler(SQLiteOpenHelper database, ContentResolver contentResolver) {
    mDatabase = database;
    mContentResolver = contentResolver;
  }

  protected ContentResolver getContentResolver() {
    return mContentResolver;
  }

  protected SQLiteDatabase getReadableDb() {
    return mDatabase.getReadableDatabase();
  }

  protected SQLiteDatabase getWritableDb() {
    return mDatabase.getWritableDatabase();
  }
}
