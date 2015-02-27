package com.getbase.android.autoprovider;

public interface CustomUriBuilder {
  CustomUri path(Object path);
  CustomUri path(Class<?> model);
}
