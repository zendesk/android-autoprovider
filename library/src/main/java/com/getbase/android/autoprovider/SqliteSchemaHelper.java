package com.getbase.android.autoprovider;

import com.getbase.android.sqlitemaster.SQLiteMaster;
import com.getbase.android.sqlitemaster.SQLiteSchemaPart;
import com.getbase.android.sqlitemaster.SQLiteSchemaPartType;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import android.database.sqlite.SQLiteOpenHelper;

import java.util.Locale;

public class SqliteSchemaHelper {

  private final SQLiteOpenHelper mDatabase;

  public SqliteSchemaHelper(SQLiteOpenHelper database) {
    mDatabase = database;
  }

  private final Supplier<ImmutableSet<String>> mTables = Suppliers.memoize(new Supplier<ImmutableSet<String>>() {
    @Override
    public ImmutableSet<String> get() {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (SQLiteSchemaPart part : SQLiteMaster.getSQLiteSchemaParts(mDatabase.getReadableDatabase(), SQLiteSchemaPartType.TABLE)) {
        builder.add(part.name.toLowerCase(Locale.US));
      }
      return builder.build();
    }
  });

  private final Supplier<ImmutableMap<String, String>> mViewsWithSqlStatements = Suppliers.memoize(new Supplier<ImmutableMap<String, String>>() {
    @Override
    public ImmutableMap<String, String> get() {
      ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
      for (SQLiteSchemaPart part : SQLiteMaster.getSQLiteSchemaParts(mDatabase.getReadableDatabase(), SQLiteSchemaPartType.VIEW)) {
        builder.put(part.name.toLowerCase(Locale.US), part.sql.toLowerCase(Locale.US));
      }
      return builder.build();
    }
  });

  public ImmutableSet<String> getTables() {
    return mTables.get();
  }

  public ImmutableSet<String> getViews() {
    return mViewsWithSqlStatements.get().keySet();
  }

  public String getViewCreateStatement(String view) {
    return mViewsWithSqlStatements.get().get(view);
  }
}
