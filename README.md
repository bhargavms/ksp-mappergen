# MapperGen

A **Kotlin Symbol Processing (KSP)** code generator that automatically creates mapper functions to convert between data classes. A Kotlin-native alternative to MapStruct.

## Kotlin Multiplatform Ready

The annotation library is **Kotlin Multiplatform** - use it in any target:
- JVM / Android
- JavaScript (Browser, Node.js)
- Native (iOS, macOS, Linux, Windows)

The KSP processor runs at compile time on JVM, but the generated code works everywhere Kotlin does.

## Quick Start

```bash
# Run the sample
./gradlew :sample:run
```

Output:
```
=== MapperGen Demo ===

Input UserDto: UserDto(id=user-123, name=John Doe, email=john@example.com, age=30)
Mapped User:   User(id=user-123, name=John Doe, email=john@example.com, age=30)

Partial DTO (with nulls): UserDto(id=user-789, name=null, email=null, age=null)
Mapped with defaults:     User(id=user-789, name=, email=, age=0)
```

## How it works

1. Define your data classes:

```kotlin
// Network layer (nullable)
data class UserDto(val id: String?, val name: String?, val email: String?)

// Domain layer (non-nullable)
data class User(val id: String, val name: String, val email: String)
```

2. Annotate with `@Mapper`:

```kotlin
import com.bhargavms.mappergen.annotations.Mapper

interface Mappers {
    @Mapper
    fun map(dto: UserDto): User
}
```

3. Build - MapperGen generates:

```kotlin
fun mapUserDtoToUser(input: UserDto?): User? = input?.let {
    User(
        id = it.id ?: "",
        name = it.name ?: "",
        email = it.email ?: ""
    )
}
```

## Project Structure

```
mappergen/
├── gradle/libs.versions.toml      # Version catalog
├── build-logic/                   # Convention plugins
│   └── src/main/kotlin/
│       ├── mappergen.kotlin-library.gradle.kts      # JVM modules
│       └── mappergen.kotlin-multiplatform.gradle.kts # KMP modules
├── annotations/          # @Mapper annotation (Multiplatform)
├── codegen/                       # Code generation (JVM)
├── mappergen/                     # KSP processor (JVM)
├── sample/                        # Usage demo (JVM)
└── test/                          # Comprehensive tests (JVM)
```

### Module Targets

| Module | Target | Purpose |
|--------|--------|---------|
| `annotations` | **Multiplatform** | Annotation for consumers |
| `codegen` | JVM | Code generation logic |
| `mappergen` | JVM | KSP processor |
| `sample` | JVM | Demo application |
| `test` | JVM | Comprehensive tests |

## Usage

### 1. Add dependencies

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    // For KMP: use commonMain
    commonMainCompileOnly("com.bhargavms.mappergen:annotations:<version>")

    // For JVM-only:
    compileOnly("com.bhargavms.mappergen:annotations:<version>")
    ksp("com.bhargavms.mappergen:mappergen:<version>")
}
```

### 2. Configure JVM version (optional)

The library doesn't enforce a JVM version - **you decide** what JVM version to use:

```kotlin
kotlin {
    jvmToolchain(17)  // or 21, or 11 - your choice
}
```

The processor will compile with whatever JVM version your project uses.

### 3. Define mapper interface

```kotlin
import com.bhargavms.mappergen.annotations.Mapper

interface Mappers {
    @Mapper
    fun map(value: Network.UserDto): Domain.User
}
```

### 4. Build

```bash
./gradlew build
```

## Tech Stack

- **Kotlin 2.1.0** (Multiplatform)
- **Gradle 8.11.1** with Version Catalogs & Convention Plugins
- **KSP 2.1.0-1.0.29**
- **KotlinPoet 1.18.1**
- **JVM**: Consumer's choice (no version enforced)

## Supported Targets

The `annotations` module supports:
- `jvm`
- `js` (Browser, Node.js)
- `macosX64`, `macosArm64`
- `iosX64`, `iosArm64`, `iosSimulatorArm64`
- `linuxX64`
- `mingwX64`

## Building

```bash
# Build all
./gradlew build

# Run demo
./gradlew :sample:run

# Run tests
./gradlew :test:test

# View generated mappers
find sample/build/generated/ksp -name "*.kt" -exec cat {} \;
```

## Security Scans

- OWASP Dependency-Check is configured but currently skipped until we have an NVD API key; run `./gradlew :dependencyCheckAnalyze` once you set the `NVD_API_KEY` environment variable.
- Integration tests now include an error compilation suite that runs via GradleRunner so we can assert on the failure output without spawning external Gradle processes.

## Development Setup

## Development prerequisites

- `yamllint` (required so the `yaml-check` pre-commit hook can validate staged YAML/YML files before local commits). Install via `pip install yamllint` (optionally pin a stable release like `pip install yamllint==1.27.1`). The project's dev container/CI already provision this tool so using those environments keeps the hook behavior consistent.


### Pre-commit Hooks

This project uses [Lefthook](https://github.com/evilmartians/lefthook) for Git hooks to ensure code quality:

- **ktlint** - Kotlin linting and formatting
- **Trailing whitespace** - Removes trailing whitespace from files
- **YAML validation** - Validates YAML/TOML syntax
- **Conventional commits** - Enforces conventional commit message format

#### Installation

```bash
# macOS
brew install lefthook

