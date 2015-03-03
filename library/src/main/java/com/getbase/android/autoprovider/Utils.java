package com.getbase.android.autoprovider;

import com.google.common.collect.ImmutableBiMap;

import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.ModelVisitor;
import org.chalup.thneed.models.DatabaseModel;
import org.chalup.thneed.models.PojoModel;

public class Utils {
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
}
