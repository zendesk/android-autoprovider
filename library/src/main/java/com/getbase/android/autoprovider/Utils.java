package com.getbase.android.autoprovider;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;

import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.ModelVisitor;
import org.chalup.thneed.models.DatabaseModel;
import org.chalup.thneed.models.PojoModel;

import android.support.annotation.NonNull;

import java.util.Iterator;

public final class Utils {
  private Utils() {
  }

  public static <TModel extends DatabaseModel & PojoModel> ImmutableBiMap<Class<?>, String> buildClassToTableMap(ModelGraph<TModel> modelGraph) {
    final ImmutableBiMap.Builder<Class<?>, String> classToTableMappingBuilder = ImmutableBiMap.builder();
    modelGraph.accept(new ModelVisitor<TModel>() {
      @Override
      public void visit(TModel model) {
        classToTableMappingBuilder.put(model.getModelClass(), model.getTableName());
      }
    });
    return classToTableMappingBuilder.build();
  }

  static <T> Iterator<T> advancePast(@NonNull Iterator<T> iterator, @NonNull T element) {
    Preconditions.checkNotNull(iterator);
    Preconditions.checkNotNull(element);

    while (iterator.hasNext()) {
      if (iterator.next() == element) {
        break;
      }
    }

    Preconditions.checkArgument(iterator.hasNext());

    return iterator;
  }
}
