package com.getbase.android.autoprovider;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import org.chalup.thneed.models.DatabaseModel;
import org.chalup.thneed.models.PojoModel;

import java.util.ArrayList;
import java.util.List;

public class PathBuilder<T extends DatabaseModel & PojoModel> {

  public static final String NUMBER = "#";
  public static final String WILDCARD = "*";

  private final List<String> segments = new ArrayList<>();
  private final ClassToTable<T> mClassToTable;

  public PathBuilder(ClassToTable<T> classToTable) {
    mClassToTable = classToTable;
  }

  public PathBuilder<T> model(Class<?> klass) {
    Preconditions.checkState(mClassToTable.hasClass(klass), "class " + klass.getSimpleName() + " has to be registered in thneed");
    segments.add(mClassToTable.getTableForClass(klass));
    return this;
  }

  public PathBuilder<T> number() {
    segments.add(NUMBER);
    return this;
  }

  public PathBuilder<T> wildcard() {
    segments.add(WILDCARD);
    return this;
  }

  public PathBuilder<T> path(String path) {
    Preconditions.checkState(!mClassToTable.hasTable(path), "path:" + path + " is assigned to a model in thneed");
    Preconditions.checkNotNull(path);
    segments.add(path);
    return this;
  }

  public String build() {
    return Joiner.on("/").join(segments);
  }
}
