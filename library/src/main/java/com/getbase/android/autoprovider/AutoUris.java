package com.getbase.android.autoprovider;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class AutoUris<TModel extends DatabaseModel & PojoModel> implements ModelUriBuilder, CustomUriBuilder {
  private static final String RELATED_TO_QUERY_PARAM = "relatedTo";
  private static final String ID_COLUMN_QUERY_PARAM = "idColumn";

  private final String mAuthority;
  private final String mIdColumnName;

  private final ClassToTable<TModel> mClassToTableMap;
  private final Table<Class<?>, Class<?>, Set<String>> mRelationsByClasses;

  AutoUris(ModelGraph<TModel> modelGraph, ClassToTable<TModel> classToTable, String authority, String idColumnName) {
    mAuthority = authority;
    mIdColumnName = idColumnName;

    mClassToTableMap = classToTable;

    final Table<Class<?>, Class<?>, Set<String>> relationsByClass = HashBasedTable.create();

    modelGraph.accept(new RelationshipVisitor<TModel>() {
      private void addRelation(Class<?> modelClass, Class<?> referencedModelClass, String columnName) {
        Set<String> relationColumns = relationsByClass.get(modelClass, referencedModelClass);
        if (relationColumns == null) {
          relationColumns = Sets.newHashSet();
          relationsByClass.put(modelClass, referencedModelClass, relationColumns);
        }
        relationColumns.add(columnName);
      }

      @Override
      public void visit(OneToManyRelationship<? extends TModel> relationship) {
        TModel manyModel = relationship.mModel;
        TModel oneModel = relationship.mReferencedModel;

        addRelation(manyModel.getModelClass(), oneModel.getModelClass(), relationship.mLinkedByColumn);
      }

      @Override
      public void visit(OneToOneRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;
        TModel linkedModel = relationship.mLinkedModel;

        addRelation(linkedModel.getModelClass(), model.getModelClass(), relationship.mLinkedByColumn);
      }

      @Override
      public void visit(RecursiveModelRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;
        addRelation(model.getModelClass(), model.getModelClass(), relationship.mGroupByColumn);
      }

      @Override
      public void visit(ManyToManyRelationship<? extends TModel> relationship) {
        // implementation not necessary
      }

      @Override
      public void visit(PolymorphicRelationship<? extends TModel> relationship) {
        TModel model = relationship.mModel;

        for (TModel polyModel : relationship.mPolymorphicModels.values()) {
          addRelation(model.getModelClass(), polyModel.getModelClass(), relationship.mPolymorphicModelIdColumn);
        }
      }
    });

    mRelationsByClasses = ImmutableTable.copyOf(relationsByClass);
  }

  public static <T extends DatabaseModel & PojoModel> AuthoritySelector<T> from(ModelGraph<T> modelGraph) {
    return new Builder<T>(modelGraph, new ClassToTable<>(modelGraph));
  }

  public static <T extends DatabaseModel & PojoModel> AuthoritySelector<T> from(ModelGraph<T> modelGraph, ClassToTable<T> classToTable) {
    return new Builder<T>(modelGraph, classToTable);
  }

  public String getAuthority() {
    return mAuthority;
  }

  public static class Builder<TModel extends DatabaseModel & PojoModel> implements AuthoritySelector<TModel> {
    private final ModelGraph<TModel> mModelGraph;
    private final ClassToTable<TModel> mClassToTable;
    private String mIdColumnName = BaseColumns._ID;
    private String mAuthority;

    Builder(ModelGraph<TModel> modelGraph, ClassToTable<TModel> classToTable) {
      mModelGraph = modelGraph;
      mClassToTable = classToTable;
    }

    public Builder<TModel> defaultIdColumn(String idColumnName) {
      mIdColumnName = idColumnName;
      return this;
    }

    public AutoUris<TModel> build() {
      return new AutoUris<TModel>(mModelGraph, mClassToTable, mAuthority, mIdColumnName);
    }

    @Override
    public Builder<TModel> forContentProvider(String authority) {
      mAuthority = authority;
      return this;
    }
  }

  public interface AuthoritySelector<TModel extends DatabaseModel & PojoModel> {
    Builder<TModel> forContentProvider(String authority);
  }

  @Override
  public ModelUri model(Class<?> klass) {
    Preconditions.checkNotNull(klass);
    Preconditions.checkArgument(mClassToTableMap.hasClass(klass), "Model %s is not present in supplied model graph", klass.getSimpleName());
    return new ModelUriImpl(klass);
  }

  @Override
  public CustomUri path(Object path) {
    return new CustomUriImpl().path(path);
  }

  @Override
  public CustomUri path(Class<?> model) {
    return new CustomUriImpl().path(model);
  }

  private abstract class AutoUriImpl implements AutoUri {
    protected Table<Class<?>, Optional<String>, EntityRelation> mRelatedEntities;

    protected AutoUriImpl() {
      mRelatedEntities = HashBasedTable.create();
    }

    protected AutoUriImpl(AutoUriImpl other) {
      mRelatedEntities = HashBasedTable.create(other.mRelatedEntities);
    }

    @Override
    public Collection<EntityRelation> getRelatedEntities() {
      return Lists.newCopyOnWriteArrayList(mRelatedEntities.values());
    }

    @Override
    public Optional<EntityRelation> getRelatedEntity(Class<?> model) {
      Preconditions.checkNotNull(model);
      return Optional.fromNullable(mRelatedEntities.get(model, Optional.absent()));
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
        EntityRelation entityRelation = mRelatedEntities.values().iterator().next();
        if (entityRelation.entityUri.getIdColumn().equals(mIdColumnName)
            && entityRelation.entityUri.getRelatedEntities().isEmpty()
            && !entityRelation.relationColumn.isPresent()) {
          builder
              .appendPath(mClassToTableMap.getTableForClass(entityRelation.entityUri.getModelUri().getModel()))
              .appendPath(String.valueOf(entityRelation.entityUri.getId()));

          return true;
        }
      }
      return false;
    }

    protected void appendRelationsAsParams(Uri.Builder builder) {
      for (EntityRelation entityRelation : mRelatedEntities.values()) {
        builder.appendQueryParameter(RELATED_TO_QUERY_PARAM, entityRelation.toParameterString());
      }
    }

    protected <T extends AutoUriImpl> T handleRelatedTo(Optional<String> relationColumnOptional, EntityUri relatedUri, T uri, boolean validateRelations) {
      Preconditions.checkNotNull(uri);

      Class<?> relationModel = relatedUri.getModelUri().getModel();

      if (relationColumnOptional.isPresent()) {
        String relationColumn = relationColumnOptional.get();

        if (validateRelations) {
          Class<?> model = uri.getModel();
          Set<String> relationColumns = mRelationsByClasses.get(model, relationModel);
          Preconditions.checkArgument((relationColumns != null && relationColumns.contains(relationColumn)), "No relation found between %s and %s.%s", model.getSimpleName(), relationModel.getSimpleName(), relationColumn);
        }
        EntityRelation previousValue = uri.mRelatedEntities.put(relationModel, relationColumnOptional, new EntityRelation(relationColumn, relatedUri));
        Preconditions.checkArgument(previousValue == null, "Duplicate relation for model %s.%s", relationModel.getSimpleName(), relationColumn);
      } else {
        if (validateRelations) {
          Class<?> model = uri.getModel();
          Set<String> relationColumns = mRelationsByClasses.get(model, relationModel);
          Preconditions.checkArgument(relationColumns != null, "No relation found between %s and %s", model.getSimpleName(), relationModel.getSimpleName());
          Preconditions.checkArgument(relationColumns.size() == 1, "Ambiguous relation between %s and %s", model.getSimpleName(), relationModel.getSimpleName());
        }
        EntityRelation previousValue = uri.mRelatedEntities.put(relationModel, relationColumnOptional, new EntityRelation(relatedUri));
        Preconditions.checkArgument(previousValue == null, "Duplicate relation for model %s", relationModel.getSimpleName());
      }

      return uri;
    }
  }

  private class CustomUriImpl extends AutoUriImpl implements CustomUri {
    private final LinkedList<String> pathSegments;

    private CustomUriImpl() {
      pathSegments = Lists.newLinkedList();
    }

    private CustomUriImpl(CustomUriImpl other) {
      super(other);

      pathSegments = new LinkedList<>(other.pathSegments);
    }

    @Override
    public CustomUri model(Class<?> model) {
      Preconditions.checkNotNull(model);
      Preconditions.checkArgument(mClassToTableMap.hasClass(model), "Model %s is not present in supplied model graph", model.getSimpleName());

      CustomUriImpl customUri = new CustomUriImpl(this);
      customUri.pathSegments.add(mClassToTableMap.getTableForClass(model));
      return customUri;
    }

    @Override
    public CustomUri id(long id) {
      return path(id);
    }

    @Override
    public CustomUri path(Object path) {
      Preconditions.checkNotNull(path);
      Preconditions.checkArgument(
          !mClassToTableMap.hasTable(path.toString()),
          "%s is reserved for model(%s.class) and path(%s.class) calls",
          path.toString(),
          mClassToTableMap.getClassForTable(path.toString()),
          mClassToTableMap.getClassForTable(path.toString())
      );

      CustomUriImpl customUri = new CustomUriImpl(this);
      customUri.pathSegments.add(path.toString());
      return customUri;
    }

    @Override
    public CustomUri path(Class<?> model) {
      Preconditions.checkNotNull(model);
      Preconditions.checkArgument(mClassToTableMap.hasClass(model), "Model %s is not present in supplied model graph", model.getSimpleName());

      CustomUriImpl customUri = new CustomUriImpl(this);
      customUri.pathSegments.add(mClassToTableMap.getTableForClass(model));
      return customUri;
    }

    @Override
    public Uri toUri() {
      Uri.Builder builder = new Uri.Builder()
          .scheme(ContentResolver.SCHEME_CONTENT)
          .authority(mAuthority);

      if (!appendRelationsAsPath(builder)) {
        appendRelationsAsParams(builder);
      }

      for (String pathSegment : pathSegments) {
        builder.appendPath(pathSegment);
      }

      return builder.build();
    }

    @Override
    public ModelUri getModelUri() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getModel() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T accept(AutoUriVisitor<T> visitor) {
      return visitor.visit(this);
    }

    @Override
    public CustomUri relatedTo(EntityUri uri) {
      return handleRelatedTo(Optional.<String>absent(), uri, new CustomUriImpl(this), false);
    }

    @Override
    public CustomUri relatedTo(String relationColumn, EntityUri uri) {
      return handleRelatedTo(Optional.of(relationColumn), uri, new CustomUriImpl(this), false);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(pathSegments, super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      if (!super.equals(obj)) return false;

      final CustomUriImpl other = (CustomUriImpl) obj;

      return Objects.equal(this.pathSegments, other.pathSegments);
    }

    @Override
    public String toString() {
      return MoreObjects
          .toStringHelper(this)
          .add("path", Joiner.on("/").join(pathSegments))
          .add("relatedTo", Joiner.on(", ").join(mRelatedEntities.values()))
          .toString();
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
          .appendPath(mClassToTableMap.getTableForClass(getModel()))
          .build();
    }

    @Override
    public ModelUri getModelUri() {
      return this;
    }

    @Override
    public ModelUri relatedTo(EntityUri uri) {
      return handleRelatedTo(Optional.<String>absent(), uri, new ModelUriImpl(this), true);
    }

    @Override
    public ModelUri relatedTo(String relationColumn, EntityUri uri) {
      return handleRelatedTo(Optional.of(relationColumn), uri, new ModelUriImpl(this), true);
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
    public CustomUri path(Object path) {
      CustomUriImpl customUri = new CustomUriImpl();

      customUri.mRelatedEntities = HashBasedTable.create(mRelatedEntities);

      return customUri.model(getModel()).path(path);
    }

    @Override
    public CustomUri path(Class<?> model) {
      CustomUriImpl customUri = new CustomUriImpl();

      customUri.mRelatedEntities = HashBasedTable.create(mRelatedEntities);

      return customUri.model(getModel()).path(model);
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
      return MoreObjects
          .toStringHelper(this)
          .add("model", getModel().getSimpleName())
          .add("relatedTo", Joiner.on(", ").join(mRelatedEntities.values()))
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
          .appendPath(mClassToTableMap.getTableForClass(getModelUri().getModel()))
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
      return handleRelatedTo(Optional.<String>absent(), uri, new EntityUriImpl(this), true);
    }

    @Override
    public EntityUri relatedTo(String relationColumn, EntityUri uri) {
      return handleRelatedTo(Optional.of(relationColumn), uri, new EntityUriImpl(this), true);
    }

    @Override
    public ModelUri model(Class<?> klass) {
      return AutoUris.this.model(klass).relatedTo(this);
    }

    @Override
    public CustomUri path(Object path) {
      return new CustomUriImpl().path(path).relatedTo(this);
    }

    @Override
    public CustomUri path(Class<?> model) {
      return new CustomUriImpl().path(model).relatedTo(this);
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
      return MoreObjects
          .toStringHelper(this)
          .add("id", mId)
          .add("idColumn", mIdColumnName)
          .add("model", getModelUri().getModel().getSimpleName())
          .add("relatedTo", Joiner.on(", ").join(mRelatedEntities.values()))
          .toString();
    }
  }

  public ModelUri getModelUri(Uri uri) {
    AutoUri autoUri = getAutoUri(uri);

    Preconditions.checkArgument(autoUri instanceof ModelUri, "%s is not a ModelUri: %s", uri, autoUri);

    return (ModelUri) autoUri;
  }

  public EntityUri getEntityUri(Uri uri) {
    AutoUri autoUri = getAutoUri(uri);

    Preconditions.checkArgument(autoUri instanceof EntityUri, "%s is not an EntityUri: %s", uri, autoUri);

    return (EntityUri) autoUri;
  }

  // AutoUri parsing creates a lot of short lived temporary objects, and
  // on the other hand both Uri and AutoUri are small immutable objects.
  // Therefore, it makes sense to have cache in the middle.
  //
  // Size of 20 translates to 99% hit ratio during sync.
  private LruCache<Uri, AutoUri> mParsingCache = new LruCache<Uri, AutoUri>(20) {
    @Override
    protected AutoUri create(Uri uri) {
      return mRootAutoUriParser.parse(uri);
    }
  };

  public AutoUri getAutoUri(Uri uri) {
    Preconditions.checkNotNull(uri);
    Preconditions.checkArgument(uri.getScheme().equals(ContentResolver.SCHEME_CONTENT),
        "invalid uri scheme: %s, (expected: %s), uri: %s", uri.getScheme(), ContentResolver.SCHEME_CONTENT, uri);
    Preconditions.checkArgument(uri.getAuthority().equals(mAuthority),
        "invalid uri authority: %s (expected: %s), uri: %s", uri.getAuthority(), mAuthority, uri);

    return mParsingCache.get(uri);
  }

  private abstract class AutoUriParser<T extends AutoUriRelationBuilder<T> & AutoUri> {
    protected final T mUri;

    protected AutoUriParser(T result) {
      mUri = result;
    }

    public final AutoUri parse(Uri uri) {
      AutoUriParser parser = this;
      Iterator<String> iterator = uri.getPathSegments().iterator();

      while (iterator.hasNext()) {
        String pathSegment = iterator.next();
        parser = parser.parseSegment(uri, pathSegment, !iterator.hasNext());
      }

      return getAutoUriWithRelatedEntities(parser, uri);
    }

    private <TResult extends AutoUriRelationBuilder<TResult> & AutoUri> AutoUri getAutoUriWithRelatedEntities(AutoUriParser<TResult> parser, Uri uri) {
      TResult autoUri = parser.mUri;

      for (String relationQueryParam : uri.getQueryParameters(RELATED_TO_QUERY_PARAM)) {
        EntityRelation relatedEntity = EntityRelation.fromParameter(AutoUris.this, relationQueryParam);

        if (relatedEntity.relationColumn.isPresent()) {
          autoUri = autoUri.relatedTo(relatedEntity.relationColumn.get(), relatedEntity.entityUri);
        } else {
          autoUri = autoUri.relatedTo(relatedEntity.entityUri);
        }
      }

      return autoUri;
    }

    protected boolean isModel(String pathSegment) {
      return mClassToTableMap.hasTable(pathSegment);
    }

    protected Class<?> getModel(String pathSegment) {
      return mClassToTableMap.getClassForTable(pathSegment);
    }

    protected abstract AutoUriParser parseSegment(Uri uri, String pathSegment, boolean isLastSegment);
  }

  private final AutoUriParser<CustomUri> mRootAutoUriParser = new AutoUriParser<CustomUri>(new CustomUriImpl()) {
    @Override
    protected AutoUriParser parseSegment(Uri uri, String pathSegment, boolean isLastSegment) {
      if (isModel(pathSegment)) {
        return new ModelUriParser(model(getModel(pathSegment)));
      } else {
        return new CustomUriParser(path(pathSegment));
      }
    }
  };

  private class ModelUriParser extends AutoUriParser<ModelUri> {
    private ModelUriParser(ModelUri uri) {
      super(uri);
    }

    @Override
    protected AutoUriParser parseSegment(Uri uri, String pathSegment, boolean isLastSegment) {
      if (isModel(pathSegment)) {
        return new CustomUriParser(mUri.path(getModel(pathSegment)));
      } else if (isDigitsWithSignOnly(pathSegment)) {
        String idColumn = isLastSegment && uri.getQueryParameter(ID_COLUMN_QUERY_PARAM) != null
            ? uri.getQueryParameter(ID_COLUMN_QUERY_PARAM)
            : mIdColumnName;

        return new EntityUriParser(mUri.id(idColumn, Long.parseLong(pathSegment)));
      } else {
        return new CustomUriParser(mUri.path(pathSegment));
      }
    }
  }

  private class EntityUriParser extends AutoUriParser<EntityUri> {
    public EntityUriParser(EntityUri uri) {
      super(uri);
    }

    @Override
    protected AutoUriParser parseSegment(Uri uri, String pathSegment, boolean isLastSegment) {
      if (isModel(pathSegment)) {
        Class<?> relationModel = mUri.getModelUri().getModel();
        Class<?> model = getModel(pathSegment);
        Set<String> relationColumns = mRelationsByClasses.get(model, relationModel);

        if (relationColumns == null || relationColumns.size() != 1) {
          return new CustomUriParser(mUri.path(model));
        } else {
          return new ModelUriParser(mUri.model(model));
        }
      } else {
        return new CustomUriParser(mUri.path(pathSegment));
      }
    }
  }

  private class CustomUriParser extends AutoUriParser<CustomUri> {
    private CustomUriParser(CustomUri uri) {
      super(uri);
    }

    @Override
    protected AutoUriParser parseSegment(Uri uri, String pathSegment, boolean isLastSegment) {
      if (isModel(pathSegment)) {
        return new CustomUriParser(mUri.model(getModel(pathSegment)));
      } else {
        return new CustomUriParser(mUri.path(pathSegment));
      }
    }
  }

  public long getEntityId(Uri uri, Class<?> model) {
    return getEntityId(getAutoUri(uri), model);
  }

  public long getEntityId(@NonNull final AutoUri autoUri, final Class<?> model) {
    EntityUri entityUri = autoUri
        .accept(new AutoUriVisitor<EntityUri>() {
          private EntityUri mResult;

          @Override
          public EntityUri visit(EntityUri uri) {
            if (uri.getModel().equals(model)) {
              Preconditions.checkArgument(
                  mResult == null,
                  "Duplicate %s.class relation in %s (%s)",
                  model.getSimpleName(),
                  autoUri.toUri(),
                  autoUri
              );
              mResult = uri;
            }

            processRelatedEntities(uri);

            return mResult;
          }

          @Override
          public EntityUri visit(ModelUri uri) {
            processRelatedEntities(uri);
            return mResult;
          }

          @Override
          public EntityUri visit(CustomUri uri) {
            processRelatedEntities(uri);
            return mResult;
          }

          private void processRelatedEntities(AutoUri autoUri) {
            for (EntityRelation entityRelation : autoUri.getRelatedEntities()) {
              entityRelation.entityUri.accept(this);
            }
          }
        });

    Preconditions.checkArgument(entityUri != null);
    Preconditions.checkArgument(entityUri.getIdColumn().equals(mIdColumnName));

    return entityUri.getId();
  }

  private static boolean isDigitsWithSignOnly(String str) {
    return str.matches("-?\\d+?");  //match a number with optional '-'.
  }
}
