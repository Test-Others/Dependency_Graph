# CycloneDX SBOM Configuration Guide for KMP Projects

This guide explains how to configure and use the CycloneDX Gradle plugin for dependency analysis in Kotlin Multiplatform projects.

## Overview

CycloneDX generates Software Bill of Materials (SBOM) documents containing all direct and transitive dependencies of your project. This is useful for security audits, license compliance, and project documentation.

## Prerequisites

- Gradle 8.4 or higher
- Kotlin Multiplatform project structure
- Java 17 or 21 (required for compatibility with Kotlin 2.3.0)

### Java Version Configuration

If you encounter Java version compatibility issues, configure Java 21 in your `gradle.properties`:

```properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
```

On macOS, check installed Java versions:
```bash
ls /Library/Java/JavaVirtualMachines/
```

On Windows, check:
```
C:\Program Files\Java\
```

On Linux, check:
```bash
ls /usr/lib/jvm/
```

## Installation

### 1. Add Plugin Version to Version Catalog

In `gradle/libs.versions.toml`, add the CycloneDX version:

```toml
[versions]
cyclonedx = "3.1.0"

[plugins]
cyclonedxBom = { id = "org.cyclonedx.bom", version.ref = "cyclonedx" }
```

### 2. Apply Plugin to Root Project

In root `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.cyclonedxBom)
    // ... other plugins
}
```

### 3. Apply Plugin to Module Projects

In `composeApp/build.gradle.kts` (or other modules):

```kotlin
plugins {
    alias(libs.plugins.cyclonedxBom)
    // ... other plugins
}
```

## Configuration

### Basic Configuration for KMP

Add to your module's `build.gradle.kts`:

```kotlin
import org.cyclonedx.model.Component

tasks.cyclonedxDirectBom {
    // Project metadata
    projectType.set(Component.Type.APPLICATION) // or Component.Type.LIBRARY
    componentName.set("your-app-name")
    componentGroup.set("com.example.yourapp")
    
    // Platform configurations
    includeConfigs.set(listOf(
        "commonMainImplementation",
        "androidReleaseRuntimeClasspath",
        "androidReleaseCompileClasspath",
        "iosArm64ApiElements",
        "iosSimulatorArm64ApiElements"
    ))
    
    // Exclude test and debug configurations
    skipConfigs.set(listOf(
        ".*[Tt]est.*",
        ".*[Dd]ebug.*"
    ))
    
    // Output options
    includeBomSerialNumber.set(true)
    includeLicenseText.set(false)
    includeMetadataResolution.set(true)
}
```

### Aggregated SBOM Configuration

For multi-module projects, configure in root project:

```kotlin
tasks.cyclonedxBom {
    projectType.set(Component.Type.APPLICATION)
    componentName.set("your-app-aggregate")
    includeBomSerialNumber.set(true)
}
```

## Usage

### Generate Per-Module SBOM

Generate SBOM for individual modules:

```bash
./gradlew cyclonedxDirectBom
```

Output location:
- `composeApp/build/reports/cyclonedx-direct/bom.json`
- `composeApp/build/reports/cyclonedx-direct/bom.xml`

### Generate Aggregated SBOM

Generate consolidated SBOM for entire project:

```bash
./gradlew cyclonedxBom
```

Output location:
- `build/reports/cyclonedx/bom.json`
- `build/reports/cyclonedx/bom.xml`

### Generate Specific Module

```bash
./gradlew :composeApp:cyclonedxDirectBom
```

### Verbose Output

```bash
./gradlew cyclonedxBom --info
```

## Output Formats

The plugin generates two formats simultaneously:

- **JSON**: Machine-readable, ideal for automated tools
- **XML**: Alternative format for tools requiring XML

To disable one format:

```kotlin
tasks.cyclonedxDirectBom {
    xmlOutput.unsetConvention()  // Disable XML
    // or
    jsonOutput.unsetConvention() // Disable JSON
}
```

## Platform-Specific Configurations

### Android Only

```kotlin
includeConfigs.set(listOf(
    "androidReleaseRuntimeClasspath",
    "androidReleaseCompileClasspath"
))
```

### iOS Only

```kotlin
includeConfigs.set(listOf(
    "iosArm64ApiElements",
    "iosSimulatorArm64ApiElements"
))
```

### Common (Shared Code) Only

```kotlin
includeConfigs.set(listOf(
    "commonMainImplementation",
    "commonMainApi"
))
```

