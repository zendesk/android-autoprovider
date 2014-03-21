package com.getbase.android.autoprovider;

import static com.getbase.android.autoprovider.TestModels.MODEL_GRAPH;
import static org.fest.assertions.Assertions.assertThat;

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

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectClassOutsideOfModelGraph() throws Exception {
    AutoUris<TestModel> autoUris = AutoUris.from(MODEL_GRAPH).build();

    autoUris.model(String.class);
  }

  @Test
  public void shouldBuildSimpleModelUri() throws Exception {
    AutoUris<TestModel> autoUris = AutoUris.from(MODEL_GRAPH).build();
    ModelUri modelUri = autoUris.model(Contact.class);

    assertThat(modelUri.getModel()).isEqualTo(Contact.class);
  }

  @Test
  public void shouldBuildEntityUriWithDefaultIdColumnName() throws Exception {
    AutoUris<TestModel> autoUris = AutoUris.from(MODEL_GRAPH).build();
    EntityUri entityUri = autoUris.model(Contact.class).id(1500);

    assertThat(entityUri.getId()).isEqualTo(1500);
    assertThat(entityUri.getIdColumn()).isEqualTo(BaseColumns._ID);
    assertThat(entityUri.getModelUri().getModel()).isEqualTo(Contact.class);
  }

  @Test
  public void shouldBuildEntityUriWithCustomIdColumnName() throws Exception {
    AutoUris<TestModel> autoUris = AutoUris.from(MODEL_GRAPH).build();
    EntityUri entityUri = autoUris.model(Contact.class).id("id", 1500);

    assertThat(entityUri.getId()).isEqualTo(1500);
    assertThat(entityUri.getIdColumn()).isEqualTo("id");
    assertThat(entityUri.getModelUri().getModel()).isEqualTo(Contact.class);
  }
}
