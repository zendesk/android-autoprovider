package com.getbase.android.autoprovider;

public interface CustomUri extends AutoUri, AutoUriRelationBuilder<CustomUri> {
  CustomUri model(Class<?> model);
  CustomUri id(long id);
}
