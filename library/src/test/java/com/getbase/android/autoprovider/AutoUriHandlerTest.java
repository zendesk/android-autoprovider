package com.getbase.android.autoprovider;

import static com.google.common.truth.Truth.assertThat;

import com.getbase.android.autoprovider.TestModels.BaseModel;
import com.getbase.android.autoprovider.TestModels.Contact;
import com.getbase.android.autoprovider.TestModels.Deal;
import com.getbase.android.autoprovider.TestModels.Lead;
import com.getbase.android.autoprovider.TestModels.TestModel;
import com.getbase.android.autoprovider.TestModels.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

@Config(sdk = 18, manifest = "./src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class AutoUriHandlerTest {
  ContentResolver mContentResolver;
  SQLiteOpenHelper mDatabase;

  private static final AutoUris<TestModel> AUTO_URIS = AutoUris
      .from(TestModels.MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .defaultIdColumn(BaseModel._ID)
      .build();

  private static final ContentTypeProvider<TestModel> CONTENT_TYPE_PROVIDER = new ContentTypeProvider<>(
      TestModels.CONTENT_TYPE_PREFIX,
      new ClassToTable<>(TestModels.MODEL_GRAPH)
  );

  private AutoNotificationUriSetter<TestModel> mAutoNotificationUriSetter;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    TestProvider provider = ProviderMock.provide();
    mContentResolver = RuntimeEnvironment.application.getContentResolver();
    mDatabase = provider.getDatabase();
    mAutoNotificationUriSetter = new AutoNotificationUriSetter<>(
        mDatabase,
        mContentResolver,
        AUTO_URIS,
        new ClassToTable<>(TestModels.MODEL_GRAPH)
    );
  }

  @Test
  public void shouldInitializeProperly() {
    new AutoUriHandler<>(mDatabase, mContentResolver, AUTO_URIS, CONTENT_TYPE_PROVIDER, TestModels.MODEL_GRAPH, mAutoNotificationUriSetter);
  }

  @Test
  public void simpleCrudOperationsShouldWork() {
    AutoUriHandler<TestModel> handler = new AutoUriHandler<>(mDatabase, mContentResolver, AUTO_URIS, CONTENT_TYPE_PROVIDER, TestModels.MODEL_GRAPH, mAutoNotificationUriSetter);
    ContentValues values = new ContentValues();
    values.put(Deal.CONTACT_ID, 0);
    values.put(Deal.USER_ID, 0);
    values.put(Deal.NAME, "Test");
    assertThat(handler.insert(AUTO_URIS.model(Deal.class).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Deal.CONTACT_ID, 1);
    values.put(Deal.USER_ID, 0);
    values.put(Deal.NAME, "Test");
    assertThat(handler.update(AUTO_URIS.model(Deal.class).toUri(), values, null, null)).isGreaterThan(0);

    Cursor c = handler.query(AUTO_URIS.model(Deal.class).toUri(), new String[] { Deal.CONTACT_ID }, Deal.CONTACT_ID + " = ?", new String[] { "1" }, Deal.CONTACT_ID);
    assertThat(c).isNotNull();
    assertThat(c.getCount()).isGreaterThan(0);
    assertThat(c.moveToFirst()).isTrue();
    assertThat(c.getLong(c.getColumnIndex(Deal.CONTACT_ID))).isEqualTo(1);
    c.close();

    assertThat(handler.delete(AUTO_URIS.model(Deal.class).toUri(), null, null)).isGreaterThan(0);
  }

  @Test
  public void relativeCrudOperationsShouldWork() {
    AutoUriHandler<TestModel> handler = new AutoUriHandler<>(mDatabase, mContentResolver, AUTO_URIS, CONTENT_TYPE_PROVIDER, TestModels.MODEL_GRAPH, mAutoNotificationUriSetter);
    ContentValues values = new ContentValues();
    values.put(Contact.ID, 666);
    values.put(Contact.USER_ID, 0);
    assertThat(handler.insert(AUTO_URIS.model(Contact.class).id(1).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Deal.USER_ID, 0);
    values.put(Deal.NAME, "Test");
    assertThat(handler.insert(AUTO_URIS.model(Contact.class).id(1).model(Deal.class).id(1).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Deal.NAME, "New Test Name");
    assertThat(handler.update(AUTO_URIS.model(Contact.class).id(1).model(Deal.class).toUri(), values, null, null)).isGreaterThan(0);

    Cursor c = handler.query(AUTO_URIS.model(Contact.class).id(1).model(Deal.class).toUri(), new String[] { Deal.NAME }, null, null, Deal.CONTACT_ID);
    assertThat(c).isNotNull();
    assertThat(c.getCount()).isGreaterThan(0);
    assertThat(c.moveToFirst()).isTrue();
    assertThat(c.getString(c.getColumnIndex(Deal.NAME))).isEqualTo("New Test Name");
    c.close();

    assertThat(handler.delete(AUTO_URIS.model(Contact.class).id(1).model(Deal.class).toUri(), null, null)).isGreaterThan(0);

    assertThat(handler.delete(AUTO_URIS.model(Contact.class).toUri(), null, null)).isGreaterThan(0);
  }

  @Test
  public void multipleRelationsInCrudOperationsShouldWork() {
    AutoUriHandler<TestModel> handler = new AutoUriHandler<>(mDatabase, mContentResolver, AUTO_URIS, CONTENT_TYPE_PROVIDER, TestModels.MODEL_GRAPH, mAutoNotificationUriSetter);
    ContentValues values = new ContentValues();
    values.put(User.ID, 123);
    values.put(User.EMAIL, "test@email.com");
    values.put(User.IS_ADMIN, 1);
    assertThat(handler.insert(AUTO_URIS.model(User.class).id(1).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Contact.ID, 13);
    assertThat(handler.insert(AUTO_URIS.model(User.class).id(1).model(Contact.class).id(1).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Deal.NAME, "Super Dil");
    values.put(Deal.ID, 18);
    assertThat(handler.insert(AUTO_URIS.model(Deal.class)
            .relatedTo(AUTO_URIS.model(User.class).id(1))
            .relatedTo(AUTO_URIS.model(Contact.class).id(1)).toUri(),
        values)).isNotNull();

    values = new ContentValues();
    values.put(Deal.NAME, "New Test Name");
    assertThat(handler.update(AUTO_URIS.model(Deal.class)
        .relatedTo(AUTO_URIS.model(User.class).id(1))
        .relatedTo(AUTO_URIS.model(Contact.class).id(1)).toUri(), values, null, null)).isGreaterThan(0);

    Cursor c = handler.query(AUTO_URIS.model(Deal.class)
        .relatedTo(AUTO_URIS.model(User.class).id(1))
        .relatedTo(AUTO_URIS.model(Contact.class).id(1)).toUri(), new String[] { Deal.USER_ID, Deal.CONTACT_ID, Deal.NAME }, null, null, null);
    assertThat(c).isNotNull();
    assertThat(c.getCount()).isGreaterThan(0);
    assertThat(c.moveToFirst()).isTrue();
    assertThat(c.getLong(c.getColumnIndex(Deal.USER_ID))).isEqualTo(123);
    assertThat(c.getLong(c.getColumnIndex(Deal.CONTACT_ID))).isEqualTo(13);
    assertThat(c.getString(c.getColumnIndex(Deal.NAME))).isEqualTo("New Test Name");
    c.close();

    assertThat(handler.delete(AUTO_URIS.model(User.class).toUri(), null, null)).isGreaterThan(0);
    assertThat(handler.delete(AUTO_URIS.model(Contact.class).toUri(), null, null)).isGreaterThan(0);
    assertThat(handler.delete(AUTO_URIS.model(Deal.class).toUri(), null, null)).isGreaterThan(0);
  }

  @Test
  public void ambiguousColumnsInCrudOperationsShouldWork() {
    AutoUriHandler<TestModel> handler = new AutoUriHandler<>(mDatabase, mContentResolver, AUTO_URIS, CONTENT_TYPE_PROVIDER, TestModels.MODEL_GRAPH, mAutoNotificationUriSetter);
    ContentValues values = new ContentValues();
    values.put(User.ID, 1);
    values.put(User.EMAIL, "test1@email.com");
    values.put(User.IS_ADMIN, 1);
    assertThat(handler.insert(AUTO_URIS.model(User.class).id(1).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(User.ID, 2);
    values.put(User.EMAIL, "test2@email.com");
    values.put(User.IS_ADMIN, 1);
    assertThat(handler.insert(AUTO_URIS.model(User.class).id(2).toUri(), values)).isNotNull();

    values = new ContentValues();
    values.put(Lead.ID, 18);
    assertThat(handler.insert(AUTO_URIS.model(Lead.class)
            .relatedTo(Lead.USER_ID, AUTO_URIS.model(User.class).id(1))
            .relatedTo(Lead.OWNER_ID, AUTO_URIS.model(User.class).id(2)).toUri(),
        values
    )).isNotNull();

    values = new ContentValues();
    values.put(Lead.ID, 19);
    assertThat(handler.update(AUTO_URIS.model(Lead.class)
            .relatedTo(Lead.USER_ID, AUTO_URIS.model(User.class).id(1))
            .relatedTo(Lead.OWNER_ID, AUTO_URIS.model(User.class).id(2)).toUri(),
        values, null, null
    )).isNotNull();

    Cursor c = handler.query(AUTO_URIS.model(Lead.class)
        .relatedTo(Lead.USER_ID, AUTO_URIS.model(User.class).id(1))
        .relatedTo(Lead.OWNER_ID, AUTO_URIS.model(User.class).id(2)).toUri(), new String[] { Lead.ID }, null, null, null);
    assertThat(c).isNotNull();
    assertThat(c.getCount()).isGreaterThan(0);
    assertThat(c.moveToFirst()).isTrue();
    assertThat(c.getLong(c.getColumnIndex(Lead.ID))).isEqualTo(19);
    c.close();

    assertThat(handler.delete(AUTO_URIS.model(User.class).toUri(), null, null)).isGreaterThan(0);
  }
}
