package com.getbase.android.autoprovider;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

public class EntityRelation {
  public final Optional<String> relationColumn;
  public final EntityUri entityUri;

  public EntityRelation(EntityUri entityUri) {
    this.entityUri = entityUri;
    this.relationColumn = Optional.absent();
  }

  public EntityRelation(String relationColumn, EntityUri entityUri) {
    this.entityUri = entityUri;
    this.relationColumn = Optional.of(relationColumn);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(relationColumn, entityUri);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof EntityRelation) {
      EntityRelation otherRelation = ((EntityRelation) o);
      return Objects.equal(relationColumn, otherRelation.relationColumn)
          && Objects.equal(entityUri, otherRelation.entityUri);
    }
    return false;
  }

  @Override
  public String toString() {
    return entityUri.getModel().getSimpleName() + ": " + entityUri;
  }

  public String toParameterString() {
    if (relationColumn.isPresent()) {
      try {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uri", entityUri.toUri().toString());
        jsonObject.put("by", relationColumn.get());
        return jsonObject.toString();
      } catch (JSONException e) {
        throw new IllegalArgumentException(e);
      }
    } else {
      return entityUri.toUri().toString();
    }
  }

  public static EntityRelation fromParameter(AutoUris autoUris, String param) {
    try {
      JSONObject object = new JSONObject(param);
      return new EntityRelation(object.getString("by"), autoUris.getEntityUri(Uri.parse(object.getString("uri"))));
    } catch (JSONException e) {
      return new EntityRelation(autoUris.getEntityUri(Uri.parse(param)));
    }
  }
}
