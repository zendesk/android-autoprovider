package com.getbase.android.autoprovider;

import static com.getbase.android.db.fluentsqlite.Query.select;

import com.getbase.android.db.fluentsqlite.Delete;
import com.getbase.android.db.fluentsqlite.Insert;
import com.getbase.android.db.fluentsqlite.Query.QueryBuilder;
import com.getbase.android.db.fluentsqlite.Update;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.chalup.thneed.models.DatabaseModel;
import org.chalup.thneed.models.PojoModel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Map;

public abstract class CompositeUriHandler<T extends DatabaseModel & PojoModel> extends BaseContentUriHandler {

  private final AutoNotificationUriSetter mAutoNotificationUriSetter;

  private final ClassToTable<T> mClassToTable;
  private final String mAuthority;

  public CompositeUriHandler(String authority, SQLiteOpenHelper database, ContentResolver contentResolver, ClassToTable<T> classToTable, AutoNotificationUriSetter<T> autoNotificationUriSetter) {
    super(database, contentResolver);
    mAuthority = authority;
    mAutoNotificationUriSetter = autoNotificationUriSetter;
    mClassToTable = classToTable;
  }

  protected MultiUriCursorWrapper performAndSetAutoUri(QueryBuilder queryBuilder) {
    return mAutoNotificationUriSetter.setNotificationUris(queryBuilder.perform(getReadableDb()), queryBuilder);
  }

  protected MultiUriCursorWrapper performAndSetAutoUri(String rawSql, String... selectionArgs) {
    return mAutoNotificationUriSetter.setNotificationUris(getReadableDb().rawQuery(rawSql, selectionArgs), rawSql);
  }

  private final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

  private Map<Integer, Endpoint> mEndpointMap = Maps.newHashMap();

  public long insertOrUpdate(Long localId, SQLiteDatabase db, ContentValues values, String tableName) {
    long id;

    if (localId != null) {
      db.update(tableName, values, BaseColumns._ID + "=?", new String[] { String.valueOf(localId) });
      id = localId;
    } else {
      id = db.insertOrThrow(tableName, null, values);
    }
    return id;
  }

  public static class Endpoint {
    private final ContentResolver mContentResolver;
    private final QueryHandler mQueryHandler;
    private final InsertHandler mInsertHandler;
    private final UpdateHandler mUpdateHandler;
    private final DeleteHandler mDeleteHandler;
    private final Uri[] mNotifyUris;
    private final TypeHandler mTypeHandler;

    public Endpoint(ContentResolver contentResolver, QueryHandler queryHandler, InsertHandler insertHandler, UpdateHandler updateHandler, DeleteHandler deleteHandler, TypeHandler typeHandler, Uri[] notifyUris) {
      mContentResolver = contentResolver;
      mQueryHandler = queryHandler;
      mInsertHandler = insertHandler;
      mUpdateHandler = updateHandler;
      mDeleteHandler = deleteHandler;
      mTypeHandler = typeHandler;
      mNotifyUris = notifyUris;
    }

    private void notifyUris(Uri mainUri) {
      mContentResolver.notifyChange(mainUri, null, false);
      for (Uri uri : mNotifyUris) {
        mContentResolver.notifyChange(uri, null, false);
      }
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
      return mQueryHandler.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
      int update = mUpdateHandler.update(uri, values, selection, selectionArgs);
      if (update > 0) notifyUris(uri);
      return update;
    }

    public Uri insert(Uri uri, ContentValues values) {
      Uri insert = mInsertHandler.insert(uri, values);
      notifyUris(uri);
      return insert;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
      int delete = mDeleteHandler.delete(uri, selection, selectionArgs);
      if (delete > 0) notifyUris(uri);
      return delete;
    }
  }

  protected void addEndpoint(PathBuilder pathBuilder, EndpointBuilder endpointBuilder) {
    final int index = mEndpointMap.size();
    mUriMatcher.addURI(mAuthority, pathBuilder.build(), index);
    mEndpointMap.put(index, endpointBuilder.build());
  }

  protected PathBuilder<T> model(Class<?> klass) {
    return new PathBuilder<>(mClassToTable).model(klass);
  }

  protected PathBuilder<T> path(String path) {
    return new PathBuilder<>(mClassToTable).path(path);
  }

