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
public class AutoUrisBuilderTest {

  @Test
  public void autoUrisShouldUseAndroidsBaseColumnsIdAsDefaultIdColumnIfNotSpecified() throws Exception {
    AutoUris<TestModel> autoUris = AutoUris.from(MODEL_GRAPH).build();
    EntityUri entityUri = autoUris.model(Contact.class).id(1500);

    assertThat(entityUri.getIdColumn()).isEqualTo(BaseColumns._ID);
  }

  @Test
  public void autoUrisShouldUseUserDefinedIdColumnAsDefaultForEntityUris() throws Exception {
    AutoUris<TestModel> autoUris = AutoUris.from(MODEL_GRAPH).defaultIdColumn("das_id").build();
    EntityUri entityUri = autoUris.model(Contact.class).id(1500);

    assertThat(entityUri.getIdColumn()).isEqualTo("das_id");
  }
}
