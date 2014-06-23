package com.getbase.android.autoprovider;

import static com.getbase.android.db.fluentsqlite.Expressions.column;
import static com.getbase.android.db.fluentsqlite.QueryBuilder.select;

import com.getbase.android.db.fluentsqlite.QueryBuilder.Query;
import com.getbase.autoindexer.DbTableModel;
import com.getbase.forger.thneed.MicroOrmModel;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;

import org.chalup.thneed.ManyToManyRelationship;
import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.OneToManyRelationship;
import org.chalup.thneed.OneToOneRelationship;
import org.chalup.thneed.PolymorphicRelationship;
import org.chalup.thneed.RecursiveModelRelationship;
import org.chalup.thneed.RelationshipVisitor;

import java.util.Map.Entry;

public class QueryBuilderVisitor implements AutoUriVisitor<Query> {
  private final BiMap<Class<?>, String> mClassToTableMap;
  private final Table<Class<?>, Class<?>, QueryProcessor> mQueryProcessors;

  private interface QueryProcessor {
    Query process(Query query, EntityUri relatedEntity);
  }

  public <TModel extends DbTableModel & MicroOrmModel> QueryBuilderVisitor(ModelGraph<TModel> modelGraph) {
    mClassToTableMap = Utils.buildClassToTableMap(modelGraph);

    final Builder<Class<?>, Class<?>, QueryProcessor> builder = ImmutableTable.builder();
    modelGraph.accept(new RelationshipVisitor<TModel>() {
      @Override
      public void visit(final OneToManyRelationship<? extends TModel> relationship) {
        builder.put(
            relationship.mModel.getModelClass(),
            relationship.mReferencedModel.getModelClass(),
            new QueryProcessor() {
              @Override
              public Query process(Query query, EntityUri relatedEntity) {
                if (relatedEntity.getIdColumn().equals(relationship.mReferencedModelIdColumn)) {
                  return query.where(column(relationship.mLinkedByColumn).eq().literal(relatedEntity.getId()));
                } else {
                  return query.where(column(relationship.mLinkedByColumn).in(
                      relatedEntity
                          .accept(QueryBuilderVisitor.this)
                          .column(relationship.mReferencedModelIdColumn)
                  ));
                }
              }
            }
        );
      }

      @Override
      public void visit(final OneToOneRelationship<? extends TModel> relationship) {
        builder.put(
            relationship.mLinkedModel.getModelClass(),
            relationship.mModel.getModelClass(),
            new QueryProcessor() {
              @Override
              public Query process(Query query, EntityUri relatedEntity) {
                if (relatedEntity.getIdColumn().equals(relationship.mParentModelIdColumn)) {
                  return query.where(column(relationship.mLinkedByColumn).eq().literal(relatedEntity.getId()));
                } else {
                  return query.where(column(relationship.mLinkedByColumn).in(
                      relatedEntity
                          .accept(QueryBuilderVisitor.this)
                          .column(relationship.mParentModelIdColumn)
                  ));
                }
              }
            }
        );
      }

      @Override
      public void visit(final RecursiveModelRelationship<? extends TModel> relationship) {
        builder.put(
            relationship.mModel.getModelClass(),
            relationship.mModel.getModelClass(),
            new QueryProcessor() {
              @Override
              public Query process(Query query, EntityUri relatedEntity) {
                if (relatedEntity.getIdColumn().equals(relationship.mModelIdColumn)) {
                  return query.where(column(relationship.mGroupByColumn).eq().literal(relatedEntity.getId()));
                } else {
                  return query.where(column(relationship.mGroupByColumn).in(
                      relatedEntity
                          .accept(QueryBuilderVisitor.this)
                          .column(relationship.mModelIdColumn)
                  ));
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
        for (final Entry<String, ? extends TModel> polyModel : relationship.mPolymorphicModels.entrySet()) {
          builder.put(
              relationship.mModel.getModelClass(),
              polyModel.getValue().getModelClass(),
              new QueryProcessor() {
                @Override
                public Query process(Query query, EntityUri relatedEntity) {
                  query = query.where(column(relationship.mPolymorphicModelIdColumn).eq().literal(polyModel.getKey()));

                  if (relatedEntity.getIdColumn().equals(relationship.mPolymorphicModelIdColumn)) {
                    return query.where(column(relationship.mIdColumnName).eq().literal(relatedEntity.getId()));
                  } else {
                    return query.where(column(relationship.mIdColumnName).in(
                        relatedEntity
                            .accept(QueryBuilderVisitor.this)
                            .column(relationship.mPolymorphicModelIdColumn)
                    ));
                  }
                }
              }
          );
        }
      }
    });
    mQueryProcessors = builder.build();
  }

  @Override
  public Query visit(EntityUri uri) {
    return select()
        .from(mClassToTableMap.get(uri.getModel()))
        .where(column(uri.getIdColumn()).eq().literal(uri.getId()));
  }

  @Override
  public Query visit(ModelUri uri) {
    Query query = select().from(mClassToTableMap.get(uri.getModel()));

    for (EntityUri entityUri : uri.getRelatedEntities()) {
      query = mQueryProcessors.get(uri.getModel(), entityUri.getModel()).process(query, entityUri);
    }

    return query;
  }
}
