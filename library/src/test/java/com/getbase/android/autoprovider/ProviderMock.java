package com.getbase.android.autoprovider;

import org.robolectric.shadows.ShadowContentResolver;

public class ProviderMock {

  public static TestProvider provide() {
    final TestProvider provider = new TestProvider();
    provider.onCreate();
    ShadowContentResolver.registerProvider(TestModels.AUTHORITY, provider);
    return provider;
  }
}