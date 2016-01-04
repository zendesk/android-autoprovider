package com.getbase.android.autoprovider;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import java.util.Iterator;

public class UtilsTest {
  @Test
  public void shouldAdvanceIteratorPastGivenElement() throws Exception {
    Iterator<Integer> advancedIterator = Utils.advancePast(ImmutableList.of(1, 2, 3, 4).iterator(), 2);
    ImmutableList<Integer> advancedList = ImmutableList.copyOf(advancedIterator);
    assertThat(advancedList).containsExactly(3, 4).inOrder();
  }

  @Test(expected = NullPointerException.class)
  public void shouldFailIfIteratorIsNull() throws Exception {
    Utils.advancePast(null, 1);
  }

  @Test(expected = NullPointerException.class)
  public void shouldFailIfGivenElementIsNull() throws Exception {
    Utils.advancePast(ImmutableList.of(1, 2, 3).iterator(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailIfIteratorDoesNotContainGivenElement() throws Exception {
    Utils.advancePast(ImmutableList.of(1, 2, 3).iterator(), 4);
  }
}
