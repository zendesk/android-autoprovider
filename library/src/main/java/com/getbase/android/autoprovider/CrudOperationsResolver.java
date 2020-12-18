package com.getbase.android.autoprovider;

import static com.getbase.android.db.fluentsqlite.Delete.delete;
import static com.getbase.android.db.fluentsqlite.Expressions.column;
import static com.getbase.android.db.fluentsqlite.Insert.insert;
import static com.getbase.android.db.fluentsqlite.Query.select;
import static com.getbase.android.db.fluentsqlite.Update.update;

import com.getbase.android.db.cursors.SingleRowTransforms;
import com.getbase.android.db.fluentsqlite.Delete;
import com.getbase.android.db.fluentsqlite.Expressions.Expression;
import com.getbase.android.db.fluentsqlite.Insert;
import com.getbase.android.db.fluentsqlite.Query.QueryBuilder;
import com.getbase.android.db.fluentsqlite.Update;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

import org.chalup.thneed.ManyToManyRelationship;
import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.OneToManyRelationship;
import org.chalup.thneed.OneToOneRelationship;
import org.chalup.thneed.PolymorphicRelationship;
import org.chalup.thneed.RecursiveModelRelationship;
import org.chalup.thneed.RelationshipVisitor;
import org.chalup.thneed.models.DatabaseModel;
import org.chalup.thneed.models.PojoModel;

