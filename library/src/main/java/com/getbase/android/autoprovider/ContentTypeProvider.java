package com.getbase.android.autoprovider;

import org.chalup.thneed.models.DatabaseModel;
import org.chalup.thneed.models.PojoModel;

import android.content.ContentResolver;

public class ContentTypeProvider<TModel extends DatabaseModel & PojoModel> {
  private final String mContentItemTypeBase;
  private final String mContentDirTypeBase;

  private final ClassToTable<TModel> mClassToTable;

  public ContentTypeProvider(String prefix, ClassToTable<TModel> classToTable) {
    final String subtypeBase = "vnd." + prefix + ".";

    mContentItemTypeBase = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + subtypeBase;
    mContentDirTypeBase = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + subtypeBase;

    mClassToTable = classToTable;
  }

  public String getModelContentType(Class<?> model) {
    return mContentDirTypeBase + mClassToTable.getTableForClass(model);
  }

  public String getEntityContentType(Class<?> model) {
    return mContentItemTypeBase + mClassToTable.getTableForClass(model);
  }
}
