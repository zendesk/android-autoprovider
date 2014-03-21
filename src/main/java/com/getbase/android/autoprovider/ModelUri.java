package com.getbase.android.autoprovider;

interface ModelUri extends AutoUri, EntityUriBuilder, AutoUriRelationBuilder<ModelUri> {
  Class<?> getModel();
}
