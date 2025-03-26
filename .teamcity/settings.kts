import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

/*
 * Spring PetClinic TeamCity Configuration
 * 
 * This file defines the TeamCity build configurations for building and deploying
 * the Spring PetClinic application.
 */

// The version of TeamCity DSL being used
version = "2023.11"

// Project configuration
project {
    // Project details
    description = "Spring PetClinic Sample Application"

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

        requirements {
            contains("env.JDK_VERSION", "17")
        }

        steps {
            // Maven build step
            maven {
                name = "Clean and Package"
                goals = "clean package"
                runnerArgs = "-Dmaven.test.failure.ignore=true"
                jdkHome = "%env.JDK_17_HOME%"
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

        artifactRules = """
            target/*.jar
            target/site/jacoco => coverage-reports.zip
        """.trimIndent()
    }

    // Deploy Configuration
    buildType {
        id("Deploy")
        name = "Deploy"
        description = "Deploys the application to a specified environment (dev, staging, or prod)"

        vcs {
            root(vcsRoot)
        }

        requirements {
            contains("env.JDK_VERSION", "17")
            exists("docker.version")
        }

        params {
            select("env.DEPLOY_ENV", "dev", label = "Deployment Environment", 
                options = listOf("dev", "staging", "prod"), 
                description = "Select the environment to deploy to")
        }

        steps {
            // Build Docker image
            script {
                name = "Build Docker Image"
                scriptContent = """
                    #!/bin/bash
                    ./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=spring-petclinic:latest
                """.trimIndent()
            }

            // Deploy to environment
            script {
                name = "Deploy to Environment"
                scriptContent = """
                    #!/bin/bash
                    echo "Deploying to %env.DEPLOY_ENV% environment..."

                    # Example deployment script - customize as needed
                    case "%env.DEPLOY_ENV%" in
                        dev)
                            echo "Deploying to development environment"
                            # Add deployment commands for dev
                            ;;
                        staging)
                            echo "Deploying to staging environment"
                            # Add deployment commands for staging
                            ;;
                        prod)
                            echo "Deploying to production environment"
                            # Add deployment commands for prod
                            ;;
                        *)
                            echo "Unknown environment: %env.DEPLOY_ENV%"
                            exit 1
                            ;;
                    esac
                """.trimIndent()
            }
        }

    }
}
