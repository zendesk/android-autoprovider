package com.getbase.android.autoprovider;

import android.database.sqlite.SQLiteDatabase;

import com.getbase.android.db.cursors.FluentCursor;
import com.getbase.android.db.fluentsqlite.Delete;
import com.getbase.android.db.fluentsqlite.Insert;
import com.getbase.android.db.fluentsqlite.Query;
import com.getbase.android.db.fluentsqlite.Update;
import com.google.common.base.Function;

import androidx.annotation.NonNull;

interface AutoProviderDatabase {
  @NonNull
  <T> T execute(@NonNull Function<SQLiteDatabase, T> function);

  @NonNull
  FluentCursor query(@NonNull Query.QueryBuilder queryBuilder);

  int delete(@NonNull Delete delete);

  long insertOrThrow(@NonNull Insert insert);

  int update(@NonNull Update update);
}


