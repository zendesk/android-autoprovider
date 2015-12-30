package com.getbase.android.autoprovider;

import static com.getbase.android.autoprovider.TestModels.MODEL_GRAPH;
import static com.google.common.truth.Truth.assertThat;

import com.getbase.android.autoprovider.AutoUris;
import com.getbase.android.autoprovider.EntityUri;
import com.getbase.android.autoprovider.ModelUri;
import com.getbase.android.autoprovider.TestModels.Contact;
import com.getbase.android.autoprovider.TestModels.TestModel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.provider.BaseColumns;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class UriBuildingTest {

  private final AutoUris<TestModel> mAutoUris = AutoUris
      .from(MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .build();

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectClassOutsideOfModelGraph() throws Exception {
    mAutoUris.model(String.class);
  }

  @Test
  public void shouldBuildSimpleModelUri() throws Exception {
    ModelUri modelUri = mAutoUris.model(Contact.class);

    assertThat(modelUri.getModel()).isEqualTo((Class) Contact.class);
  }

  @Test
  public void shouldBuildEntityUriWithDefaultIdColumnName() throws Exception {
    EntityUri entityUri = mAutoUris.model(Contact.class).id(1500);

    assertThat(entityUri.getId()).isEqualTo(1500);
    assertThat(entityUri.getIdColumn()).isEqualTo(BaseColumns._ID);
    assertThat(entityUri.getModel()).isEqualTo((Class) Contact.class);
  }

  @Test
  public void shouldBuildEntityUriWithCustomIdColumnName() throws Exception {
    EntityUri entityUri = mAutoUris.model(Contact.class).id("id", 1500);

    assertThat(entityUri.getId()).isEqualTo(1500);
    assertThat(entityUri.getIdColumn()).isEqualTo("id");
    assertThat(entityUri.getModel()).isEqualTo((Class) Contact.class);
  }
}
