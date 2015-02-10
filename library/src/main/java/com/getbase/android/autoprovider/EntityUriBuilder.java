package com.getbase.android.autoprovider;

public interface EntityUriBuilder {
  EntityUri id(long id);
  EntityUri id(String column, long id);
}