import android.content.ContentValues;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CrudOperationsResolver {
  private final BiMap<Class<?>, String> mClassToTableMap;
  private final Table<Class<?>, Class<?>, Map<String, CrudDataProcessor>> mCrudDataProcessors;
  private final AutoProviderDatabase mDatabase;

  private interface CrudDataProcessor {
    void processSelection(List<Expression> expressions, EntityUri relatedEntity);
    void processValues(ContentValues values, EntityUri relatedEntity);
  }

  private void addCrudDataProcessor(Table<Class<?>, Class<?>, Map<String, CrudDataProcessor>> crudDataProcessors, Class<?> modelClass, Class<?> referencedModelClass, String columnName, CrudDataProcessor crudDataProcessor) {
    Map<String, CrudDataProcessor> crudDataProcessorsMap = crudDataProcessors.get(modelClass, referencedModelClass);
    if (crudDataProcessorsMap == null) {
      crudDataProcessorsMap = Maps.newHashMap();
      crudDataProcessors.put(modelClass, referencedModelClass, crudDataProcessorsMap);
    }
    crudDataProcessorsMap.put(columnName, crudDataProcessor);
  }

  private List<Expression> getSelection(AutoUri uri) {
    List<Expression> expressions = Lists.newArrayList();
    for (EntityRelation entityRelation : uri.getRelatedEntities()) {
      Map<String, CrudDataProcessor> processorMap = mCrudDataProcessors.get(uri.getModel(), entityRelation.entityUri.getModel());
      if (entityRelation.relationColumn.isPresent()) {
        mCrudDataProcessors.get(uri.getModel(), entityRelation.entityUri.getModel()).get(entityRelation.relationColumn.get()).processSelection(expressions, entityRelation.entityUri);
      } else {
        if (processorMap.size() == 1) {
          mCrudDataProcessors.get(uri.getModel(), entityRelation.entityUri.getModel()).values().iterator().next().processSelection(expressions, entityRelation.entityUri);
        } else {
          throw new IllegalArgumentException("Ambiguous relation for uri: " + uri.toUri().toString());
        }
      }
    }
    return expressions;
  }

  private ContentValues getValues(AutoUri uri) {
    ContentValues values = new ContentValues();
    for (EntityRelation entityRelation : uri.getRelatedEntities()) {
      Map<String, CrudDataProcessor> processorMap = mCrudDataProcessors.get(uri.getModel(), entityRelation.entityUri.getModel());
      if (entityRelation.relationColumn.isPresent()) {
        mCrudDataProcessors.get(uri.getModel(), entityRelation.entityUri.getModel()).get(entityRelation.relationColumn.get()).processValues(values, entityRelation.entityUri);
      } else {
        if (processorMap.size() == 1) {
          mCrudDataProcessors.get(uri.getModel(), entityRelation.entityUri.getModel()).values().iterator().next().processValues(values, entityRelation.entityUri);
        } else {
          throw new IllegalArgumentException("Ambiguous relation for uri: " + uri.toUri().toString());
        }
      }
    }
    return values;
  }

  private final AutoUriVisitor<QueryBuilder> mQueryBuilderVisitor = new AutoUriVisitor<QueryBuilder>() {
    @Override
    public QueryBuilder visit(EntityUri uri) {
      return getQueryBuilder(uri).where(column(uri.getIdColumn()).eq().literal(uri.getId()));
    }

    @Override
    public QueryBuilder visit(ModelUri uri) {
      return getQueryBuilder(uri);
    }

    @Override
    public QueryBuilder visit(CustomUri uri) {
      throw new UnsupportedOperationException(uri.toString());
    }

    private QueryBuilder getQueryBuilder(AutoUri uri) {
      QueryBuilder queryBuilder = select().from(mClassToTableMap.get(uri.getModel()));

      if (!uri.getRelatedEntities().isEmpty()) {
        for (Expression expression : getSelection(uri)) {
          queryBuilder.where(expression);
        }
      }

      return queryBuilder;
    }
  };

  private final AutoUriVisitor<Update> mUpdateVisitor = new AutoUriVisitor<Update>() {
    @Override
    public Update visit(EntityUri uri) {
      return getUpdate(uri).where(uri.getIdColumn() + "=?", uri.getId());
    }

    @Override
    public Update visit(ModelUri uri) {
      return getUpdate(uri);
    }

    @Override
    public Update visit(CustomUri uri) {
      throw new UnsupportedOperationException(uri.toString());
    }

    private Update getUpdate(AutoUri uri) {
      Update update = update().table(mClassToTableMap.get(uri.getModel()));
      if (!uri.getRelatedEntities().isEmpty()) {
        for (Expression expression : getSelection(uri)) {
          update = update.where(expression);
        }
      }
      return update;
    }
  };

  private final AutoUriVisitor<Insert> mInsertVisitor = new AutoUriVisitor<Insert>() {
    @Override
    public Insert visit(EntityUri uri) {
      return getInsert(uri).value(uri.getIdColumn(), uri.getId());
    }

    @Override
    public Insert visit(ModelUri uri) {
      return getInsert(uri);
    }

    @Override
    public Insert visit(CustomUri uri) {
      throw new UnsupportedOperationException(uri.toString());
    }

    private final ContentValues EMPTY_VALUES = new ContentValues();

    private Insert getInsert(AutoUri uri) {
      Insert insert = insert().into(mClassToTableMap.get(uri.getModel()))
          .values(EMPTY_VALUES); // why are you doing this to me?

      if (!uri.getRelatedEntities().isEmpty()) {
        insert = insert.values(getValues(uri));
      }

      return insert;
    }
  };

  private final AutoUriVisitor<Delete> mDeleteVisitor = new AutoUriVisitor<Delete>() {
    @Override
    public Delete visit(EntityUri uri) {
      return getDelete(uri).where(uri.getIdColumn() + "=?", uri.getId());
    }

    @Override
    public Delete visit(ModelUri uri) {
      return getDelete(uri);
    }

    @Override
    public Delete visit(CustomUri uri) {
      throw new UnsupportedOperationException(uri.toString());
    }

    private Delete getDelete(AutoUri uri) {
      Delete delete = delete().from(mClassToTableMap.get(uri.getModel()));
      if (!uri.getRelatedEntities().isEmpty()) {
        for (Expression expression : getSelection(uri)) {
          delete = delete.where(expression);
        }
      }
      return delete;
    }
  };

  private final AutoUriVisitor<Set<Class<?>>> mRelatedModelsVisitor = new AutoUriVisitor<Set<Class<?>>>() {
    @Override
    public Set<Class<?>> visit(EntityUri uri) {
      return getRelatedModels(uri.getModel());
    }

    @Override
    public Set<Class<?>> visit(ModelUri uri) {
      return getRelatedModels(uri.getModel());
    }

    @Override
    public Set<Class<?>> visit(CustomUri uri) {
      throw new UnsupportedOperationException(uri.toString());
    }

    private Set<Class<?>> getRelatedModels(Class<?> model) {
      if (mCrudDataProcessors.containsColumn(model)) {
        Set<Class<?>> relatedModels = mCrudDataProcessors.column(model).keySet();
        return relatedModels;
      }
      return null;
    }
  };

  public <TModel extends DatabaseModel & PojoModel> CrudOperationsResolver(AutoProviderDatabase database, ModelGraph<TModel> modelGraph) {
    mDatabase = database;
    mClassToTableMap = Utils.buildClassToTableMap(modelGraph);
    final Table<Class<?>, Class<?>, Map<String, CrudDataProcessor>> crudDataProcessors = HashBasedTable.create();

    modelGraph.accept(new RelationshipVisitor<TModel>() {
      @Override
      public void visit(final OneToManyRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;
        TModel referencedModel = relationship.mReferencedModel;
        addCrudDataProcessor(crudDataProcessors,
            model.getModelClass(),
            referencedModel.getModelClass(),
            relationship.mLinkedByColumn,
            new CrudDataProcessor() {
              @Override
              public void processSelection(List<Expression> expressions, EntityUri relatedEntity) {
                if (relatedEntity.getIdColumn().equals(relationship.mReferencedModelIdColumn)) {
                  expressions.add(column(relationship.mLinkedByColumn).eq().literal(relatedEntity.getId()));
                } else {
                  expressions.add(column(relationship.mLinkedByColumn).in(
                      relatedEntity
                          .accept(mQueryBuilderVisitor)
                          .column(relationship.mReferencedModelIdColumn)
                  ));
                }
              }

              @Override
              public void processValues(ContentValues values, EntityUri relatedEntity) {
                if (relatedEntity.getIdColumn().equals(relationship.mReferencedModelIdColumn)) {
                  values.put(relationship.mLinkedByColumn, relatedEntity.getId());
                } else {
                  String id = mDatabase.query(mQueryBuilderVisitor.visit(relatedEntity).column(relationship.mReferencedModelIdColumn))
                      .toOnlyElement(SingleRowTransforms.getColumn(relationship.mReferencedModelIdColumn).asString());
                  values.put(relationship.mLinkedByColumn, id);
                }
              }
            }
        );
      }

      @Override
      public void visit(final OneToOneRelationship<? extends TModel> relationship) {
        TModel linkedModel = relationship.mLinkedModel;
        TModel model = relationship.mModel;
        addCrudDataProcessor(crudDataProcessors,
            linkedModel.getModelClass(),
            model.getModelClass(),
            relationship.mLinkedByColumn,
            new CrudDataProcessor() {
              @Override
              public void processSelection(List<Expression> expressions, EntityUri relatedEntity) {
                if (relatedEntity.getIdColumn().equals(relationship.mParentModelIdColumn)) {
                  expressions.add(column(relationship.mLinkedByColumn).eq().literal(relatedEntity.getId()));
                } else {
                  expressions.add(column(relationship.mLinkedByColumn).in(
                      relatedEntity
                          .accept(mQueryBuilderVisitor)
                          .column(relationship.mParentModelIdColumn)
                  ));
                }
              }

              @Override
              public void processValues(ContentValues values, EntityUri relatedEntity) {
                if (relatedEntity.getIdColumn().equals(relationship.mParentModelIdColumn)) {
                  values.put(relationship.mLinkedByColumn, relatedEntity.getId());
                } else {
                  String id = mDatabase.query(mQueryBuilderVisitor.visit(relatedEntity).column(relationship.mParentModelIdColumn))
                      .toOnlyElement(SingleRowTransforms.getColumn(relationship.mParentModelIdColumn).asString());
                  values.put(relationship.mLinkedByColumn, id);
                }
              }
            }
        );
      }

      @Override
      public void visit(final RecursiveModelRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;
        addCrudDataProcessor(crudDataProcessors,
            model.getModelClass(),
            model.getModelClass(),
            relationship.mGroupByColumn,
            new CrudDataProcessor() {
              @Override
              public void processSelection(List<Expression> expressions, EntityUri relatedEntity) {
                if (relatedEntity.getIdColumn().equals(relationship.mModelIdColumn)) {
                  expressions.add(column(relationship.mGroupByColumn).eq().literal(relatedEntity.getId()));
                } else {
                  expressions.add(column(relationship.mGroupByColumn).in(
                      relatedEntity
                          .accept(mQueryBuilderVisitor)
                          .column(relationship.mModelIdColumn)
                  ));
                }
              }

              @Override
              public void processValues(ContentValues values, EntityUri relatedEntity) {
                if (relatedEntity.getIdColumn().equals(relationship.mModelIdColumn)) {
                  values.put(relationship.mGroupByColumn, relatedEntity.getId());
                } else {
                  String id = mDatabase.query(mQueryBuilderVisitor.visit(relatedEntity).column(relationship.mModelIdColumn))
                      .toOnlyElement(SingleRowTransforms.getColumn(relationship.mModelIdColumn).asString());
                  values.put(relationship.mGroupByColumn, id);
                }
              }
            }
        );
      }

      @Override
      public void visit(ManyToManyRelationship<? extends TModel> relationship) {
        // no implementation necessary
      }

      @Override
      public void visit(final PolymorphicRelationship<? extends TModel> relationship) {
        for (final Entry<String, ? extends TModel> polyModelEntry : relationship.mPolymorphicModels.entrySet()) {
          TModel model = relationship.mModel;
          TModel polyModel = polyModelEntry.getValue();
          addCrudDataProcessor(crudDataProcessors,
              model.getModelClass(),
              polyModel.getModelClass(),
              relationship.mPolymorphicModelIdColumn,
              new CrudDataProcessor() {
                @Override
                public void processSelection(List<Expression> expressions, EntityUri relatedEntity) {
                  expressions.add(column(relationship.mTypeColumnName).eq().literal(polyModelEntry.getKey()));

                  if (relatedEntity.getIdColumn().equals(relationship.mPolymorphicModelIdColumn)) {
                    expressions.add(column(relationship.mIdColumnName).eq().literal(relatedEntity.getId()));
                  } else {
                    expressions.add(column(relationship.mIdColumnName).in(
                        relatedEntity
                            .accept(mQueryBuilderVisitor)
                            .column(relationship.mPolymorphicModelIdColumn)
                    ));
                  }
                }

                @Override
                public void processValues(ContentValues values, EntityUri relatedEntity) {
                  values.put(relationship.mTypeColumnName, polyModelEntry.getKey());

                  if (relatedEntity.getIdColumn().equals(relationship.mPolymorphicModelIdColumn)) {
                    values.put(relationship.mIdColumnName, relatedEntity.getId());
                  } else {
                    String id = mDatabase.query(mQueryBuilderVisitor.visit(relatedEntity).column(relationship.mPolymorphicModelIdColumn))
                        .toOnlyElement(SingleRowTransforms.getColumn(relationship.mPolymorphicModelIdColumn).asString());
                    values.put(relationship.mIdColumnName, id);
                  }
                }
              }
          );
        }
      }
    });

    mCrudDataProcessors = ImmutableTable.copyOf(crudDataProcessors);
  }

  public AutoUriVisitor<QueryBuilder> getQueryBuilderVisitor() {
    return mQueryBuilderVisitor;
  }

  public AutoUriVisitor<Update> getUpdateVisitor() {
    return mUpdateVisitor;
  }

  public AutoUriVisitor<Insert> getInsertVisitor() {
    return mInsertVisitor;
  }

  public AutoUriVisitor<Delete> getDeleteVisitor() {
    return mDeleteVisitor;
  }

  public AutoUriVisitor<Set<Class<?>>> getRelatedModelsVisitor() {
    return mRelatedModelsVisitor;
  }
}
