package com.getbase.android.autoprovider;

import static com.getbase.android.autoprovider.TestModels.MODEL_GRAPH;
import static com.google.common.truth.Truth.assertThat;

import com.getbase.android.autoprovider.AutoUris;
import com.getbase.android.autoprovider.CustomUri;
import com.getbase.android.autoprovider.EntityRelation;
import com.getbase.android.autoprovider.EntityUri;
import com.getbase.android.autoprovider.TestModels.Contact;
import com.getbase.android.autoprovider.TestModels.Tag;
import com.getbase.android.autoprovider.TestModels.Tagging;
import com.getbase.android.autoprovider.TestModels.TestModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.provider.BaseColumns;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class CustomUriBuilderTest {

  private AutoUris<TestModel> mAutoUris;

  @Before
  public void setUp() throws Exception {
    mAutoUris = AutoUris.from(MODEL_GRAPH).forContentProvider(TestModels.AUTHORITY).build();
  }

  @Test
  public void autoUrisShouldSupportCustomPaths() {
    CustomUri customUri = mAutoUris.model(Contact.class).id(10).path("custom");
    assertThat(customUri.toUri().getPath()).isEqualTo("/contact/10/custom");

    customUri = mAutoUris.model(Contact.class).id(10).path("custom").path(11);
    assertThat(customUri.toUri().getPath()).isEqualTo("/contact/10/custom/11");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldFailOnGettingModelFromCustomUri() {
    mAutoUris.path("dude").getModel();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldFailOnGettingModelUriFromCustomUri() {
    mAutoUris.path("dude").getModelUri();
  }

  @Test
  public void customUriWithSingleRelationShouldBePutIntoPath() {
    CustomUri customUri = mAutoUris.path("custom").relatedTo(mAutoUris.model(Contact.class).id(10));
    assertThat(customUri.toUri().getPath()).isEqualTo("/contact/10/custom");
  }

  @Test
  public void customUriBuiltFromEntityUriShouldBeRelatedToThisEntity() {
    CustomUri customUri = mAutoUris.model(Contact.class).id(10).path("custom");
    assertThat(customUri.getRelatedEntities()).hasSize(1);

    EntityRelation relation = customUri.getRelatedEntities().iterator().next();
    assertThat(relation.relationColumn.isPresent()).isFalse();
    assertThat(relation.entityUri.getModel()).isEqualTo((Class) Contact.class);
    assertThat(relation.entityUri.getId()).isEqualTo(10);
    assertThat(relation.entityUri.getIdColumn()).isEqualTo(BaseColumns._ID);
  }

  @Test
  public void customUriShouldKeepRelationsFromModelUriItWasBuiltFrom() {
    EntityUri tag = mAutoUris.model(Tag.class).id(1500);
    EntityUri contact = mAutoUris.model(Contact.class).id(1500);

    CustomUri customUri = mAutoUris
        .model(Tagging.class)
        .relatedTo(tag)
        .relatedTo(contact)
        .path("custom");

    assertThat(customUri.getRelatedEntities()).hasSize(2);
    assertThat(customUri.getRelatedEntity(Tag.class).isPresent()).isTrue();
    assertThat(customUri.getRelatedEntity(Tag.class).get().entityUri).isEqualTo(tag);
    assertThat(customUri.getRelatedEntity(Contact.class).isPresent()).isTrue();
    assertThat(customUri.getRelatedEntity(Contact.class).get().entityUri).isEqualTo(contact);
  }

  @Test
  public void customUriShouldNotCopyRelationsFromEntityUriItWasBuiltFrom() {
    EntityUri tag = mAutoUris.model(Tag.class).id(1500);
    EntityUri tagging = mAutoUris
        .model(Tagging.class)
        .id(2900)
        .relatedTo(tag);

    CustomUri customUri = tagging.path("custom");

    assertThat(customUri.getRelatedEntities()).hasSize(1);
    assertThat(customUri.getRelatedEntity(Tagging.class).isPresent()).isTrue();
    assertThat(customUri.getRelatedEntity(Tag.class).isPresent()).isFalse();
  }

  @Test(expected = NullPointerException.class)
  public void shouldRejectNullPath() {
    mAutoUris.path((String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectPathMappedToModel() {
    mAutoUris.path("contact");
  }

  @Test(expected = NullPointerException.class)
  public void shouldRejectNullModel() {
    mAutoUris.path(null);
  }

  @Test
  public void shouldCreateCustomUriFromModel() {
    assertThat(mAutoUris.path(Contact.class).toUri().getPath()).isEqualTo("/contact");
  }
}
