package com.getbase.android.autoprovider;

public interface EntityUri extends AutoUri, ModelUriBuilder, AutoUriRelationBuilder<EntityUri> {
  long getId();
  String getIdColumn();
}
