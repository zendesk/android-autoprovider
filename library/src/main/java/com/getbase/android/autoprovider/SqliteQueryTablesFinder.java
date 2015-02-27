package com.getbase.android.autoprovider;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.util.Locale;

public class SqliteQueryTablesFinder {
  private final SqliteSchemaHelper mSchemaHelper;

  public SqliteQueryTablesFinder(SqliteSchemaHelper schemaHelper) {
    mSchemaHelper = schemaHelper;
  }

  public ImmutableSet<String> getTablesFromRawSql(String rawSql) {
    final String sql = rawSql.toLowerCase(Locale.US);

    return FluentIterable
        .from(Iterables.concat(mSchemaHelper.getTables(), mSchemaHelper.getViews()))
        .filter(new Predicate<String>() {
          @Override
          public boolean apply(String tableOrView) {
            return sql.contains(tableOrView);
          }
        })
        .toSet();
  }
}
