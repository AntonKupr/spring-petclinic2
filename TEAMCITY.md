# TeamCity Configuration for Spring PetClinic

This project includes a TeamCity Kotlin DSL configuration for building and deploying the Spring PetClinic application.

## Quick Start

1. Import this project into TeamCity by pointing it to your Git repository
2. TeamCity will automatically detect the Kotlin DSL configuration in the `.teamcity` directory
3. The build configurations will be created automatically:
   - **Build**: Builds the application, runs tests, and generates code coverage reports
   - **Deploy**: Deploys the application to a specified environment (dev, staging, or prod)

## Prerequisites

- TeamCity server (version 2023.11 or later)
- Build agents with JDK 17, Maven, and Docker installed

## Configuration Details

The TeamCity configuration includes:

### Build Configuration

- Cleans and packages the application using Maven
- Runs tests and generates code coverage reports
- Produces artifacts (JAR file and coverage reports)

### Deploy Configuration

- Builds a Docker image for the application
- Deploys to different environments (dev, staging, prod)
- Supports customization for different deployment targets

## Documentation

For detailed documentation on the TeamCity configuration, see the [TeamCity README](.teamcity/README.md) in the `.teamcity` directory.

## Customization

You can customize the deployment steps in the `Deploy` build configuration to match your specific deployment environment. See the detailed documentation for more information.
