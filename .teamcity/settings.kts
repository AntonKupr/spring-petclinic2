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

version = "2023.11"

project {
    description = "Spring PetClinic TeamCity Configuration"

    // Define VCS Root
    val vcsRoot = GitVcsRoot {
        id("SpringPetClinicVcs")
        name = "Spring PetClinic Git Repository"
        url = "https://github.com/spring-projects/spring-petclinic.git"
        branch = "refs/heads/main"
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
            // Clean and package with Maven
            maven {
                name = "Clean and Package"
                goals = "clean package"
                runnerArgs = "-Dmaven.test.failure.ignore=true"
                jdkHome = "%env.JDK_17_0%"
            }

            // Alternative Gradle build step
            gradle {
                name = "Gradle Build (Alternative)"
                tasks = "clean build"
                gradleParams = "-x test" // Skip tests as they're already run by Maven
                enabled = false // Disabled by default, can be enabled if needed
            }
        }

        triggers {
            vcs {
                branchFilter = "+:*"
            }
        }

        features {
            perfmon {
            }
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
        description = "Deploys the application to a specified environment"

        vcs {
            root(vcsRoot)
        }

        params {
            select("env.DEPLOY_ENV", "dev", label = "Deployment Environment", 
                   options = listOf("dev", "staging", "prod"))
        }

        steps {
            // Build Docker image
            script {
                name = "Build Docker Image"
                scriptContent = """
                    #!/bin/bash

                    echo "Building Docker image for Spring PetClinic"
                    ./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=spring-petclinic:latest
                """.trimIndent()
            }

            // Deploy to environment
            script {
                name = "Deploy to Environment"
                scriptContent = """
                    #!/bin/bash

                    ENV="%env.DEPLOY_ENV%"
                    echo "Deploying to """ + "\$ENV" + """ environment"

                    case """ + "\"\$ENV\"" + """ in
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
                            # Add deployment commands for production
                            ;;
                        *)
                            echo "Unknown environment: """ + "\$ENV" + """"
                            exit 1
                            ;;
                    esac
                """.trimIndent()
            }
        }

        dependencies {
            snapshot(RelativeId("Build")) {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }
        }

        features {
            perfmon {
            }
        }
    }
}
