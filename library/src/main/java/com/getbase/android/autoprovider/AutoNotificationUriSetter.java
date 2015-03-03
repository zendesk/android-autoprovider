package com.getbase.android.autoprovider;

import com.getbase.android.db.fluentsqlite.Query.QueryBuilder;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

import org.chalup.thneed.models.DatabaseModel;
import org.chalup.thneed.models.PojoModel;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.Set;

public class AutoNotificationUriSetter<TModel extends DatabaseModel & PojoModel> {

  private final SqliteViewDependenciesResolver mViewDependenciesResolver;
  private final SqliteQueryTablesFinder mTablesFinder;
  private final ContentResolver mContentResolver;
  private final AutoUris<TModel> mAutoUris;
  private final ClassToTable<TModel> mClassToTable;

  private final Function<String, Iterable<String>> mResolveViewsDependencies = new Function<String, Iterable<String>>() {
    @Override
    public Iterable<String> apply(String tableOrView) {
      return mViewDependenciesResolver.getTables(tableOrView);
    }
  };

  private final Function<String, Uri> mGetUriForTable = new Function<String, Uri>() {
    @Override
    public Uri apply(String table) {
      return mAutoUris.model(mClassToTable.getClassForTable(table)).toUri();
    }
  };

  private final Predicate<String> mFilterNonModelTables = new Predicate<String>() {
    @Override
    public boolean apply(String table) {
      return mClassToTable.hasTable(table);
    }
  };

  public AutoNotificationUriSetter(SQLiteOpenHelper database, ContentResolver contentResolver, AutoUris<TModel> autoUris, ClassToTable<TModel> classToTable) {
    SqliteSchemaHelper schemaHelper = new SqliteSchemaHelper(database);
    mViewDependenciesResolver = new SqliteViewDependenciesResolver(schemaHelper);
    mTablesFinder = new SqliteQueryTablesFinder(schemaHelper);
    mContentResolver = contentResolver;
    mClassToTable = classToTable;
    mAutoUris = autoUris;
  }

  public MultiUriCursorWrapper setNotificationUris(Cursor cursor, QueryBuilder query) {
    return new MultiUriCursorWrapper(cursor)
        .withNotificationUris(mContentResolver, getUris(query));
  }

  public MultiUriCursorWrapper setNotificationUris(Cursor cursor, String rawQuery) {
    return new MultiUriCursorWrapper(cursor)
        .withNotificationUris(mContentResolver, getUris(rawQuery));
  }

  public ImmutableSet<Uri> getUris(String rawQuery) {
    return getUris(mTablesFinder.getTablesFromRawSql(rawQuery));
  }

  public ImmutableSet<Uri> getUris(QueryBuilder queryBuilder) {
    return getUris(queryBuilder.getTables());
  }

  private ImmutableSet<Uri> getUris(Set<String> tables) {
    // TODO: add LRU cache?

    return FluentIterable.from(tables)
        .transformAndConcat(mResolveViewsDependencies)
        .filter(mFilterNonModelTables)
        .transform(mGetUriForTable)
        .toSet();
  }
}
