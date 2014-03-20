package com.getbase.android.autocpuris;

interface EntityUriBuilder {
  EntityUri id(long id);
  EntityUri id(String column, long id);
}
