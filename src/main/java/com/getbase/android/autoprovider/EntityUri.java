package com.getbase.android.autoprovider;

interface EntityUri extends AutoUri, ModelUriBuilder, AutoUriRelationBuilder<EntityUri> {
  long getId();
  String getIdColumn();
}
