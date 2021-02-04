AutoProvider
============
Utility for creating ContentProviders without boilerplate and with heavy customization options.

Features
========

Usage
=====
Just add repository and the dependency to your `build.gradle`:

```groovy
allprojects {
    repositories {
        // ...
        maven { url 'https://dl.bintray.com/basecrm/maven' }
    }
}

```groovy
dependencies {
    compile 'com.getbase.android.autoprovider:library:0.2.0'
}
```

## Publishing new version

1. Update `VERSION_NAME` in `gradle.properties` file
2. Merge PR to master
3. Create new GitHub release with tag name `v` followed by version - e.g. `v0.15.0`
4. GitHub Actions will automatically build and publish new package in Bintray repository

## Copyright and license

Copyright 2014 Zendesk

Licensed under the [Apache License, Version 2.0](LICENSE)
