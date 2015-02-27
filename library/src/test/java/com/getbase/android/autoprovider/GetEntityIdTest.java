package com.getbase.android.autoprovider;

import static com.getbase.android.autoprovider.TestModels.MODEL_GRAPH;
import static org.fest.assertions.api.Assertions.assertThat;

import com.getbase.android.autoprovider.TestModels.Contact;
import com.getbase.android.autoprovider.TestModels.Deal;
import com.getbase.android.autoprovider.TestModels.DealContact;
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
public class GetEntityIdTest {

  private AutoUris<TestModel> mAutoUris = AutoUris
      .from(MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .build();

  @Test
  public void shouldGetEntityIdFromSoleRelationOfModelUri() throws Exception {
    AutoUri uri = mAutoUris
        .model(Contact.class).id(1500)
        .model(Deal.class);

    assertThat(mAutoUris.getEntityId(uri, Contact.class)).isEqualTo(1500);
  }

  @Test
  public void shouldGetEntityIdFromMultipleRelationsOfModelUri() throws Exception {
    AutoUri uri = mAutoUris
        .model(Contact.class).id(1500)
        .model(Deal.class)
        .relatedTo(mAutoUris.model(User.class).id(2900));

    assertThat(mAutoUris.getEntityId(uri, Contact.class)).isEqualTo(1500);
  }

  @Test
  public void shouldGetEntityIdFromEntityId() throws Exception {
    AutoUri uri = mAutoUris
        .model(Deal.class).id(1500);

    assertThat(mAutoUris.getEntityId(uri, Deal.class)).isEqualTo(1500);
  }

  @Test
  public void shouldGetEntityIdFromSoleRelationOfEntityUri() throws Exception {
    AutoUri uri = mAutoUris
        .model(Deal.class).id(1500)
        .relatedTo(mAutoUris.model(User.class).id(2900));

    assertThat(mAutoUris.getEntityId(uri, Deal.class)).isEqualTo(1500);
  }

  @Test
  public void shouldGetEntityIdFromMultipleRelationsOfEntityUri() throws Exception {
    AutoUri uri = mAutoUris
        .model(Deal.class).id(1500)
        .relatedTo(mAutoUris.model(User.class).id(2900))
        .relatedTo(mAutoUris.model(Contact.class).id(42));

    assertThat(mAutoUris.getEntityId(uri, Deal.class)).isEqualTo(1500);
  }

  @Test(expected = NullPointerException.class)
  public void shouldRejectNullUri() throws Exception {
    mAutoUris.getEntityId((Uri) null, Deal.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectUriWithoutRelationToRequestedModel() throws Exception {
    mAutoUris.getEntityId(mAutoUris.model(Lead.class).id(1500), Deal.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectUriWithMultipleRelationsToTheRequestedModel() throws Exception {
    AutoUri uri = mAutoUris.model(Lead.class)
        .relatedTo(Lead.USER_ID, mAutoUris.model(User.class).id(1500))
        .relatedTo(Lead.OWNER_ID, mAutoUris.model(User.class).id(2900));

    mAutoUris.getEntityId(uri, Deal.class);
  }

  @Test
  public void shouldIgnoreMultipleRelationsToOtherModels() throws Exception {
    AutoUri uri = mAutoUris.model(Lead.class)
        .id(42)
        .relatedTo(Lead.USER_ID, mAutoUris.model(User.class).id(1500))
        .relatedTo(Lead.OWNER_ID, mAutoUris.model(User.class).id(2900));

    assertThat(mAutoUris.getEntityId(uri, Lead.class)).isEqualTo(42);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectUriWithMultipleRelationsToTheRequestedModelAcrossTheUriHierarchy() throws Exception {
    AutoUri uri = mAutoUris.model(DealContact.class)
        .relatedTo(mAutoUris
                .model(Contact.class)
                .id(1500)
                .relatedTo(mAutoUris
                        .model(User.class)
                        .id(666)
                )
        )
        .relatedTo(mAutoUris
                .model(Deal.class)
                .id(2900)
                .relatedTo(mAutoUris
                        .model(User.class)
                        .id(667)
                )
        );

    mAutoUris.getEntityId(uri, User.class);
  }

  @Test
  public void shouldIgnoreMultipleRelationsToOtherModelsAcrossTheUriHierarchy() throws Exception {
    AutoUri uri = mAutoUris.model(DealContact.class)
        .relatedTo(mAutoUris
                .model(Contact.class)
                .id(1500)
                .relatedTo(mAutoUris
                        .model(User.class)
                        .id(666)
                )
        )
        .relatedTo(mAutoUris
                .model(Deal.class)
                .id(2900)
                .relatedTo(mAutoUris
                        .model(User.class)
                        .id(667)
                )
        );

    assertThat(mAutoUris.getEntityId(uri, Deal.class)).isEqualTo(2900);
  }
}