  @Override
  public boolean canHandle(Uri uri, ContentUriAction action) {
    Endpoint endpointHandler = getEndpointHandler(uri);
    if (endpointHandler == null) {
      return false;
    }

    switch (action) {
    case QUERY:
      return endpointHandler.mQueryHandler != null;
    case UPDATE:
      return endpointHandler.mUpdateHandler != null;
    case INSERT:
      return endpointHandler.mInsertHandler != null;
    case DELETE:
      return endpointHandler.mDeleteHandler != null;
    case GET_TYPE:
      return endpointHandler.mTypeHandler != null;
    default:
      throw new IllegalStateException(action.toString());
    }
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    return getEndpointHandler(uri).query(uri, projection, selection, selectionArgs, sortOrder);
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return getEndpointHandler(uri).insert(uri, values);
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return getEndpointHandler(uri).update(uri, values, selection, selectionArgs);
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return getEndpointHandler(uri).delete(uri, selection, selectionArgs);
  }

  @Override
  public String getType(Uri uri) {
    TypeHandler typeHandler = getEndpointHandler(uri).mTypeHandler;
    Preconditions.checkNotNull(typeHandler, "Uri not handled: " + uri);
    return typeHandler.getType(uri);
  }

  Endpoint getEndpointHandler(Uri uri) {
    return mEndpointMap.get(mUriMatcher.match(uri));
  }

  protected EndpointBuilder handle() {
    return new EndpointBuilderImpl();
  }

  protected interface EndpointBuilder {
    Endpoint build();
    EndpointBuilder insert(InsertHandler insertHandler);
    EndpointBuilder query(QueryHandler queryHandler);
    EndpointBuilder update(UpdateHandler updateHandler);
    EndpointBuilder delete(DeleteHandler deleteHandler);
    EndpointBuilder type(String type);
    EndpointBuilder type(TypeHandler typeHandler);
    EndpointBuilder query(String table);
    EndpointBuilder query(QueryBuilder queryBuilder);
  }

  protected class EndpointBuilderImpl implements EndpointBuilder {
    private QueryHandler mQueryHandler;
    private InsertHandler mInsertHandler;
    private UpdateHandler mUpdateHandler;
    private DeleteHandler mDeleteHandler;
    private TypeHandler mTypeHandler;
    private Uri[] mNotifyUris = new Uri[0];

    private EndpointBuilderImpl() {
    }

    @Override
    public Endpoint build() {
      return new Endpoint(getContentResolver(), mQueryHandler, mInsertHandler, mUpdateHandler, mDeleteHandler, mTypeHandler, mNotifyUris);
    }

    @Override
    public EndpointBuilder insert(InsertHandler insertHandler) {
      Preconditions.checkState(mInsertHandler == null, "handler already set");
      mInsertHandler = insertHandler;
      return this;
    }

    @Override
    public EndpointBuilder query(QueryHandler queryHandler) {
      Preconditions.checkState(mQueryHandler == null, "handler already set");
      mQueryHandler = queryHandler;
      return this;
    }

    @Override
    public EndpointBuilder update(UpdateHandler updateHandler) {
      Preconditions.checkState(mUpdateHandler == null, "handler already set");
      mUpdateHandler = updateHandler;
      return this;
    }

    @Override
    public EndpointBuilder delete(DeleteHandler deleteHandler) {
      Preconditions.checkState(mDeleteHandler == null, "handler already set");
      mDeleteHandler = deleteHandler;
      return this;
    }

    @Override
    public EndpointBuilder type(String type) {
      return type(new ConstantTypeHandler(type));
    }

    @Override
    public EndpointBuilder type(TypeHandler typeHandler) {
      Preconditions.checkState(mTypeHandler == null, "type handler already set");
      mTypeHandler = typeHandler;
      return this;
    }

    @Override
    public EndpointBuilder query(String table) {
      return query(new SimpleTableQuery(table));
    }

    @Override
    public EndpointBuilder query(QueryBuilder queryBuilder) {
      return query(new FluentQueryHandler(queryBuilder));
    }
  }

