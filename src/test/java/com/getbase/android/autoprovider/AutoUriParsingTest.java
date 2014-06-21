package com.getbase.android.autoprovider;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.net.Uri;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AutoUriParsingTest {

  private static final AutoUris AUTO_URIS = AutoUris
      .from(TestModels.MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .build();

  @Test
  public void shouldRejectNullUri() throws Exception {
    assertThat(AUTO_URIS.getAutoUri(null).isPresent()).isFalse();
  }

  @Test
  public void shouldRejectNonContentProviderUris() throws Exception {
    assertThat(AUTO_URIS.getAutoUri(Uri.parse("http://google.com")).isPresent()).isFalse();
  }

  @Test
  public void shouldRejectUrisForOtherAuthorities() throws Exception {
    assertThat(AUTO_URIS.getAutoUri(Uri.parse("content://com.android.contacts/data/emails/filter/acme?directory=3")).isPresent()).isFalse();
  }
}
