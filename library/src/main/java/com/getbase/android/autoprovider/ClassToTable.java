package com.getbase.android.autoprovider;

import com.google.common.collect.ImmutableBiMap;

import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.ModelVisitor;
import org.chalup.thneed.models.DatabaseModel;
import org.chalup.thneed.models.PojoModel;

public final class ClassToTable<TModel extends DatabaseModel & PojoModel> {

  private static <TModel extends DatabaseModel & PojoModel> ImmutableBiMap<Class<?>, String> buildClassToTableMap(ModelGraph<TModel> modelGraph) {
    final ImmutableBiMap.Builder<Class<?>, String> classToTableMappingBuilder = ImmutableBiMap.builder();
    modelGraph.accept(new ModelVisitor<TModel>() {
      @Override
      public void visit(TModel model) {
        classToTableMappingBuilder.put(model.getModelClass(), model.getTableName());
      }
    });
    return classToTableMappingBuilder.build();
  }

  private final ImmutableBiMap<Class<?>, String> mClassToTableBiMap;

  public ClassToTable(ModelGraph<TModel> modelModelGraph) {
    mClassToTableBiMap = buildClassToTableMap(modelModelGraph);
  }

  public String getTableForClass(Class<?> klass) {
    return mClassToTableBiMap.get(klass);
  }

  public Class<?> getClassForTable(String table) {
    return mClassToTableBiMap.inverse().get(table);
  }

  public boolean hasClass(Class<?> klass) {
    return mClassToTableBiMap.containsKey(klass);
  }

  public boolean hasTable(String table) {
    return mClassToTableBiMap.inverse().containsKey(table);
  }
}
