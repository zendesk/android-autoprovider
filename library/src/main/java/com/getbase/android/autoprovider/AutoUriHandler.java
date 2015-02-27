package com.getbase.android.autoprovider;

import com.getbase.android.db.fluentsqlite.Delete;
import com.getbase.android.db.fluentsqlite.Query.QueryBuilder;
import com.getbase.android.db.fluentsqlite.Update;
import com.getbase.autoindexer.DbTableModel;
import com.getbase.forger.thneed.MicroOrmModel;

import org.chalup.thneed.ModelGraph;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import java.util.EnumSet;

public class AutoUriHandler<TModel extends DbTableModel & MicroOrmModel> extends BaseContentUriHandler {
  private final AutoUris<TModel> mAutoUris;
  private final ContentTypeVisitor mContentTypeVisitor;
  private final CrudOperationsResolver mCrudOperationsResolver;
  private final AutoUriVisitor<EnumSet<ContentUriAction>> mSupportedActionsVisitor;
  private final AutoNotificationUriSetter mAutoNotificationUriSetter;

  public AutoUriHandler(SQLiteOpenHelper database, ContentResolver contentResolver, AutoUris<TModel> autoUris, ContentTypeProvider<TModel> contentTypeProvider, ModelGraph<TModel> modelGraph, AutoNotificationUriSetter<TModel> autoNotificationUriSetter) {
    super(database, contentResolver);
    mAutoUris = autoUris;
    mAutoNotificationUriSetter = autoNotificationUriSetter;

    mContentTypeVisitor = new ContentTypeVisitor(contentTypeProvider);
    mCrudOperationsResolver = new CrudOperationsResolver(database, modelGraph);

    mSupportedActionsVisitor = new AutoUriVisitor<EnumSet<ContentUriAction>>() {
      @Override
      public EnumSet<ContentUriAction> visit(EntityUri uri) {
        return EnumSet.complementOf(EnumSet.of(ContentUriAction.INSERT));
      }

      @Override
      public EnumSet<ContentUriAction> visit(ModelUri uri) {
        return EnumSet.allOf(ContentUriAction.class);
      }

      @Override
      public EnumSet<ContentUriAction> visit(CustomUri uri) {
        return EnumSet.noneOf(ContentUriAction.class);
      }
    };
  }

  public String getType(Uri uri) {
    return mAutoUris.getAutoUri(uri).accept(mContentTypeVisitor);
  }

  @Override
  public boolean canHandle(Uri uri, ContentUriAction action) {
    return mAutoUris
        .getAutoUri(uri)
        .accept(mSupportedActionsVisitor)
        .contains(action);
  }

  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    QueryBuilder queryBuilder = mAutoUris.getAutoUri(uri).accept(mCrudOperationsResolver.getQueryBuilderVisitor())
        .columns(projection);
    if (!TextUtils.isEmpty(selection)) {
      queryBuilder.where(selection, selectionArgs);
    }
    if (!TextUtils.isEmpty(sortOrder)) {
      queryBuilder.orderBy(sortOrder);
    }
    return mAutoNotificationUriSetter.setNotificationUris(
        queryBuilder.perform(getReadableDb()),
        queryBuilder
    );
  }

  public Uri insert(Uri uri, ContentValues values) {
    AutoUri autoUri = mAutoUris.getAutoUri(uri);
    long id = autoUri.accept(mCrudOperationsResolver.getInsertVisitor())
        .values(values)
        .performOrThrow(getWritableDb());
    notifyChange(autoUri);
    return mAutoUris.model(autoUri.getModel()).id(id).toUri();
  }

  public int delete(Uri uri, String selection, String[] selectionArgs) {
    AutoUri autoUri = mAutoUris.getAutoUri(uri);
    Delete delete = autoUri.accept(mCrudOperationsResolver.getDeleteVisitor());
    if (!TextUtils.isEmpty(selection)) {
      delete = delete.where(selection, selectionArgs);
    }
    int count = delete.perform(getWritableDb());
    if (count > 0) {
      notifyChange(autoUri);
    }
    return count;
  }

  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    AutoUri autoUri = mAutoUris.getAutoUri(uri);
    Update update = autoUri.accept(mCrudOperationsResolver.getUpdateVisitor())
        .values(values);
    if (!TextUtils.isEmpty(selection)) {
      update = update.where(selection, selectionArgs);
    }
    int count = update.perform(getWritableDb());
    if (count > 0) {
      notifyChange(autoUri);
    }
    return count;
  }

  private void notifyChange(AutoUri autoUri) {
    getContentResolver().notifyChange(mAutoUris.model(autoUri.getModel()).toUri(), null, false);
  }
}
