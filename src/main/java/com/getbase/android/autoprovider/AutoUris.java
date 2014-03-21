package com.getbase.android.autoprovider;

import com.getbase.autoindexer.DbTableModel;
import com.getbase.forger.thneed.MicroOrmModel;

import org.chalup.thneed.ModelGraph;

import android.net.Uri;

public class AutoUris<TModel extends DbTableModel & MicroOrmModel> implements ModelUriBuilder {
  private final ModelGraph<TModel> mModelGraph;

  private AutoUris(ModelGraph<TModel> modelGraph) {
    mModelGraph = modelGraph;
  }

  public static <T extends DbTableModel & MicroOrmModel> AutoUris from(ModelGraph<T> modelGraph) {
    return new AutoUris(modelGraph);
  }

  @Override
  public ModelUri model(Class<?> klass) {
    return null;
  }

  public AutoUri fromUri(Uri uri) {
    return null;
  }
}
