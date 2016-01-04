package com.getbase.android.autoprovider;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import com.getbase.android.autoprovider.ContentUriHandler.ContentUriAction;
import com.getbase.android.autoprovider.TestModels.TestModel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DelegatingContentUriHandlerTest {

  private static final AutoUris<TestModel> AUTO_URIS = AutoUris
      .from(TestModels.MODEL_GRAPH)
      .forContentProvider(TestModels.AUTHORITY)
      .build();

  private static final Uri TEST_URI = AUTO_URIS.path("custom").toUri();

  private ContentUriHandler mockHandler() {
    ContentUriHandler handler = mock(ContentUriHandler.class);
    when(handler.canHandle(eq(TEST_URI), any(ContentUriAction.class))).thenReturn(true);
    return handler;
  }

  private ContentUriHandler mockHandler(ContentUriAction action) {
    ContentUriHandler handler = mock(ContentUriHandler.class);
    when(handler.canHandle(eq(TEST_URI), eq(action))).thenReturn(true);
    return handler;
  }

  @Test
  public void shouldLookForTheFirstHandlerThatCanHandleRequestedAction() throws Exception {
    DelegatingContentUriHandler testSubject = new DelegatingContentUriHandler();

    assertThat(testSubject.canHandle(TEST_URI, ContentUriAction.GET_TYPE)).isFalse();

    ContentUriHandler firstHandler = mockHandler();
    ContentUriHandler secondHandler = mockHandler();

    testSubject.addHandlers(firstHandler, secondHandler);
    assertThat(testSubject.canHandle(TEST_URI, ContentUriAction.GET_TYPE)).isTrue();
    verifyZeroInteractions(secondHandler);
  }

  @Test
  public void shouldDelegateTheActionToFirstHandlerThatCanHandleIt() throws Exception {
    DelegatingContentUriHandler testSubject = new DelegatingContentUriHandler();

    HashMap<ContentUriAction, ContentUriHandler> expectedHandlerForAction = new HashMap<>();
    Set<ContentUriHandler> shadowedHandlers = new HashSet<>();

    for (ContentUriAction action : ContentUriAction.values()) {
      ContentUriHandler firstHandler = mockHandler(action);
      ContentUriHandler secondHandler = mockHandler(action);

      testSubject.addHandlers(firstHandler, secondHandler);

      expectedHandlerForAction.put(action, firstHandler);
      shadowedHandlers.add(secondHandler);
    }

    testSubject.getType(TEST_URI);
    verify(expectedHandlerForAction.get(ContentUriAction.GET_TYPE)).getType(eq(TEST_URI));
    for (ContentUriHandler handler : shadowedHandlers) {
      verify(handler, never()).getType(any(Uri.class));
    }

    testSubject.query(TEST_URI, null, null, null, null);
    verify(expectedHandlerForAction.get(ContentUriAction.QUERY)).query(eq(TEST_URI), isNull(String[].class), isNull(String.class), isNull(String[].class), isNull(String.class));
    for (ContentUriHandler handler : shadowedHandlers) {
      verify(handler, never()).query(any(Uri.class), any(String[].class), any(String.class), any(String[].class), any(String.class));
    }

    testSubject.insert(TEST_URI, new ContentValues());
    verify(expectedHandlerForAction.get(ContentUriAction.INSERT)).insert(eq(TEST_URI), any(ContentValues.class));
    for (ContentUriHandler handler : shadowedHandlers) {
      verify(handler, never()).insert(any(Uri.class), any(ContentValues.class));
    }

    testSubject.delete(TEST_URI, null, null);
    verify(expectedHandlerForAction.get(ContentUriAction.DELETE)).delete(eq(TEST_URI), isNull(String.class), isNull(String[].class));
    for (ContentUriHandler handler : shadowedHandlers) {
      verify(handler, never()).delete(any(Uri.class), any(String.class), any(String[].class));
    }

    testSubject.update(TEST_URI, new ContentValues(), null, null);
    verify(expectedHandlerForAction.get(ContentUriAction.UPDATE)).update(eq(TEST_URI), any(ContentValues.class), isNull(String.class), isNull(String[].class));
    for (ContentUriHandler handler : shadowedHandlers) {
      verify(handler, never()).update(any(Uri.class), any(ContentValues.class), any(String.class), any(String[].class));
    }
  }

  @Test
  public void shouldAllowSkippingHandlers() throws Exception {
    final DelegatingContentUriHandler testSubject = new DelegatingContentUriHandler();

    ContentUriHandler loggingHandler = spy(
        new FailingUriHandler() {
          @Override
          public boolean canHandle(Uri uri, ContentUriAction action) {
            return action == ContentUriAction.QUERY;
          }

          @Override
          public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            ContentUriHandler handler = testSubject.getDelegateHandler(uri, ContentUriAction.QUERY, this);
            return handler.query(uri, projection, selection, selectionArgs, sortOrder);
          }
        }
    );

    ContentUriHandler underlyingHandler = mockHandler();
    testSubject.addHandlers(loggingHandler, underlyingHandler);

    testSubject.query(TEST_URI, null, null, null, null);

    verify(loggingHandler).query(eq(TEST_URI), isNull(String[].class), isNull(String.class), isNull(String[].class), isNull(String.class));
    verify(underlyingHandler).query(eq(TEST_URI), isNull(String[].class), isNull(String.class), isNull(String[].class), isNull(String.class));
  }
}