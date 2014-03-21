package com.getbase.android.autoprovider;

import com.getbase.autoindexer.DbTableModel;
import com.getbase.forger.thneed.MicroOrmModel;

import org.chalup.thneed.ModelGraph;

import android.net.Uri;
import android.provider.BaseColumns;

public class AutoUris<TModel extends DbTableModel & MicroOrmModel> implements ModelUriBuilder {
  private final ModelGraph<TModel> mModelGraph;
  private final String mIdColumnName;

  AutoUris(ModelGraph<TModel> modelGraph, String idColumnName) {
    mModelGraph = modelGraph;
    mIdColumnName = idColumnName;
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
    return null;
  }

  public AutoUri fromUri(Uri uri) {
    return null;
  }
}
