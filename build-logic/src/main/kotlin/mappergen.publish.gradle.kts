/**
 * Maven publication for library artifacts that consumers resolve by coordinate.
 *
 * Coordinates and POM fields come from gradle.properties (`GROUP`, `VERSION_NAME`,
 * `POM_*`, `SONATYPE_HOST`). Signing and Portal tokens are GitHub Actions
 * secrets, injected only by `.github/workflows/publish.yml`.
 */
plugins {
    id("com.vanniktech.maven.publish")
}

group = findProperty("GROUP") ?: group
version = findProperty("VERSION_NAME") ?: version
