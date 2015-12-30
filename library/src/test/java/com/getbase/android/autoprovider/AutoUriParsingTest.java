package com.getbase.android.autoprovider;

import static com.google.common.truth.Truth.assertThat;

import com.getbase.android.autoprovider.AutoUri;
import com.getbase.android.autoprovider.AutoUris;
import com.getbase.android.autoprovider.TestModels.TestModel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.net.Uri;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AutoUriParsingTest {

  private static final AutoUris<TestModel> AUTO_URIS = AutoUris
      .from(TestModels.MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .build();

  @Test(expected = NullPointerException.class)
  public void shouldRejectNullUri() throws Exception {
    AUTO_URIS.getAutoUri(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectNonContentProviderUris() throws Exception {
    AUTO_URIS.getAutoUri(Uri.parse("http://google.com"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectUrisForOtherAuthorities() throws Exception {
    AUTO_URIS.getAutoUri(Uri.parse("content://com.android.contacts/data/emails/filter/acme?directory=3"));
  }

  @Test
  public void shouldParseRootUriAsCustomUri() throws Exception {
    AutoUri autoUri = AUTO_URIS.getAutoUri(Uri.parse("content://" + TestModels.AUTHORITY));
    assertThat(autoUri.toUri().getPath()).isEqualTo("");
  }
}
