package com.getbase.android.autoprovider;

import static com.google.common.truth.Truth.assertThat;

import com.getbase.android.autoprovider.TestModels.BaseModel;
import com.getbase.android.autoprovider.TestModels.Contact;
import com.getbase.android.autoprovider.TestModels.ContactData;
import com.getbase.android.autoprovider.TestModels.Deal;
import com.getbase.android.autoprovider.TestModels.Lead;
import com.getbase.android.autoprovider.TestModels.Note;
import com.getbase.android.autoprovider.TestModels.TestModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.sqlite.SQLiteOpenHelper;

@Config(emulateSdk = 18, manifest = "./src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class CrudOperationsResolverTest {
  ContentResolver mContentResolver;
  SQLiteOpenHelper mDatabase;
  AutoNotificationUriSetter<TestModel> mAutoNotificationUriSetter;

  private static final AutoUris<TestModel> AUTO_URIS = AutoUris
      .from(TestModels.MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .defaultIdColumn(BaseModel._ID)
      .build();

  private static final ContentTypeProvider<TestModel> CONTENT_TYPE_PROVIDER = new ContentTypeProvider<>(
      TestModels.CONTENT_TYPE_PREFIX,
      new ClassToTable<>(TestModels.MODEL_GRAPH)
  );

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    TestProvider provider = ProviderMock.provide();
    mContentResolver = Robolectric.getShadowApplication().getContentResolver();
    mDatabase = provider.getDatabase();
    mAutoNotificationUriSetter = new AutoNotificationUriSetter<>(
        mDatabase,
        mContentResolver,
        AUTO_URIS,
        new ClassToTable<>(TestModels.MODEL_GRAPH)
    );
  }

  @Test
  public void shouldHandleOneToManyRelationship() {
    AutoUriHandler handler = new AutoUriHandler<>(mDatabase, mContentResolver, AUTO_URIS, CONTENT_TYPE_PROVIDER, TestModels.MODEL_GRAPH, mAutoNotificationUriSetter);
    ContentValues values = new ContentValues();
    values.put(Contact.ID, 13);
    assertThat(handler.insert(AUTO_URIS.model(Contact.class).id(1).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Deal.NAME, "Test");
    assertThat(handler.insert(AUTO_URIS.model(Contact.class).id(1).model(Deal.class).id(1).toUri(), values)).isNotNull();

    assertThat(handler.delete(AUTO_URIS.model(Contact.class).id(1).model(Deal.class).id(1).toUri(), null, null)).isEqualTo(1);
    assertThat(handler.delete(AUTO_URIS.model(Contact.class).id(1).toUri(), null, null)).isEqualTo(1);
  }

  @Test
  public void shouldHandleOneToOneRelationship() {
    AutoUriHandler handler = new AutoUriHandler<>(mDatabase, mContentResolver, AUTO_URIS, CONTENT_TYPE_PROVIDER, TestModels.MODEL_GRAPH, mAutoNotificationUriSetter);
    ContentValues values = new ContentValues();
    values.put(Lead.ID, 13);
    assertThat(handler.insert(AUTO_URIS.model(Lead.class).id(1).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(ContactData.ID, 133);
    assertThat(handler.insert(AUTO_URIS.model(Lead.class).id(1).model(ContactData.class).id(2).toUri(), values)).isNotNull();

    assertThat(handler.delete(AUTO_URIS.model(Lead.class).id(1).model(ContactData.class).id(2).toUri(), null, null)).isEqualTo(1);
    assertThat(handler.delete(AUTO_URIS.model(Lead.class).id(1).toUri(), null, null)).isEqualTo(1);
  }

  @Test
  public void shouldHandleRecursiveModelRelationship() {
    AutoUriHandler handler = new AutoUriHandler<>(mDatabase, mContentResolver, AUTO_URIS, CONTENT_TYPE_PROVIDER, TestModels.MODEL_GRAPH, mAutoNotificationUriSetter);
    ContentValues values = new ContentValues();
    values.put(Contact.ID, 13);
    assertThat(handler.insert(AUTO_URIS.model(Contact.class).id(1).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Contact.ID, 133);
    assertThat(handler.insert(AUTO_URIS.model(Contact.class).id(1).model(Contact.class).id(2).toUri(), values)).isNotNull();

    assertThat(handler.delete(AUTO_URIS.model(Contact.class).id(1).model(Contact.class).id(2).toUri(), null, null)).isEqualTo(1);
    assertThat(handler.delete(AUTO_URIS.model(Contact.class).id(1).toUri(), null, null)).isEqualTo(1);
  }

  @Test
  public void shouldHandlePolymorphicRelationship() {
    AutoUriHandler handler = new AutoUriHandler<>(mDatabase, mContentResolver, AUTO_URIS, CONTENT_TYPE_PROVIDER, TestModels.MODEL_GRAPH, mAutoNotificationUriSetter);
    ContentValues values = new ContentValues();
    values.put(Contact.ID, 13);
    assertThat(handler.insert(AUTO_URIS.model(Contact.class).id(1).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Lead.ID, 13);
    assertThat(handler.insert(AUTO_URIS.model(Lead.class).id(2).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Note.ID, 133);
    assertThat(handler.insert(AUTO_URIS.model(Contact.class).id(1).model(Note.class).id(3).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Note.ID, 134);
    assertThat(handler.insert(AUTO_URIS.model(Lead.class).id(2).model(Note.class).id(4).toUri(), values)).isNotNull();

    assertThat(handler.delete(AUTO_URIS.model(Contact.class).id(1).model(Note.class).id(3).toUri(), null, null)).isEqualTo(1);
    assertThat(handler.delete(AUTO_URIS.model(Contact.class).id(1).toUri(), null, null)).isEqualTo(1);
    assertThat(handler.delete(AUTO_URIS.model(Lead.class).id(2).model(Note.class).id(4).toUri(), null, null)).isEqualTo(1);
    assertThat(handler.delete(AUTO_URIS.model(Lead.class).id(2).toUri(), null, null)).isEqualTo(1);
  }
}
