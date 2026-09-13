# Releasing

This SDK is distributed via [JitPack](https://jitpack.io/#Insert-Affiliate/InsertAffiliateAndroidSDK), which builds directly from GitHub tags.

## Steps

1. Merge your changes into `main`.
2. Bump the version in `app/build.gradle`:
   ```groovy
   publishing {
       publications {
           release(MavenPublication) {
               ...
               version = 'X.Y.Z'
           }
       }
   }
   ```
3. Commit the bump on its own:
   ```
   git commit -am "Bump version to X.Y.Z"
   ```
4. Tag the commit and push both the commit and the tag:
   ```
   git tag vX.Y.Z
   git push origin main
   git push origin vX.Y.Z
   ```

JitPack builds the artifact the first time a consumer's Gradle build requests that tag (`implementation 'com.github.Insert-Affiliate:InsertAffiliateAndroidSDK:vX.Y.Z'`) and caches it after that.

## Versioning

Use [semantic versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`) for new releases:

- **Patch** — bug fixes, no public API changes
- **Minor** — new functionality, backward compatible
- **Major** — breaking changes to the public API
