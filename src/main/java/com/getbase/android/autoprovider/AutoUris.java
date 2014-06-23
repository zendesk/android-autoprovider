package com.getbase.android.autoprovider;

import com.getbase.autoindexer.DbTableModel;
import com.getbase.forger.thneed.MicroOrmModel;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.chalup.thneed.ManyToManyRelationship;
import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.OneToManyRelationship;
import org.chalup.thneed.OneToOneRelationship;
import org.chalup.thneed.PolymorphicRelationship;
import org.chalup.thneed.RecursiveModelRelationship;
import org.chalup.thneed.RelationshipVisitor;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AutoUris<TModel extends DbTableModel & MicroOrmModel> implements ModelUriBuilder {
  private static final String RELATED_TO_QUERY_PARAM = "relatedTo";
  private static final String ID_COLUMN_QUERY_PARAM = "idColumn";

  private final ModelGraph<TModel> mModelGraph;
  private final String mAuthority;
  private final String mIdColumnName;

  private final BiMap<Class<?>, String> mClassToTableMap;
  private final Multimap<Class<?>, Class<?>> mRelationsByClasses;

  AutoUris(ModelGraph<TModel> modelGraph, String authority, String idColumnName) {
    mModelGraph = modelGraph;
    mAuthority = authority;
    mIdColumnName = idColumnName;

    mClassToTableMap = Utils.buildClassToTableMap(mModelGraph);

    final HashMultimap<Class<?>, Class<?>> relationsByClass = HashMultimap.create();
    final Map<Class<?>, Class<?>> unsupportedRelations = Maps.newHashMap();
    mModelGraph.accept(new RelationshipVisitor<TModel>() {
      @Override
      public void visit(OneToManyRelationship<? extends TModel> relationship) {
        TModel manyModel = relationship.mModel;
        TModel oneModel = relationship.mReferencedModel;

        if (!relationsByClass.put(manyModel.getModelClass(), oneModel.getModelClass())) {
          unsupportedRelations.put(manyModel.getModelClass(), oneModel.getModelClass());
        }
      }

      @Override
      public void visit(OneToOneRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;
        TModel linkedModel = relationship.mLinkedModel;

        if (!relationsByClass.put(linkedModel.getModelClass(), model.getModelClass())) {
          unsupportedRelations.put(linkedModel.getModelClass(), model.getModelClass());
        }
      }

      @Override
      public void visit(RecursiveModelRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;

        if (!relationsByClass.put(model.getModelClass(), model.getModelClass())) {
          unsupportedRelations.put(model.getModelClass(), model.getModelClass());
        }
      }

      @Override
      public void visit(ManyToManyRelationship<? extends TModel> relationship) {
        // implementation not necessary
      }

      @Override
      public void visit(PolymorphicRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;

        for (TModel polyModel : relationship.mPolymorphicModels.values()) {
          if (!relationsByClass.put(model.getModelClass(), polyModel.getModelClass())) {
            unsupportedRelations.put(model.getModelClass(), polyModel.getModelClass());
          }
        }
      }
    });

    for (Entry<Class<?>, Class<?>> unsupportedRelation : unsupportedRelations.entrySet()) {
      relationsByClass.remove(unsupportedRelation.getKey(), unsupportedRelation.getValue());
    }

    mRelationsByClasses = ImmutableMultimap.copyOf(relationsByClass);
  }

  public static <T extends DbTableModel & MicroOrmModel> AuthoritySelector<T> from(ModelGraph<T> modelGraph) {
    return new Builder<T>(modelGraph);
  }

  public String getAuthority() {
    return mAuthority;
  }

  public static class Builder<TModel extends DbTableModel & MicroOrmModel> implements AuthoritySelector<TModel> {
    private final ModelGraph<TModel> mModelGraph;
    private String mIdColumnName = BaseColumns._ID;
    private String mAuthority;

    Builder(ModelGraph<TModel> modelGraph) {
      mModelGraph = modelGraph;
    }

    public Builder<TModel> defaultIdColumn(String idColumnName) {
      mIdColumnName = idColumnName;
      return this;
    }

    public AutoUris<TModel> build() {
      return new AutoUris<TModel>(mModelGraph, mAuthority, mIdColumnName);
    }

    @Override
    public Builder<TModel> forContentProvider(String authority) {
      mAuthority = authority;
      return this;
    }
  }

  public interface AuthoritySelector<TModel extends DbTableModel & MicroOrmModel> {
    Builder<TModel> forContentProvider(String authority);
  }

  @Override
  public ModelUri model(Class<?> klass) {
    Preconditions.checkNotNull(klass);
    Preconditions.checkArgument(mClassToTableMap.containsKey(klass), "Model %s is not present in supplied model graph", klass.getSimpleName());
    return new ModelUriImpl(klass);
  }

  private abstract class AutoUriImpl implements AutoUri {
    protected Map<Class<?>, EntityUri> mRelatedEntities;

    protected AutoUriImpl() {
      mRelatedEntities = Maps.newHashMap();
    }

    protected AutoUriImpl(AutoUriImpl other) {
      mRelatedEntities = Maps.newHashMap(other.mRelatedEntities);
    }

    @Override
    public Collection<EntityUri> getRelatedEntities() {
      return Lists.newCopyOnWriteArrayList(mRelatedEntities.values());
    }

    @Override
    public Optional<EntityUri> getRelatedEntity(Class<?> model) {
      Preconditions.checkNotNull(model);
      return Optional.fromNullable(mRelatedEntities.get(model));
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(mRelatedEntities);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;

      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;

      final AutoUriImpl other = (AutoUriImpl) obj;
      return Objects.equal(this.mRelatedEntities, other.mRelatedEntities);
    }

    protected boolean appendRelationsAsPath(Uri.Builder builder) {
      if (mRelatedEntities.size() == 1) {
        EntityUri entityUri = mRelatedEntities.values().iterator().next();
        if (entityUri.getIdColumn().equals(mIdColumnName)) {
          if (entityUri.getRelatedEntities().isEmpty()) {
            builder
                .appendPath(mClassToTableMap.get(entityUri.getModelUri().getModel()))
                .appendPath(String.valueOf(entityUri.getId()));

            return true;
          }
        }
      }
      return false;
    }

    protected void appendRelationsAsParams(Uri.Builder builder) {
      for (EntityUri relatedEntityUri : mRelatedEntities.values()) {
        builder.appendQueryParameter(RELATED_TO_QUERY_PARAM, relatedEntityUri.toUri().toString());
      }
    }
  }

  private class ModelUriImpl extends AutoUriImpl implements ModelUri {
    final Class<?> mKlass;

    ModelUriImpl(Class<?> klass) {
      mKlass = klass;
    }

    ModelUriImpl(ModelUriImpl other) {
      super(other);
      mKlass = other.mKlass;
    }

    @Override
    public Class<?> getModel() {
      return mKlass;
    }

    @Override
    public <T> T accept(AutoUriVisitor<T> visitor) {
      return visitor.visit(this);
    }

    @Override
    public Uri toUri() {
      Uri.Builder builder = new Uri.Builder()
          .scheme(ContentResolver.SCHEME_CONTENT)
          .authority(mAuthority);

      if (!appendRelationsAsPath(builder)) {
        appendRelationsAsParams(builder);
      }

      return builder
          .appendPath(mClassToTableMap.get(getModel()))
          .build();
    }

    @Override
    public ModelUri getModelUri() {
      return this;
    }

    @Override
    public ModelUri relatedTo(EntityUri uri) {
      Preconditions.checkNotNull(uri);
      ModelUriImpl modelUri = new ModelUriImpl(this);
      Class<?> relationModel = uri.getModelUri().getModel();
      Class<?> model = getModel();
      Preconditions.checkArgument(mRelationsByClasses.containsEntry(model, relationModel));
      EntityUri previousValue = modelUri.mRelatedEntities.put(relationModel, uri);
      Preconditions.checkArgument(previousValue == null, "Duplicate relation for model %s", relationModel.getSimpleName());
      return modelUri;
    }

    @Override
    public EntityUri id(long id) {
      return id(mIdColumnName, id);
    }

    @Override
    public EntityUri id(String column, long id) {
      EntityUriImpl entityUri = new EntityUriImpl(mKlass, column, id);
      entityUri.mRelatedEntities.putAll(mRelatedEntities);
      return entityUri;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(mKlass, super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      if (!super.equals(obj)) return false;

      final ModelUriImpl other = (ModelUriImpl) obj;

      return Objects.equal(this.mKlass, other.mKlass);
    }

    @Override
    public String toString() {
      return Objects
          .toStringHelper(this)
          .add("model", getModel().getSimpleName())
          .add("relatedTo", Joiner.on(", ").withKeyValueSeparator(": ").join(mRelatedEntities))
          .toString();
    }
  }

  private class EntityUriImpl extends AutoUriImpl implements EntityUri {
    private final Class<?> mKlass;
    private final String mIdColumnName;
    private final long mId;

    EntityUriImpl(Class<?> klass, String idColumnName, long id) {
      mKlass = klass;
      mIdColumnName = idColumnName;
      mId = id;
    }

    EntityUriImpl(EntityUriImpl other) {
      super(other);
      mKlass = other.mKlass;
      mIdColumnName = other.mIdColumnName;
      mId = other.mId;
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
      Uri.Builder builder = new Uri.Builder()
          .scheme(ContentResolver.SCHEME_CONTENT)
          .authority(mAuthority)
          .appendPath(mClassToTableMap.get(getModelUri().getModel()))
          .appendPath(String.valueOf(getId()));

      appendRelationsAsParams(builder);

      if (!AutoUris.this.mIdColumnName.equals(mIdColumnName)) {
        builder.appendQueryParameter(ID_COLUMN_QUERY_PARAM, mIdColumnName);
      }

      return builder.build();
    }

    @Override
    public ModelUri getModelUri() {
      return new ModelUriImpl(mKlass);
    }

    @Override
    public Class<?> getModel() {
      return mKlass;
    }

    @Override
    public <T> T accept(AutoUriVisitor<T> visitor) {
      return visitor.visit(this);
    }

    @Override
    public EntityUri relatedTo(EntityUri uri) {
      Preconditions.checkNotNull(uri);
      EntityUriImpl entityUri = new EntityUriImpl(this);
      Class<?> relationModel = uri.getModelUri().getModel();
      Class<?> model = entityUri.getModelUri().getModel();
      Preconditions.checkArgument(mRelationsByClasses.containsEntry(model, relationModel));
      EntityUri previousValue = entityUri.mRelatedEntities.put(relationModel, uri);
      Preconditions.checkArgument(previousValue == null, "Duplicate relation for model %s", relationModel.getSimpleName());
      return entityUri;
    }

    @Override
    public ModelUri model(Class<?> klass) {
      return AutoUris.this.model(klass).relatedTo(this);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(mKlass, mIdColumnName, mId, super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      if (!super.equals(obj)) return false;

      final EntityUriImpl other = (EntityUriImpl) obj;

      return Objects.equal(this.mKlass, other.mKlass) &&
          Objects.equal(this.mIdColumnName, other.mIdColumnName) &&
          Objects.equal(this.mId, other.mId);
    }

    @Override
    public String toString() {
      return Objects
          .toStringHelper(this)
          .add("id", mId)
          .add("idColumn", mIdColumnName)
          .add("model", getModelUri().getModel().getSimpleName())
          .add("relatedTo", Joiner.on(", ").withKeyValueSeparator(": ").join(mRelatedEntities))
          .toString();
    }
  }

  private void checkUri(Uri uri) {
    Preconditions.checkNotNull(uri);
    Preconditions.checkArgument(uri.getScheme().equals(ContentResolver.SCHEME_CONTENT));
    Preconditions.checkArgument(uri.getAuthority().equals(mAuthority));
  }

  public ModelUri getModelUri(Uri uri) {
    checkUri(uri);

    List<EntityUri> relatedEntities = getRelatedEntities(uri);

    List<String> pathSegments = uri.getPathSegments();
    switch (pathSegments.size()) {
    case 1:
      break;
    case 3:
      relatedEntities.add(
          model(mClassToTableMap.inverse().get(pathSegments.get(0)))
              .id(Long.parseLong(pathSegments.get(1)))
      );
      break;
    default:
      throw new IllegalArgumentException("Invalid number of path segments");
    }

    return appendRelatedEntities(
        model(mClassToTableMap.inverse().get(uri.getLastPathSegment())),
        relatedEntities
    );
  }

  public EntityUri getEntityUri(Uri uri) {
    checkUri(uri);

    List<String> pathSegments = uri.getPathSegments();
    Preconditions.checkArgument(pathSegments.size() == 2, "Invalid number of path segments");

    return appendRelatedEntities(
        model(mClassToTableMap.inverse().get(pathSegments.get(0)))
            .id(getIdColumn(uri), Long.parseLong(pathSegments.get(1))),
        getRelatedEntities(uri)
    );
  }

  public Optional<AutoUri> getAutoUri(Uri uri) {
    if (uri == null)
      return Optional.absent();

    try {
      switch (uri.getPathSegments().size()) {
      case 1: // fallthrough
      case 3:
        return Optional.<AutoUri>of(getModelUri(uri));
      case 2:
        return Optional.<AutoUri>of(getEntityUri(uri));
      default:
        return Optional.absent();
      }
    } catch (IllegalArgumentException e) {
      return Optional.absent();
    }
  }

  private List<EntityUri> getRelatedEntities(Uri uri) {
    return Lists.newArrayList(
        FluentIterable
            .from(uri.getQueryParameters(RELATED_TO_QUERY_PARAM))
            .transform(new Function<String, EntityUri>() {
              @Override
              public EntityUri apply(String input) {
                return getEntityUri(Uri.parse(input));
              }
            })
    );
  }

  private String getIdColumn(Uri uri) {
    return Objects.firstNonNull(uri.getQueryParameter(ID_COLUMN_QUERY_PARAM), mIdColumnName);
  }

  private <T extends AutoUriRelationBuilder<T> & AutoUri> T appendRelatedEntities(T modelOrEntityUri, Iterable<EntityUri> relatedEntities) {
    for (EntityUri relatedEntity : relatedEntities) {
      modelOrEntityUri = modelOrEntityUri.relatedTo(relatedEntity);
    }
    return modelOrEntityUri;
  }
}
