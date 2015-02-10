package com.getbase.android.autoprovider;

public interface AutoUriVisitor<T> {
  T visit(EntityUri uri);
  T visit(ModelUri uri);
}
