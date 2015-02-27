package com.getbase.android.autoprovider;

import com.getbase.autoindexer.DbTableModel;
import com.getbase.forger.thneed.MicroOrmModel;
import com.google.common.collect.ImmutableList;

import org.chalup.microorm.annotations.Column;
import org.chalup.thneed.ModelGraph;
import org.chalup.thneed.PolymorphicType;

import java.util.Locale;

public final class TestModels {
  private TestModels() {
  }

  public static final String AUTHORITY = "com.getbase.android.autoprovider.testprovider";
  public static final String CONTENT_TYPE_PREFIX = "autoprovider";

  public static class BaseModel {
    public static final String _ID = "_id";
    public static final String ID = "id";

    @Column(value = _ID, readonly = true)
    public long _id;
    @Column(ID)
    public long id;
  }

  public static class Deal extends BaseModel {
    public static final String CONTACT_ID = "contact_id";
    public static final String USER_ID = "user_id";
    public static final String NAME = "name";

    @Column(CONTACT_ID)
    public long contactId;
    @Column(USER_ID)
    public long userId;
    @Column(NAME)
    public String name;
  }

  public static class User extends BaseModel {
    public static final String EMAIL = "email";
    public static final String IS_ADMIN = "is_admin";

    @Column(EMAIL)
    public String email;
    @Column(IS_ADMIN)
    public boolean admin;
  }

  public static class Contact extends BaseModel {
    public static final String USER_ID = "user_id";
    public static final String CONTACT_ID = "contact_id";

    @Column(USER_ID)
    public long userId;
    @Column(CONTACT_ID)
    public long contactId;
  }

  public static class Lead extends BaseModel {
    public static final String USER_ID = "user_id";
    public static final String OWNER_ID = "owner_id";

    @Column(USER_ID)
    public long userId;
    @Column(OWNER_ID)
    public long ownerId;
  }

  public static class ContactData extends BaseModel {
    public static final String LEAD_ID = "lead_id";

    @Column(LEAD_ID)
    public long leadId;
  }

  public static class DealContact extends BaseModel {
    public static final String CONTACT_ID = "contact_id";
    public static final String DEAL_ID = "deal_id";

    @Column(CONTACT_ID)
    public long contactId;
    @Column(DEAL_ID)
    public long dealId;
  }

  public static class Note extends BaseModel {
    public static final String NOTABLE_TYPE = "notable_type";
    public static final String NOTABLE_ID = "notable_id";

    @Column(NOTABLE_TYPE)
    public String notableType;
    @Column(NOTABLE_ID)
    public long notableId;
  }

  public static class Call extends BaseModel {
    public static final String CALLABLE_TYPE = "callable_type";
    public static final String CALLABLE_ID = "callable_id";

    @Column(CALLABLE_TYPE)
    public String callableType;
    @Column(CALLABLE_ID)
    public long callableId;
  }

  public static class Tag extends BaseModel {
    public static final String VALUE = "value";

    @Column(VALUE)
    public String value;
  }

  public static class Tagging extends BaseModel {
    public static final String TAGGABLE_TYPE = "taggable_type";
    public static final String TAGGABLE_ID = "taggable_id";
    public static final String TAG_ID = "tag_id";
    public static final String USER_ID = "user_id";

    @Column(TAGGABLE_TYPE)
    public String taggableType;
    @Column(TAGGABLE_ID)
    public long taggableId;
    @Column(TAG_ID)
    public long tagId;
    @Column(USER_ID)
    public long userId;
  }

  public interface TestModel extends DbTableModel, MicroOrmModel {
  }

  public static class BaseTestModel implements TestModel {
    private final Class<?> mKlass;

    public BaseTestModel(Class<?> klass) {
      mKlass = klass;
    }

    @Override
    public String getDbTable() {
      return mKlass.getSimpleName().toLowerCase(Locale.US);
    }

    @Override
    public Class<?> getModelClass() {
      return mKlass;
    }
  }

  public static class PolyModel extends BaseTestModel implements PolymorphicType<PolyModel> {
    private final String mModelName;

    public PolyModel(Class<?> klass, String modelName) {
      super(klass);
      mModelName = modelName;
    }

    @Override
    public PolyModel self() {
      return this;
    }

    @Override
    public String getModelName() {
      return mModelName;
    }
  }

  public static PolyModel CONTACT = new PolyModel(Contact.class, "Contact");
  public static PolyModel DEAL = new PolyModel(Deal.class, "Deal");
  public static TestModel USER = new BaseTestModel(User.class);
  public static PolyModel LEAD = new PolyModel(Lead.class, "Lead");
  public static TestModel CONTACT_DATA = new BaseTestModel(ContactData.class);
  public static TestModel DEAL_CONTACT = new BaseTestModel(DealContact.class);
  public static TestModel NOTE = new BaseTestModel(Note.class);
  public static TestModel CALL = new BaseTestModel(Call.class);
  public static TestModel TAG = new BaseTestModel(Tag.class);
  public static TestModel TAGGING = new BaseTestModel(Tagging.class);

  static ModelGraph<TestModel> MODEL_GRAPH = ModelGraph.of(TestModel.class)
      .identifiedByDefault().by(BaseModel.ID)
      .where()
      .the(DEAL).references(CONTACT).by(Deal.CONTACT_ID)
      .the(LEAD).mayHave(CONTACT_DATA).linked().by(ContactData.LEAD_ID)
      .the(LEAD).references(USER).by(Lead.USER_ID)
      .the(LEAD).references(USER).by(Lead.OWNER_ID)
      .the(DEAL_CONTACT).links(DEAL).by(DealContact.DEAL_ID).with(CONTACT).by(DealContact.CONTACT_ID)
      .the(NOTE).references(ImmutableList.of(CONTACT, DEAL, LEAD)).by(Note.NOTABLE_TYPE, Note.NOTABLE_ID)
      .the(CALL).references(ImmutableList.of(CONTACT, LEAD)).by(Call.CALLABLE_TYPE, Call.CALLABLE_ID)
      .the(CONTACT).groupsOther().by(Contact.CONTACT_ID)
      .the(TAGGING).links(TAG).by(Tagging.TAG_ID).with(ImmutableList.of(CONTACT, LEAD, DEAL)).by(Tagging.TAGGABLE_TYPE, Tagging.TAGGABLE_ID)
      .the(TAGGING).references(USER).by(Tagging.USER_ID)
      .the(DEAL).references(USER).by(Deal.USER_ID)
      .the(CONTACT).references(USER).by(Contact.USER_ID)
      .build();
}
