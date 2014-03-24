package com.getbase.android.autoprovider;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import junit.framework.TestCase;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class UriMarshallingTest {

  private static final AutoUris AUTO_URIS = AutoUris
      .from(TestModels.MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .build();

  private static final List<? extends AutoUri> TEST_CASES = ImmutableList.of(
  );

  @Test
  public void shouldMarshallAndUnmarshallAutoUri() throws Exception {
    FluentIterable<String> failures = FluentIterable.from(TEST_CASES)
        .transform(new Function<AutoUri, String>() {
          @Override
          public String apply(AutoUri autoUri) {
            try {
              AutoUri unmarshalledUri = AUTO_URIS.fromUri(autoUri.toUri());
              if (!autoUri.equals(unmarshalledUri)) {
                return autoUri + " != " + unmarshalledUri;
              }
            } catch (Exception e) {
              return autoUri + " : " + Throwables.getStackTraceAsString(e);
            }
            return null;
          }
        })
        .filter(Predicates.notNull());

    if (!failures.isEmpty()) {
      TestCase.fail(failures.size() + " failures:\n" + Joiner.on("\n").join(failures));
    }
  }
}