  public static abstract class QueryHandler {
    public abstract Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);
  }

  public static abstract class InsertHandler {
    public abstract Uri insert(Uri uri, ContentValues values);
  }

  public static abstract class UpdateHandler {
    public abstract int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);
  }

  public static abstract class DeleteHandler {
    public abstract int delete(Uri uri, String selection, String[] selectionArgs);
  }

  public static abstract class TypeHandler {
    public abstract String getType(Uri uri);
  }

  private class ConstantTypeHandler extends TypeHandler {

    private final String mConstantType;

    private ConstantTypeHandler(String constantType) {
      mConstantType = constantType;
    }

    @Override
    public String getType(Uri uri) {
      return mConstantType;
    }
  }

  public class FluentQueryHandler extends QueryHandler {

    private final QueryBuilder mQueryBuilder;

    public FluentQueryHandler() {
      mQueryBuilder = null;
    }

    public FluentQueryHandler(QueryBuilder queryBuilder) {
      mQueryBuilder = queryBuilder;
    }

    public QueryBuilder getQueryBuilder(Uri uri) {
      Preconditions.checkNotNull(mQueryBuilder, "You must override getQuery() method if you do not provide Query instance to the constructor");
      return mQueryBuilder.build().buildUpon();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
      return performAndSetAutoUri(
          getQueryBuilder(uri)
              .columns(projection)
              .where(selection, selectionArgs)
              .orderBy(sortOrder)
      );
    }
  }

  public abstract class FluentInsertHandler extends InsertHandler {
    private final Uri[] mNotifyUris;

    public FluentInsertHandler(Uri... notifyUris) {
      mNotifyUris = notifyUris;
    }

    public abstract Insert getInsert(Uri uri);

    @Override
    public Uri insert(Uri uri, ContentValues values) {
      long id = getInsert(uri).values(values).perform(getWritableDb());
      if (mNotifyUris.length > 0) {
        for (Uri notifyUri : mNotifyUris) {
          getContentResolver().notifyChange(notifyUri, null, false);
        }
      }
      return Uri.withAppendedPath(uri, String.valueOf(id));
    }
  }

  public abstract class FluentUpdateHandler extends UpdateHandler {
    private final Uri[] mNotifyUris;

    public FluentUpdateHandler(Uri... notifyUris) {
      mNotifyUris = notifyUris;
    }

    public abstract Update getUpdate(Uri uri);

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
      Update update = getUpdate(uri).where(selection, selectionArgs).values(values);

      int ret = update.perform(getWritableDb());
      if (ret > 0 && mNotifyUris.length > 0) {
        for (Uri notifyUri : mNotifyUris) {
          getContentResolver().notifyChange(notifyUri, null, false);
        }
      }
      return ret;
    }
  }

  public abstract class FluentDeleteHandler extends DeleteHandler {
    private final Uri[] mNotifyUris;

    public FluentDeleteHandler(Uri... notifyUris) {
      mNotifyUris = notifyUris;
    }

    public abstract Delete getDelete(Uri uri);

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
      Delete delete = getDelete(uri).where(selection, selectionArgs);

      int ret = delete.perform(getWritableDb());
      if (ret > 0 && mNotifyUris.length > 0) {
        for (Uri notifyUri : mNotifyUris) {
          getContentResolver().notifyChange(notifyUri, null, false);
        }
      }
      return ret;
    }
  }

  // standard handlers
  public class SimpleTableQuery extends FluentQueryHandler {
    protected final String mTable;

    public SimpleTableQuery(String table) {
      super(select().from(table));
      mTable = table;
    }

    @Override
    public QueryBuilder getQueryBuilder(Uri uri) {
      QueryBuilder queryBuilder = super.getQueryBuilder(uri);
      try {
        long id = Long.parseLong(uri.getLastPathSegment());
        queryBuilder = queryBuilder.where(BaseColumns._ID + "=?", id);
      } catch (NumberFormatException ignored) {
      }
      return queryBuilder;
    }
  }

  private static final ContentValues EMPTY_VALUES = new ContentValues();

  public class SimpleTableInsert extends FluentInsertHandler {
    private final String mTable;

    public SimpleTableInsert(String table, Uri... notifyUris) {
      super(notifyUris);
      mTable = table;
    }

    @Override
    public Insert getInsert(Uri uri) {
      return Insert.insert().into(mTable).values(EMPTY_VALUES);
    }
  }

  public class SimpleTableUpdate extends FluentUpdateHandler {
    private String mTable;

    public SimpleTableUpdate(String table, Uri... notifyUris) {
      super(notifyUris);
      mTable = table;
    }

    @Override
    public Update getUpdate(Uri uri) {
      Update update = Update.update().table(mTable);
      try {
        long id = Long.parseLong(uri.getLastPathSegment());
        update = update.where(BaseColumns._ID + "=?", id);
      } catch (NumberFormatException ignored) {
      }
      return update;
    }
  }
}
