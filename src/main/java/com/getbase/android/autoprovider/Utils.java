package com.getbase.android.autoprovider;

import com.getbase.autoindexer.DbTableModel;
import com.getbase.forger.thneed.MicroOrmModel;
import com.google.common.collect.ImmutableBiMap;

import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.ModelVisitor;

public class Utils {
  public static <TModel extends DbTableModel & MicroOrmModel> ImmutableBiMap<Class<?>, String> buildClassToTableMap(ModelGraph<TModel> modelGraph) {
    final ImmutableBiMap.Builder<Class<?>, String> classToTableMappingBuilder = ImmutableBiMap.builder();
    modelGraph.accept(new ModelVisitor<TModel>() {
      @Override
      public void visit(TModel model) {
        classToTableMappingBuilder.put(model.getModelClass(), model.getDbTable());
      }
    });
    return classToTableMappingBuilder.build();
  }
}
