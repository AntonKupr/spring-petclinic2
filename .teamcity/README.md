# TeamCity Configuration for Spring PetClinic

This directory contains the TeamCity Kotlin DSL configuration for building and deploying the Spring PetClinic application.

## Overview

The TeamCity configuration consists of two build configurations:

1. **Build** - Builds the Spring PetClinic application, runs tests, and generates code coverage reports
2. **Deploy** - Deploys the Spring PetClinic application to a specified environment (dev, staging, or prod)

## Prerequisites

To use this TeamCity configuration, you need:

1. A TeamCity server (version 2023.11 or later)
2. Build agents with the following tools installed:
   - JDK 17
   - Maven
   - Docker (for deployment)

## Setup

1. Import this project into TeamCity by pointing it to your Git repository
2. TeamCity will automatically detect the Kotlin DSL configuration in the `.teamcity` directory
3. The build configurations will be created automatically

## Build Configuration

The Build configuration performs the following steps:

1. Cleans and packages the application using Maven
2. Runs tests and generates code coverage reports using JaCoCo
3. Produces artifacts (JAR file and coverage reports)

## Deploy Configuration

The Deploy configuration performs the following steps:

1. Prepares the deployment by copying the JAR file to a deploy directory
2. Builds a Docker image for the application
3. Deploys the application to the selected environment:
   - **dev**: Deploys to a local Docker container
   - **staging**: Example deployment to a staging server (commented out)
   - **prod**: Example deployment to Kubernetes (commented out)

### Deployment Parameters

- **Deployment Environment**: Select the environment to deploy to (dev, staging, or prod)

## Customization

You can customize the deployment steps in the `Deploy` build configuration to match your specific deployment environment:

1. For server deployment, uncomment and modify the SCP and SSH commands
2. For Kubernetes deployment, uncomment and modify the kubectl commands
3. Add additional steps as needed for your deployment process

## Troubleshooting

If you encounter issues with the TeamCity configuration:

1. Check that your build agents have the required tools installed
2. Verify that the environment variables (e.g., `%env.JDK_17%`) are properly defined in TeamCity
3. Check the build logs for specific error messages

## Further Resources

- [TeamCity Documentation](https://www.jetbrains.com/help/teamcity/teamcity-documentation.html)
- [Kotlin DSL Documentation](https://www.jetbrains.com/help/teamcity/kotlin-dsl.html)
- [Spring PetClinic Documentation](https://github.com/spring-projects/spring-petclinic)
