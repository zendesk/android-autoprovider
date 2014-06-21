package com.getbase.android.autoprovider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.net.Uri;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ModelUriParsingTest {

  private static final AutoUris AUTO_URIS = AutoUris
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
}
