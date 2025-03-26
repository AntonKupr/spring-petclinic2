import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

/*
 * TeamCity Configuration for Spring PetClinic
 * 
 * This Kotlin DSL script defines the TeamCity build configurations for building and deploying
 * the Spring PetClinic application.
 */

version = "2023.11"

project {
    // Project details - name and description are determined by TeamCity server

    // VCS Root definition
    val vcsRoot = GitVcsRoot {
        id("SpringPetClinicVcs")
        name = "Spring PetClinic Git Repository"
        url = "https://github.com/spring-projects/spring-petclinic.git"
        branch = "main"
        branchSpec = "+:refs/heads/*"
    }
    vcsRoot(vcsRoot)

    // Build Configuration
    buildType {
        id("Build")
        name = "Build"
        description = "Builds the application, runs tests, and generates code coverage reports"

        vcs {
            root(vcsRoot)
        }

        steps {
            // Clean and package the application using Maven
            maven {
                name = "Clean and Package"
                goals = "clean package"
                runnerArgs = "-Dmaven.test.failure.ignore=true"
                jdkHome = "%env.JDK_17%"
            }
        }

        triggers {
            vcs {
                branchFilter = "+:*"
            }
        }

        features {
            perfmon {}
        }

        // Artifacts to publish
        artifactRules = """
            target/*.jar => build
            target/site/jacoco => coverage-report
        """.trimIndent()
    }

    // Deploy Configuration
    buildType {
        id("Deploy")
        name = "Deploy"
        description = "Deploys the application to a specified environment"

        vcs {
            root(vcsRoot)
        }

        // This build depends on the Build configuration
        dependencies {
            snapshot(RelativeId("Build")) {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }
            artifacts(RelativeId("Build")) {
                buildRule = lastSuccessful()
                artifactRules = "build/*.jar => deploy"
            }
        }

        steps {
            // Build Docker image
            script {
                name = "Build Docker Image"
                scriptContent = """
                    #!/bin/bash

                    # Set environment variable based on parameter
                    ENVIRONMENT=%env.DEPLOY_ENV%
                    if [ -z "${'$'}ENVIRONMENT" ]; then
                        ENVIRONMENT="dev"
                    fi

                    echo "Building Docker image for ${'$'}ENVIRONMENT environment"

                    # Build the Docker image
                    docker build -t spring-petclinic:${'$'}ENVIRONMENT .
                """.trimIndent()
            }

            // Deploy to environment
            script {
                name = "Deploy to Environment"
                scriptContent = """
                    #!/bin/bash

                    # Set environment variable based on parameter
                    ENVIRONMENT=%env.DEPLOY_ENV%
                    if [ -z "${'$'}ENVIRONMENT" ]; then
                        ENVIRONMENT="dev"
                    fi

                    echo "Deploying to ${'$'}ENVIRONMENT environment"

                    # Simulate deployment (replace with actual deployment commands)
                    echo "Starting container for ${'$'}ENVIRONMENT environment"
                    docker run -d --name spring-petclinic-${'$'}ENVIRONMENT -p 8080:8080 spring-petclinic:${'$'}ENVIRONMENT
                """.trimIndent()
            }
        }

        // Parameters for deployment
        params {
            select("DEPLOY_ENV", "dev", label = "Deployment Environment", 
                description = "Select the environment to deploy to",
                options = listOf("dev", "staging", "prod"))
        }
    }
}
