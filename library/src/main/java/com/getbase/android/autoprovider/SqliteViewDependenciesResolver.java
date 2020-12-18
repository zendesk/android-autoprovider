package com.getbase.android.autoprovider;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Locale;

import androidx.annotation.NonNull;

public class SqliteViewDependenciesResolver {
  private final LoadingCache<String, ImmutableSet<String>> mDependencies = CacheBuilder.newBuilder().build(
      new CacheLoader<String, ImmutableSet<String>>() {

        @Override
        public ImmutableSet<String> load(@NonNull String viewOrTableName) throws Exception {
          viewOrTableName = viewOrTableName.toLowerCase(Locale.US);

          if (mSchemaHelper.getTables().contains(viewOrTableName)) {
            return ImmutableSet.of(viewOrTableName);
          }

          Preconditions.checkArgument(mSchemaHelper.getViews().contains(viewOrTableName));

          HashSet<String> result = Sets.newHashSet();
          String viewSqlStatement = mSchemaHelper.getViewCreateStatement(viewOrTableName);
          for (String table : mSchemaHelper.getTables()) {
            if (viewSqlStatement.contains(table)) {
              result.add(table);
            }
          }

          for (String view : mSchemaHelper.getViews()) {
            if (!view.equals(viewOrTableName) && viewSqlStatement.contains(view)) {
              result.addAll(load(view));
            }
          }

          return ImmutableSet.copyOf(result);
        }
      }
  );

  private final SqliteSchemaHelper mSchemaHelper;

  public SqliteViewDependenciesResolver(SqliteSchemaHelper schemaHelper) {
    mSchemaHelper = schemaHelper;
  }

  public ImmutableSet<String> getTables(String viewOrTableName) {
    return mDependencies.getUnchecked(viewOrTableName);
  }
}
