package com.getbase.android.autoprovider;

import static com.getbase.android.autoprovider.TestModels.MODEL_GRAPH;
import static com.google.common.truth.Truth.assertThat;

import com.getbase.android.autoprovider.TestModels.Contact;
import com.getbase.android.autoprovider.TestModels.Deal;
import com.getbase.android.autoprovider.TestModels.TestModel;
import com.getbase.android.autoprovider.TestModels.User;
import com.google.common.base.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PathRelationsTest {

  private AutoUris<TestModel> mAutoUris = AutoUris
      .from(MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .build();

  @Test
  public void addingModelShouldCreateRelationToEntityUri() throws Exception {
    ModelUri dealsForContact = mAutoUris.model(Contact.class).id(1500).model(Deal.class);

    assertThat(dealsForContact.getModel()).isEqualTo((Class) Deal.class);

    Optional<EntityRelation> relatedContact = dealsForContact.getRelatedEntity(Contact.class);
    assertThat(relatedContact.get().relationColumn.isPresent()).isFalse();
    assertThat(relatedContact.isPresent()).isTrue();
    assertThat(relatedContact.get().entityUri.getModel()).isEqualTo((Class) Contact.class);
    assertThat(relatedContact.get().entityUri.getId()).isEqualTo(1500);
  }

  @Test
  public void addingIdShouldPreserveRelationToEntityUri() throws Exception {
    EntityUri dealUri = mAutoUris.model(Contact.class).id(1500).model(Deal.class).id(2900);

    assertThat(dealUri.getModelUri().getModel()).isEqualTo((Class) Deal.class);

    Optional<EntityRelation> relatedContact = dealUri.getRelatedEntity(Contact.class);
    assertThat(relatedContact.isPresent()).isTrue();
    assertThat(relatedContact.get().relationColumn.isPresent()).isFalse();
    assertThat(relatedContact.get().entityUri.getModel()).isEqualTo((Class) Contact.class);
    assertThat(relatedContact.get().entityUri.getId()).isEqualTo(1500);
  }

  @Test
  public void shouldNotMatterIfRelatedToIsSpecifiedBeforeOrAfterId() throws Exception {
    EntityUri userUri = mAutoUris.model(User.class).id(1500);
    EntityUri afterIdUri = mAutoUris.model(Deal.class).id(2900).relatedTo(userUri);
    EntityUri beforeIdUri = mAutoUris.model(Deal.class).relatedTo(userUri).id(2900);

    assertThat(afterIdUri).isEqualTo(beforeIdUri);
  }
}
