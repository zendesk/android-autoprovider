package com.getbase.android.autoprovider;

import com.getbase.android.db.fluentsqlite.Delete;
import com.getbase.android.db.fluentsqlite.Insert;
import com.getbase.android.db.fluentsqlite.QueryBuilder.Query;
import com.getbase.android.db.fluentsqlite.Update;
import com.getbase.autoindexer.DbTableModel;
import com.getbase.forger.thneed.MicroOrmModel;

import android.content.ContentValues;
import android.net.Uri;

public class AutoProvider<TModel extends DbTableModel & MicroOrmModel> {
  private final AutoUris<TModel> mAutoUris;

  public AutoProvider(AutoUris<TModel> autoUris) {
    mAutoUris = autoUris;
  }

  public String getType(Uri uri) {
    return null;
  }

  public Query query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    return null;
  }

  public Insert insert(Uri uri, ContentValues values) {
    return null;
  }

  public Delete delete(Uri uri, String selection, String[] selectionArgs) {
    return null;
  }

  public Update update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return null;
  }
}
