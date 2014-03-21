package com.getbase.android.autoprovider;

interface EntityUriBuilder {
  EntityUri id(long id);
  EntityUri id(String column, long id);
}