# Or with Go
go install github.com/evilmartians/lefthook@latest

# Activate hooks in this repository
lefthook install

# ktlint CLI is downloaded automatically on first run (cached in .cache/ktlint/)
# No additional setup required!
```

#### What happens on commit

- **pre-commit**: Auto-formats Kotlin files with ktlint, checks trailing whitespace, and validates YAML syntax
- **commit-msg**: Validates commit message follows conventional commits format

**ktlint auto-formatting**: The pre-commit hook uses a fast, pinned ktlint CLI (`./scripts/ktlintw`) that:
- Downloads ktlint once and caches it locally (no external dependencies after first run)
- Auto-formats only the staged Kotlin files (fast!)
- Re-stages any formatting changes automatically

Example valid commit messages:
- `feat(codegen): add new mapper feature`
- `fix(processor): handle null values correctly`
- `docs(readme): update installation instructions`

#### Skipping hooks (emergency only)

```bash
# Skip all hooks for this commit
git commit --no-verify -m "fix: emergency commit"
```

### Code Style

The project uses:
- **EditorConfig** for consistent formatting across IDEs
- **ktlint** for Kotlin code style enforcement
- **Gradle** for dependency and build management

## CI Pipelines

This project uses Gradle as the CI system. CI platforms can invoke Gradle pipeline tasks directly.

### Pipeline Overview

**Main Pipelines:**
- `pipeline` - Complete CI pipeline (JVM-only)
- `pipelineRelease` - Release pipeline (full checks + publish)

**Pipeline Stages:**
- `pipelineBuild` - Build stage (JVM modules)
- `pipelineBuildFull` - Build stage (all platforms, requires Node.js)
- `pipelineTest` - Test stage
- `pipelineQuality` - Quality checks (linting, security)
- `pipelineIntegration` - Integration testing (sample project)
- `pipelineReport` - Generate test reports
- `pipelineClean` - Clean build outputs

### Usage Examples

```bash
# Full CI pipeline (recommended for most CI setups)
./gradlew pipeline

# Individual stages (for custom workflows)
./gradlew pipelineBuild pipelineTest

# Release pipeline (when publishing)
./gradlew pipelineRelease

# Clean build + pipeline
./gradlew pipelineClean pipeline
```

### CI Platform Integration

**GitHub Actions:**
```yaml
- name: Run CI Pipeline
  run: ./gradlew pipeline
```

**GitLab CI:**
```yaml
test:
  script:
    - ./gradlew pipeline
```

**Jenkins:**
```groovy
sh './gradlew pipeline'
```

### Build Cache

Uses local Gradle build cache (free, no external costs):

```bash
# Local cache is automatically used
./gradlew ciPipeline
```

**Note:** Remote cache support was removed to keep CI costs at $0. Local caching provides significant performance benefits without any cost.

### Pipeline Stages Explained

| Stage | Purpose | Duration | Dependencies |
|-------|---------|----------|--------------|
| **Build** | Compile all modules | ~10-30s | None |
| **Test** | Run unit tests | ~5-15s | Build |
| **Quality** | Code analysis | ~5-10s | Build |
| **Integration** | End-to-end testing | ~5-10s | Build |
| **Publish** | Release artifacts | ~10-30s | All stages |

### CI Environment Setup

The pipelines automatically detect CI environments via the `CI=true` environment variable:

```bash
# Run in CI mode (disables daemon, optimizes for CI)
CI=true ./gradlew ciPipeline
```

### Test Reports

Test reports are generated automatically:
- **HTML Reports**: `build/reports/tests/test/index.html`
- **JUnit XML**: `build/test-results/test/*.xml` (for CI integration)

### Troubleshooting

**KMP Build Issues:**
- Use `ciPipeline` (JVM-only) instead of `ciPipelineFull`
- Add Node.js to your CI environment for full platform testing

**Slow Builds:**
- Enable remote build cache
- Use `org.gradle.caching=true` (already enabled)

**Out of Memory:**
- CI environments get optimized JVM args via `gradle.properties`

## Testing

The `test` module provides comprehensive JUnit tests covering all mapper functionality:

- ✅ Basic object mapping (User, Product)
- ✅ All primitive types with null defaults
- ✅ Nested object mapping (Person → Address)
- ✅ Collection mapping (simple types, empty, null defaults)
- ✅ Collection of complex types (Company → List<Person>)
- ✅ Enum mapping via `valueOf()` (StatusDto → Status)
- ✅ Null input and partial null handling

```bash
# Run specific test class
./gradlew :test:test --tests "*ComplexStructuresTest*"

# Run all tests with coverage
./gradlew :test:test
```

## Features

- ✅ Kotlin Multiplatform annotations
- ✅ Flexible JVM version (consumer decides)
- ✅ Simple property mapping (same names)
- ✅ Nullable to non-nullable with defaults
- ✅ Primitive type defaults
- ✅ Data class constructor generation

## Limitations

- ⚠️ Properties must have exact name match (case-insensitive)
- ⚠️ No custom transformations yet
- ⚠️ Nested object mapping needs work
- ⚠️ Collection mapping is basic

## License

[Apache License 2.0](LICENSE.txt).
