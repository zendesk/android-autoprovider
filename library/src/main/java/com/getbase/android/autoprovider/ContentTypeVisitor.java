package com.getbase.android.autoprovider;

public class ContentTypeVisitor implements AutoUriVisitor<String> {
  private final ContentTypeProvider<?> mContentTypeProvider;

  public ContentTypeVisitor(ContentTypeProvider<?> contentTypeProvider) {
    mContentTypeProvider = contentTypeProvider;
  }

  @Override
  public String visit(EntityUri uri) {
    return mContentTypeProvider.getEntityContentType(uri.getModel());
  }

  @Override
  public String visit(ModelUri uri) {
    return mContentTypeProvider.getModelContentType(uri.getModel());
  }

  @Override
  public String visit(CustomUri uri) {
    throw new UnsupportedOperationException(uri.toString());
  }
}
