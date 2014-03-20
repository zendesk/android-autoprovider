package com.getbase.android.autocpuris;

interface EntityUri extends AutoUri, ModelUriBuilder, AutoUriRelationBuilder<EntityUri> {
  long getId();
  String getIdColumn();
}
