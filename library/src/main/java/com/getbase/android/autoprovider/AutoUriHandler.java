package com.getbase.android.autoprovider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.getbase.android.db.fluentsqlite.Delete;
import com.getbase.android.db.fluentsqlite.Query.QueryBuilder;
import com.getbase.android.db.fluentsqlite.Update;

import java.util.EnumSet;

import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.models.DatabaseModel;
import org.chalup.thneed.models.PojoModel;

public class AutoUriHandler<TModel extends DatabaseModel & PojoModel> implements ContentUriHandler {
  private final AutoUris<TModel> mAutoUris;
  private final ContentTypeVisitor mContentTypeVisitor;
  private final CrudOperationsResolver mCrudOperationsResolver;
  private final AutoUriVisitor<EnumSet<ContentUriAction>> mSupportedActionsVisitor;
  private final AutoNotificationUriSetter mAutoNotificationUriSetter;
  private final AutoProviderDatabase mDatabase;
  private final ContentResolver mContentResolver;

  public AutoUriHandler(AutoProviderDatabase database, ContentResolver contentResolver, AutoUris<TModel> autoUris, ContentTypeProvider<TModel> contentTypeProvider, ModelGraph<TModel> modelGraph, AutoNotificationUriSetter<TModel> autoNotificationUriSetter) {
    mDatabase = database;
    mContentResolver = contentResolver;
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
        mDatabase.query(queryBuilder),
        queryBuilder
    );
  }

  public Uri insert(Uri uri, ContentValues values) {
    AutoUri autoUri = mAutoUris.getAutoUri(uri);
    long id = mDatabase.insertOrThrow(autoUri.accept(mCrudOperationsResolver.getInsertVisitor())
        .values(values));
    notifyChange(autoUri);
    return mAutoUris.model(autoUri.getModel()).id(id).toUri();
  }

  public int delete(Uri uri, String selection, String[] selectionArgs) {
    AutoUri autoUri = mAutoUris.getAutoUri(uri);
    Delete delete = autoUri.accept(mCrudOperationsResolver.getDeleteVisitor());
    if (!TextUtils.isEmpty(selection)) {
      delete = delete.where(selection, selectionArgs);
    }
    int count = mDatabase.delete(delete);
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
    int count = mDatabase.update(update);
    if (count > 0) {
      notifyChange(autoUri);
    }
    return count;
  }

  private void notifyChange(AutoUri autoUri) {
    mContentResolver.notifyChange(mAutoUris.model(autoUri.getModel()).toUri(), null, false);
  }
}
