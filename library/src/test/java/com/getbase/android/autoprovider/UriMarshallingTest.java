package com.getbase.android.autoprovider;

import com.getbase.android.autoprovider.TestModels.Contact;
import com.getbase.android.autoprovider.TestModels.Deal;
import com.getbase.android.autoprovider.TestModels.Tag;
import com.getbase.android.autoprovider.TestModels.Tagging;
import com.getbase.android.autoprovider.TestModels.TestModel;
import com.getbase.android.autoprovider.TestModels.User;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import junit.framework.TestCase;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class UriMarshallingTest {

  private static final AutoUris<TestModel> AUTO_URIS = AutoUris
      .from(TestModels.MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .build();

  private static final List<ModelUri> MODEL_TEST_CASES = ImmutableList.of(
      AUTO_URIS.model(User.class),
      AUTO_URIS.model(Contact.class).id(1500).model(Deal.class),
      AUTO_URIS.model(Contact.class).id("id", 1500).model(Deal.class),
      AUTO_URIS.model(Tagging.class).relatedTo(AUTO_URIS.model(Contact.class).id(1500)).relatedTo(AUTO_URIS.model(Tag.class).id(2900))
  );

  private static final List<EntityUri> ENTITY_TEST_CASES = ImmutableList.of(
      AUTO_URIS.model(User.class).id(1500),
      AUTO_URIS.model(User.class).id("id", 1500),
      AUTO_URIS.model(User.class).id(1500).model(Contact.class).id(2900).model(Deal.class).id(666),
      AUTO_URIS.model(Deal.class).id(666).relatedTo(AUTO_URIS.model(Contact.class).id(2900).relatedTo(AUTO_URIS.model(User.class).id(1500))),
      AUTO_URIS.model(Tagging.class).id(666).relatedTo(AUTO_URIS.model(Contact.class).id(1500)).relatedTo(AUTO_URIS.model(Tag.class).id(2900))
  );

  @Test
  public void shouldMarshallAndUnmarshallAutoUri() throws Exception {
    Iterable<String> failures = Iterables.concat(
        FluentIterable.from(MODEL_TEST_CASES)
            .transform(new Function<ModelUri, String>() {
              @Override
              public String apply(ModelUri autoUri) {
                try {
                  AutoUri unmarshalledUri = AUTO_URIS.getModelUri(autoUri.toUri());
                  if (!autoUri.equals(unmarshalledUri)) {
                    return autoUri + " != " + unmarshalledUri;
                  }
                } catch (Exception e) {
                  return autoUri + " : " + Throwables.getStackTraceAsString(e);
                }
                return null;
              }
            })
            .filter(Predicates.notNull()),
        FluentIterable.from(ENTITY_TEST_CASES)
            .transform(new Function<EntityUri, String>() {
              @Override
              public String apply(EntityUri autoUri) {
                try {
                  AutoUri unmarshalledUri = AUTO_URIS.getEntityUri(autoUri.toUri());
                  if (!autoUri.equals(unmarshalledUri)) {
                    return autoUri + " != " + unmarshalledUri;
                  }
                } catch (Exception e) {
                  return autoUri + " : " + Throwables.getStackTraceAsString(e);
                }
                return null;
              }
            })
            .filter(Predicates.notNull())
    );

    if (!Iterables.isEmpty(failures)) {
      TestCase.fail(Iterables.size(failures) + " failures:\n" + Joiner.on("\n").join(failures));
    }
  }
}
