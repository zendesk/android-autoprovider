package com.getbase.android.autoprovider;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.getbase.android.db.cursors.FluentCursor;
import com.getbase.android.db.fluentsqlite.Delete;
import com.getbase.android.db.fluentsqlite.Insert;
import com.getbase.android.db.fluentsqlite.Query;
import com.getbase.android.db.fluentsqlite.Update;
import com.google.common.base.Function;

import androidx.annotation.NonNull;

class DefaultDatabase implements AutoProviderDatabase {
  @NonNull
  private final SQLiteOpenHelper mSqliteOpenHelper;

  DefaultDatabase(@NonNull SQLiteOpenHelper sqLiteOpenHelper) {
    this.mSqliteOpenHelper = sqLiteOpenHelper;
  }

  @Override
  @NonNull
  public <T> T execute(@NonNull Function<SQLiteDatabase, T> function) {
    return function.apply(mSqliteOpenHelper.getWritableDatabase());
  }

  @Override
  @NonNull
  public FluentCursor query(@NonNull Query.QueryBuilder queryBuilder) {
    return queryBuilder.perform(mSqliteOpenHelper.getReadableDatabase());
  }

  @Override
  public int delete(@NonNull Delete delete) {
    return delete.perform(mSqliteOpenHelper.getWritableDatabase());
  }

  @Override
  public long insertOrThrow(@NonNull Insert insert) {
    return insert.perform(mSqliteOpenHelper.getWritableDatabase());
  }

  @Override
  public int update(@NonNull Update update) {
    return update.perform(mSqliteOpenHelper.getWritableDatabase());
  }
}
