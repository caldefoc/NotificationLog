# JDK 21 Setup Guide

## Quick Start

1. **Install JDK 21** if not already installed:
   - Download from: https://adoptium.net/temurin/releases/?version=21

2. **Configure JDK 21** for this project:
   - Edit `gradle.properties` and set:
     ```properties
     org.gradle.java.home=C:\\Path\\To\\Your\\JDK-21
     ```

## Verification

Run the validation task to check your setup:

```bash
./gradlew validateJdk
```

It should output: `✅ SUCCESS: JDK 21 detected correctly`