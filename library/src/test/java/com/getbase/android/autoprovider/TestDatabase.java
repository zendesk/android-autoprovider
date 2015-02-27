package com.getbase.android.autoprovider;

import com.getbase.android.autoprovider.TestModels.Call;
import com.getbase.android.autoprovider.TestModels.Contact;
import com.getbase.android.autoprovider.TestModels.ContactData;
import com.getbase.android.autoprovider.TestModels.Deal;
import com.getbase.android.autoprovider.TestModels.DealContact;
import com.getbase.android.autoprovider.TestModels.Lead;
import com.getbase.android.autoprovider.TestModels.Note;
import com.getbase.android.autoprovider.TestModels.Tag;
import com.getbase.android.autoprovider.TestModels.Tagging;
import com.getbase.android.autoprovider.TestModels.User;
import com.getbase.android.schema.Schemas;
import com.getbase.android.schema.Schemas.AddColumn;
import com.getbase.android.schema.Schemas.Schema;
import com.getbase.android.schema.Schemas.TableDefinition;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TestDatabase extends SQLiteOpenHelper {
  public static final String TEST_DB_NAME = "autoprovider_test.db";
  private static final Schemas SCHEMAS = Schemas.Builder.currentSchema(0,
      new TableDefinition(TestModels.DEAL.getDbTable(),
          new AddColumn(Deal._ID, "INTEGER"),
          new AddColumn(Deal.ID, "INTEGER"),
          new AddColumn(Deal.CONTACT_ID, "INTEGER"),
          new AddColumn(Deal.USER_ID, "INTEGER"),
          new AddColumn(Deal.NAME, "TEXT")),
      new TableDefinition(TestModels.USER.getDbTable(),
          new AddColumn(User._ID, "INTEGER"),
          new AddColumn(User.ID, "INTEGER"),
          new AddColumn(User.EMAIL, "TEXT"),
          new AddColumn(User.IS_ADMIN, "INTEGER")),
      new TableDefinition(TestModels.CONTACT.getDbTable(),
          new AddColumn(Contact._ID, "INTEGER"),
          new AddColumn(Contact.ID, "INTEGER"),
          new AddColumn(Contact.CONTACT_ID, "INTEGER"),
          new AddColumn(Contact.USER_ID, "INTEGER")),
      new TableDefinition(TestModels.LEAD.getDbTable(),
          new AddColumn(Lead._ID, "INTEGER"),
          new AddColumn(Lead.ID, "INTEGER"),
          new AddColumn(Lead.USER_ID, "INTEGER"),
          new AddColumn(Lead.OWNER_ID, "INTEGER")),
      new TableDefinition(TestModels.CONTACT_DATA.getDbTable(),
          new AddColumn(ContactData._ID, "INTEGER"),
          new AddColumn(ContactData.ID, "INTEGER"),
          new AddColumn(ContactData.LEAD_ID, "INTEGER")),
      new TableDefinition(TestModels.DEAL_CONTACT.getDbTable(),
          new AddColumn(DealContact._ID, "INTEGER"),
          new AddColumn(DealContact.ID, "INTEGER"),
          new AddColumn(DealContact.DEAL_ID, "INTEGER"),
          new AddColumn(DealContact.CONTACT_ID, "INTEGER")),
      new TableDefinition(TestModels.NOTE.getDbTable(),
          new AddColumn(Note._ID, "INTEGER"),
          new AddColumn(Note.ID, "INTEGER"),
          new AddColumn(Note.NOTABLE_TYPE, "TEXT"),
          new AddColumn(Note.NOTABLE_ID, "INTEGER")),
      new TableDefinition(TestModels.CALL.getDbTable(),
          new AddColumn(Call._ID, "INTEGER"),
          new AddColumn(Call.ID, "INTEGER"),
          new AddColumn(Call.CALLABLE_TYPE, "TEXT"),
          new AddColumn(Call.CALLABLE_ID, "INTEGER")),
      new TableDefinition(TestModels.TAG.getDbTable(),
          new AddColumn(Tag._ID, "INTEGER"),
          new AddColumn(Tag.ID, "INTEGER"),
          new AddColumn(Tag.VALUE, "TEXT")),
      new TableDefinition(TestModels.TAGGING.getDbTable(),
          new AddColumn(Tagging._ID, "INTEGER"),
          new AddColumn(Tagging.ID, "INTEGER"),
          new AddColumn(Tagging.TAGGABLE_TYPE, "TEXT"),
          new AddColumn(Tagging.TAGGABLE_ID, "INTEGER"),
          new AddColumn(Tagging.TAG_ID, "INTEGER"),
          new AddColumn(Tagging.USER_ID, "INTEGER")))
      .build();

  private Context mContext;

  public TestDatabase(Context context) {
    super(context, TEST_DB_NAME, null, 1);
    mContext = context;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    Schema schema = SCHEMAS.getSchema(0);
    for (String table : schema.getTables()) {
      db.execSQL(schema.getCreateTableStatement(table));
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    SCHEMAS.upgrade(mContext, db, oldVersion, newVersion);
  }
}