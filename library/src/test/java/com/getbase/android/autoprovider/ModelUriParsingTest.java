package com.getbase.android.autoprovider;

import static com.google.common.truth.Truth.assertThat;

import com.getbase.android.autoprovider.TestModels.Lead;
import com.getbase.android.autoprovider.TestModels.TestModel;
import com.getbase.android.autoprovider.TestModels.User;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.net.Uri;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ModelUriParsingTest {

  private static final AutoUris<TestModel> AUTO_URIS = AutoUris
      .from(TestModels.MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .build();

  @Test(expected = NullPointerException.class)
  public void shouldRejectNullUri() throws Exception {
    AUTO_URIS.getModelUri(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectNonContentProviderUris() throws Exception {
    AUTO_URIS.getModelUri(Uri.parse("http://google.com"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectUrisForOtherAuthorities() throws Exception {
    AUTO_URIS.getModelUri(Uri.parse("content://com.android.contacts/data/emails/filter/acme?directory=3"));
  }

  @Test
  public void modelUrisShouldHandleAmbiguousColumns() {
    Uri uri = AUTO_URIS.model(Lead.class)
        .relatedTo(Lead.OWNER_ID, AUTO_URIS.model(User.class).id(1))
        .relatedTo(Lead.USER_ID, AUTO_URIS.model(User.class).id(2)).toUri();
    assertThat(uri).isNotNull();
    ModelUri modelUri = AUTO_URIS.getModelUri(uri);
    assertThat(modelUri).isNotNull();
  }
}
