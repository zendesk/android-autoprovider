package com.getbase.android.autoprovider;

import com.getbase.autoindexer.DbTableModel;
import com.getbase.forger.thneed.MicroOrmModel;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.ModelVisitor;

import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Collection;

public class AutoUris<TModel extends DbTableModel & MicroOrmModel> implements ModelUriBuilder {
  private final ModelGraph<TModel> mModelGraph;
  private final String mIdColumnName;

  private final BiMap<Class<?>, String> mClassToTableMap;

  AutoUris(ModelGraph<TModel> modelGraph, String idColumnName) {
    mModelGraph = modelGraph;
    mIdColumnName = idColumnName;

    final ImmutableBiMap.Builder<Class<?>, String> classToTableMappingBuilder = ImmutableBiMap.builder();
    mModelGraph.accept(new ModelVisitor<TModel>() {
      @Override
      public void visit(TModel model) {
        classToTableMappingBuilder.put(model.getModelClass(), model.getDbTable());
      }
    });
    mClassToTableMap = classToTableMappingBuilder.build();
  }

  public static <T extends DbTableModel & MicroOrmModel> Builder<T> from(ModelGraph<T> modelGraph) {
    return new Builder<T>(modelGraph);
  }

  public static class Builder<TModel extends DbTableModel & MicroOrmModel> {
    private final ModelGraph<TModel> mModelGraph;
    private String mIdColumnName = BaseColumns._ID;

    Builder(ModelGraph<TModel> modelGraph) {
      mModelGraph = modelGraph;
    }

    public Builder<TModel> defaultIdColumn(String idColumnName) {
      mIdColumnName = idColumnName;
      return this;
    }

    public AutoUris<TModel> build() {
      return new AutoUris<TModel>(mModelGraph, mIdColumnName);
    }
  }

  @Override
  public ModelUri model(Class<?> klass) {
    Preconditions.checkArgument(mClassToTableMap.containsKey(klass));
    return new ModelUriImpl(klass);
  }

  private abstract class AutoUriImpl implements AutoUri {
    @Override
    public Collection<EntityUri> getRelatedEntities() {
      return null;
    }

    @Override
    public Optional<EntityUri> getRelatedEntity(Class<?> model) {
      return null;
    }
  }

  private class ModelUriImpl extends AutoUriImpl implements ModelUri {
    private final Class<?> mKlass;

    ModelUriImpl(Class<?> klass) {
      mKlass = klass;
    }

    ModelUriImpl(ModelUriImpl other) {
      mKlass = other.mKlass;
    }

    @Override
    public Class<?> getModel() {
      return mKlass;
    }

    @Override
    public Uri toUri() {
      return null;
    }

    @Override
    public ModelUri getModelUri() {
      return this;
    }

    @Override
    public ModelUri relatedTo(EntityUri uri) {
      return null;
    }

    @Override
    public EntityUri id(long id) {
      return id(mIdColumnName, id);
    }

    @Override
    public EntityUri id(String column, long id) {
      return new EntityUriImpl(new ModelUriImpl(this), column, id);
    }
  }

  private class EntityUriImpl extends AutoUriImpl implements EntityUri {
    private final ModelUriImpl mModelUri;
    private final String mIdColumnName;
    private final long mId;

    EntityUriImpl(ModelUriImpl modelUri, String idColumnName, long id) {
      mModelUri = modelUri;
      mIdColumnName = idColumnName;
      mId = id;
    }

    @Override
    public long getId() {
      return mId;
    }

    @Override
    public String getIdColumn() {
      return mIdColumnName;
    }

    @Override
    public Uri toUri() {
      return null;
    }

    @Override
    public ModelUri getModelUri() {
      return mModelUri;
    }

    @Override
    public EntityUri relatedTo(EntityUri uri) {
      return null;
    }

    @Override
    public ModelUri model(Class<?> klass) {
      return null;
    }
  }

  public AutoUri fromUri(Uri uri) {
    return null;
  }
}
