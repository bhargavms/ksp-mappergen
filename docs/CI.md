# CI/CD Pipeline Guide

This guide explains how to use the Gradle-based CI/CD pipelines for the MapperGen library.

## Pipeline Architecture

### Main Pipelines

| Pipeline | Purpose | Use Case |
|----------|---------|----------|
| `ciPipeline` | Complete CI pipeline (JVM-only) | Standard CI runs |
| `ciPipelineFull` | Complete CI pipeline (all platforms) | Full platform testing |
| `ciPipelineRelease` | Release pipeline | Publishing releases |

### Pipeline Stages

```
ciPipeline
├── ciPipelineBuild      (Compile all modules)
├── ciPipelineTest       (Run unit tests)
├── ciPipelineQuality    (Code quality checks)
└── ciPipelineIntegration (End-to-end testing)
```

## Usage

### Basic CI Run

```bash
# Run the complete CI pipeline
./gradlew pipeline
```

### Individual Stages

```bash
# Run only specific stages
./gradlew pipelineBuild
./gradlew pipelineTest
./gradlew pipelineQuality
./gradlew pipelineIntegration
./gradlew pipelineReport
```

### Release Pipeline

```bash
# Full release pipeline (includes publishing)
./gradlew pipelineRelease
```

### Clean and Build

```bash
# Clean and run full pipeline
./gradlew pipelineClean pipeline
```

## CI Platform Integration

### GitHub Actions

```yaml
name: CI
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew pipeline
```

### GitLab CI

```yaml
stages:
  - test

test:
  stage: test
  script:
    - ./gradlew pipeline
  artifacts:
    reports:
      junit: build/test-results/test/TEST-*.xml
    paths:
      - build/reports/tests/
    expire_in: 1 week
```

### Jenkins

```groovy
pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                sh './gradlew pipeline'
            }
            post {
                always {
                    junit 'build/test-results/test/TEST-*.xml'
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/tests/test',
                        reportFiles: 'index.html',
                        reportName: 'Test Report'
                    ])
                }
            }
        }
    }
}
```

### AWS CodeBuild

```yaml
version: 0.2
phases:
  build:
    commands:
      - ./gradlew pipeline
artifacts:
  files:
    - build/reports/tests/**/*
    - build/test-results/**/*
```

## Build Cache Configuration

### Local Cache (Free)

The pipeline uses Gradle's local build cache automatically:

```bash
# Local cache is enabled by default (gradle.properties)
./gradlew ciPipeline
```

**Benefits:**
- ✅ Zero cost
- ✅ Faster incremental builds
- ✅ Works offline
- ✅ No external dependencies

**Cache Locations:**
- Gradle dependencies: `~/.gradle/caches/`
- Build outputs: `~/.gradle/build-cache/`
- Configuration cache: `~/.gradle/configuration-cache/`

## CI Environment Optimization

### Automatic CI Detection

The build automatically detects CI environments:

```bash
# Explicit CI mode
CI=true ./gradlew pipeline

# Detected automatically on:
# - GitHub Actions (GITHUB_ACTIONS=true)
# - GitLab CI (GITLAB_CI=true)
# - Jenkins (JENKINS_HOME set)
# - Travis CI (TRAVIS=true)
```

### CI Optimizations Applied

- **Daemon Disabled**: Faster startup, predictable builds
- **Parallel Execution**: Multiple modules build in parallel
- **Configuration Cache**: Reuse configuration across runs
- **Optimized JVM Args**: Better GC settings for CI

## Test Reports and Artifacts

### Generated Reports

- **HTML Reports**: `build/reports/tests/test/index.html`
- **JUnit XML**: `build/test-results/test/TEST-*.xml`
- **Coverage Reports**: `build/reports/jacoco/` (if enabled)

### Sample Output

- **Generated Code**: `sample/build/generated/ksp/main/kotlin/`
- **Demo Output**: Captured from `ciPipelineIntegration`

## Troubleshooting

### Common Issues

**KMP Build Fails:**
```
# Use JVM-only pipeline
./gradlew pipeline  # instead of pipelineBuildFull
```

**Slow Builds:**
```bash
# Enable remote cache
GRADLE_REMOTE_CACHE_URL=https://cache.example.com ./gradlew pipeline

# Or check local cache
./gradlew pipelineClean pipeline
```

**Out of Memory:**
```bash
# Increase memory (already optimized in gradle.properties)
ORG_GRADLE_JVMARGS="-Xmx4g" ./gradlew pipeline
```

### Debugging

```bash
# Verbose output
./gradlew pipeline --info

# Debug mode
./gradlew pipeline --debug

# Stack trace on failure
./gradlew pipeline --stacktrace
```

## Pipeline Customization

### Adding Custom Quality Checks

```kotlin
// In build.gradle.kts
tasks.register("customQualityCheck") {
    doLast {
        // Your quality checks here
    }
}

tasks.named("ciPipelineQuality") {
    dependsOn("customQualityCheck")
}
```

### Adding Custom Integration Tests

```kotlin
// In build.gradle.kts
tasks.register("customIntegrationTest") {
    dependsOn(":myModule:integrationTest")
}

tasks.named("ciPipelineIntegration") {
    dependsOn("customIntegrationTest")
}
```

## Performance Tips

1. **Local Cache**: Gradle automatically caches build outputs locally (free)
2. **CI Platform Cache**: Use your CI platform's dependency caching (GitHub Actions cache, etc.)
3. **Parallel Execution**: Already enabled, scales with CPU cores
4. **Incremental Builds**: Only rebuild what changed
5. **Shallow Clones**: For Git-based CI platforms
6. **Configuration Cache**: Reuse configuration across builds

## Security Considerations

1. **Dependency Verification**: Consider enabling dependency verification
2. **Artifact Signing**: Enable GPG signing for releases
3. **Vulnerability Scanning**: Integrate OWASP dependency checks
4. **Code Signing**: Sign commits and releases

## Monitoring and Metrics

### Build Metrics

```bash
# Show build timing
./gradlew pipeline --profile

# Generate build scan
./gradlew pipeline --scan
```

### Test Metrics

- Test execution time
- Test failure rates
- Code coverage trends
- Build success rates