## Analyzing Generated Reports

### Viewing SBOM Files

1. **JSON viewers**:
   - Any text editor with JSON formatting
   - Online: [jsonviewer.stack.hu](https://jsonviewer.stack.hu/)
   - VS Code with JSON extensions

2. **SBOM analysis tools**:
   - [OWASP Dependency-Track](https://dependencytrack.org/) - Upload SBOM for vulnerability analysis
   - [CycloneDX Tool Center](https://cyclonedx.org/tool-center/) - List of compatible tools
   - GitHub Advanced Security (if enabled)

### Key SBOM Sections

Generated SBOM contains:

- **metadata**: Project information, build environment
- **components**: List of dependencies with versions
- **dependencies**: Dependency tree structure
- **licenses**: License information per component
- **externalReferences**: Links to repositories, documentation

## Advanced Configuration

### Include License Text

```kotlin
includeLicenseText.set(true)
```

### Custom Output Locations

```kotlin
tasks.cyclonedxDirectBom {
    jsonOutput.set(file("custom/path/sbom.json"))
    xmlOutput.set(file("custom/path/sbom.xml"))
}
```

### CI/CD Integration

```kotlin
tasks.cyclonedxDirectBom {
    // Dynamic versioning
    componentVersion.set(System.getenv("BUILD_VERSION") ?: project.version.toString())
    
    // Build system URL
    includeBuildSystem.set(true)
    buildSystemEnvironmentVariable.set("\${BUILD_URL}")
}
```

### Organizational Metadata

```kotlin
import org.cyclonedx.model.*

tasks.cyclonedxDirectBom {
    organizationalEntity.set(OrganizationalEntity().apply {
        name = "Your Organization"
        urls = listOf("https://yourcompany.com")
        addContact(OrganizationalContact().apply {
            name = "Security Team"
            email = "security@yourcompany.com"
        })
    })
}
```

## Troubleshooting

### Configuration Not Found

If configurations like `androidReleaseRuntimeClasspath` are not found:

1. Check available configurations:
   ```bash
   ./gradlew :composeApp:dependencies --configuration androidReleaseRuntimeClasspath
   ```

2. Adjust `includeConfigs` based on output

### Empty SBOM

- Ensure plugin is applied to the correct module
- Verify configurations exist: `./gradlew :composeApp:dependencies`
- Check `skipConfigs` is not excluding everything

### Build Cache Issues

Clear cache if needed:

```bash
./gradlew clean
./gradlew --no-build-cache cyclonedxBom
```

## Integration Examples

### Gradle Task Dependencies

Run SBOM generation before build:

```kotlin
tasks.named("build") {
    dependsOn(tasks.cyclonedxDirectBom)
}
```

### Automated Comparison

Compare dependencies between platforms:

```bash
# Generate SBOMs
./gradlew cyclonedxDirectBom

# Parse and compare (example with jq)
jq '.components[] | .name' build/reports/cyclonedx-direct/bom.json
```

## Best Practices

1. **Version Control**: Add SBOM output directories to `.gitignore`
   ```
   **/build/reports/cyclonedx/
   **/build/reports/cyclonedx-direct/
   ```

2. **CI Pipeline**: Generate SBOMs on each release build

3. **Security Scanning**: Upload generated SBOMs to vulnerability databases

4. **Documentation**: Keep SBOMs for release versions for audit trails

5. **Regular Updates**: Regenerate SBOMs when dependencies change

## Additional Resources

- [CycloneDX Official Documentation](https://cyclonedx.org/)
- [CycloneDX Gradle Plugin Repository](https://github.com/CycloneDX/cyclonedx-gradle-plugin)
- [CycloneDX Specification](https://cyclonedx.org/docs/1.6/)
- [OWASP Dependency-Track](https://github.com/DependencyTrack/dependency-track)

## Schema Version Support

Current configuration uses CycloneDX v1.6 (default). Supported versions:

| Plugin Version | CycloneDX Schema | Output Formats |
|---------------|------------------|----------------|
| 3.x.x         | v1.6             | JSON, XML      |
| 2.x.x         | v1.6             | JSON, XML      |
| 1.10.x        | v1.6             | JSON, XML      |

To change schema version:

```kotlin
import org.cyclonedx.model.schema.SchemaVersion

tasks.cyclonedxDirectBom {
    schemaVersion.set(SchemaVersion.VERSION_15)
}
```
