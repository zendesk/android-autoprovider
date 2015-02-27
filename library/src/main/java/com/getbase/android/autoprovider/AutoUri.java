package com.getbase.android.autoprovider;

import com.google.common.base.Optional;

import android.net.Uri;

import java.util.Collection;

public interface AutoUri extends CustomUriBuilder {
  Uri toUri();

  ModelUri getModelUri();

  Class<?> getModel();

  Collection<EntityRelation> getRelatedEntities();

  Optional<EntityRelation> getRelatedEntity(Class<?> model);

  <T> T accept(AutoUriVisitor<T> visitor);
}
