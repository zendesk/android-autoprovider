package com.getbase.android.autocpuris;

import com.google.common.base.Optional;

import android.net.Uri;

import java.util.List;

interface AutoUri {
  Uri toUri();

  ModelUri getModelUri();

  List<EntityUri> getRelatedEntities();

  Optional<EntityUri> getRelatedEntity(Class<?> model);
}
