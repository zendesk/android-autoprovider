package com.getbase.android.autoprovider;

import android.database.sqlite.SQLiteDatabase;

import com.getbase.android.sqlitemaster.SQLiteMaster;
import com.getbase.android.sqlitemaster.SQLiteSchemaPart;
import com.getbase.android.sqlitemaster.SQLiteSchemaPartType;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Locale;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class SqliteSchemaHelper {

  private final AutoProviderDatabase mDatabase;

  public SqliteSchemaHelper(AutoProviderDatabase database) {
    mDatabase = database;
  }

  private final Supplier<ImmutableSet<String>> mTables = Suppliers.memoize(new Supplier<ImmutableSet<String>>() {
    @Override
    public ImmutableSet<String> get() {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (SQLiteSchemaPart part : getSQLiteSchemaParts(SQLiteSchemaPartType.TABLE)) {
        builder.add(part.name.toLowerCase(Locale.US));
      }
      return builder.build();
    }
  });

  private final Supplier<ImmutableMap<String, String>> mViewsWithSqlStatements = Suppliers.memoize(new Supplier<ImmutableMap<String, String>>() {
    @Override
    public ImmutableMap<String, String> get() {
      ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
      for (SQLiteSchemaPart part : getSQLiteSchemaParts(SQLiteSchemaPartType.VIEW)) {
        builder.put(part.name.toLowerCase(Locale.US), part.sql.toLowerCase(Locale.US));
      }
      return builder.build();
    }
  });

  private List<SQLiteSchemaPart> getSQLiteSchemaParts(final SQLiteSchemaPartType partType) {
    return mDatabase.execute(new Function<SQLiteDatabase, List<SQLiteSchemaPart>>() {
      @NullableDecl
      @Override
      public List<SQLiteSchemaPart> apply(@NullableDecl SQLiteDatabase input) {
        return SQLiteMaster.getSQLiteSchemaParts(input, partType);
      }
    });
  }

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
