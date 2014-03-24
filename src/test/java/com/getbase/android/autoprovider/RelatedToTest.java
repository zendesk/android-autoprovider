package com.getbase.android.autoprovider;

import static com.getbase.android.autoprovider.TestModels.MODEL_GRAPH;
import static org.fest.assertions.Assertions.assertThat;

import com.getbase.android.autoprovider.TestModels.Contact;
import com.getbase.android.autoprovider.TestModels.Deal;
import com.getbase.android.autoprovider.TestModels.Lead;
import com.getbase.android.autoprovider.TestModels.TestModel;
import com.getbase.android.autoprovider.TestModels.User;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collection;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RelatedToTest {

  private AutoUris<TestModel> mAutoUris = AutoUris
      .from(MODEL_GRAPH)
      .build();

  @Test
  public void shouldAddRelationToModelUri() throws Exception {
    ModelUri uri = mAutoUris.model(Contact.class).relatedTo(mAutoUris.model(User.class).id(1500));

    assertThat(uri.getRelatedEntity(User.class).isPresent()).isTrue();
  }

  @Test
  public void shouldReturnAllRelationsAddedToModelUri() throws Exception {
    ModelUri uri = mAutoUris
        .model(Deal.class)
        .relatedTo(mAutoUris.model(Contact.class).id(1500))
        .relatedTo(mAutoUris.model(User.class).id(2900));

    Collection<EntityUri> relatedEntities = uri.getRelatedEntities();
    relatedEntities.contains(mAutoUris.model(Contact.class).id(1500));
    relatedEntities.contains(mAutoUris.model(User.class).id(2900));
  }

  @Test
  public void shouldCopyModelUriOnAddingRelation() throws Exception {
    ModelUri deals = mAutoUris.model(Deal.class);
    ModelUri dealsRelatedToContact = deals.relatedTo(mAutoUris.model(Contact.class).id(1500));
    ModelUri dealsRelatedToUser = deals.relatedTo(mAutoUris.model(User.class).id(2900));

    assertThat(deals.getRelatedEntity(Contact.class).isPresent()).isFalse();
    assertThat(deals.getRelatedEntity(User.class).isPresent()).isFalse();

    assertThat(dealsRelatedToContact.getRelatedEntity(Contact.class).isPresent()).isTrue();
    assertThat(dealsRelatedToContact.getRelatedEntity(User.class).isPresent()).isFalse();

    assertThat(dealsRelatedToUser.getRelatedEntity(Contact.class).isPresent()).isFalse();
    assertThat(dealsRelatedToUser.getRelatedEntity(User.class).isPresent()).isTrue();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectDuplicateRelationOnModelUri() throws Exception {
    mAutoUris
        .model(Deal.class)
        .relatedTo(mAutoUris.model(Contact.class).id(1500))
        .relatedTo(mAutoUris.model(Contact.class).id(2900));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectInvalidRelationOnModelUri() throws Exception {
    mAutoUris
        .model(Deal.class)
        .relatedTo(mAutoUris.model(Lead.class).id(1500));
  }

  @Test
  public void shouldAddRelationToEntityUri() throws Exception {
    EntityUri uri = mAutoUris.model(Contact.class).id(2900).relatedTo(mAutoUris.model(User.class).id(1500));

    assertThat(uri.getRelatedEntity(User.class).isPresent()).isTrue();
  }

  @Test
  public void shouldReturnAllRelationsAddedToEntityUri() throws Exception {
    EntityUri uri = mAutoUris
        .model(Deal.class).id(42)
        .relatedTo(mAutoUris.model(Contact.class).id(1500))
        .relatedTo(mAutoUris.model(User.class).id(2900));

    Collection<EntityUri> relatedEntities = uri.getRelatedEntities();
    relatedEntities.contains(mAutoUris.model(Contact.class).id(1500));
    relatedEntities.contains(mAutoUris.model(User.class).id(2900));
  }

  @Test
  public void shouldCopyEntityUriOnAddingRelation() throws Exception {
    EntityUri deal = mAutoUris.model(Deal.class).id(42);
    EntityUri dealRelatedToContact = deal.relatedTo(mAutoUris.model(Contact.class).id(1500));
    EntityUri dealRelatedToUser = deal.relatedTo(mAutoUris.model(User.class).id(2900));

    assertThat(deal.getRelatedEntity(Contact.class).isPresent()).isFalse();
    assertThat(deal.getRelatedEntity(User.class).isPresent()).isFalse();

    assertThat(dealRelatedToContact.getRelatedEntity(Contact.class).isPresent()).isTrue();
    assertThat(dealRelatedToContact.getRelatedEntity(User.class).isPresent()).isFalse();

    assertThat(dealRelatedToUser.getRelatedEntity(Contact.class).isPresent()).isFalse();
    assertThat(dealRelatedToUser.getRelatedEntity(User.class).isPresent()).isTrue();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectDuplicateRelationOnEntityUri() throws Exception {
    mAutoUris
        .model(Deal.class).id(42)
        .relatedTo(mAutoUris.model(Contact.class).id(1500))
        .relatedTo(mAutoUris.model(Contact.class).id(2900));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectInvalidRelationOnEntityUri() throws Exception {
    mAutoUris
        .model(Deal.class).id(42)
        .relatedTo(mAutoUris.model(Lead.class).id(1500));
  }
}
