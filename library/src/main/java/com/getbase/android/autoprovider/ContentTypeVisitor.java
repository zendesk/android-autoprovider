package com.getbase.android.autoprovider;

import android.content.ContentResolver;

public class ContentTypeVisitor implements AutoUriVisitor<String> {
  private final String mContentItemTypeBase;
  private final String mContentDirTypeBase;

  public ContentTypeVisitor(String authority) {
    final String subtypeBase = "vnd." + authority + ".";

    mContentItemTypeBase = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + subtypeBase;
    mContentDirTypeBase = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + subtypeBase;
  }

  @Override
  public String visit(EntityUri uri) {
    return mContentItemTypeBase + getModel(uri);
  }

  @Override
  public String visit(ModelUri uri) {
    return mContentDirTypeBase + getModel(uri);
  }

  private String getModel(AutoUri uri) {
    return uri.getModel().getSimpleName();
  }
}
