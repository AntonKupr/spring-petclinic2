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
 * This configuration includes:
 * - Build configuration: Builds the application, runs tests, and generates code coverage reports
 * - Deploy configuration: Deploys the application to a specified environment (dev, staging, or prod)
 */

version = "2023.11"

project {
    description = "Spring PetClinic TeamCity Configuration"
    
    // Define VCS Root
    val vcsRoot = GitVcsRoot {
        id("SpringPetClinicVcs")
        name = "Spring PetClinic VCS"
        url = "https://github.com/spring-projects/spring-petclinic.git"
        branch = "main"
        branchSpec = "+:refs/heads/*"
    }
    
    // Register VCS Root
    vcsRoot(vcsRoot)
    
    // Build Configuration
    buildType {
        id("Build")
        name = "Build"
        
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
            
            // Run tests and generate code coverage reports
            maven {
                name = "Run Tests with Coverage"
                goals = "verify"
                runnerArgs = "-P jacoco"
                jdkHome = "%env.JDK_17%"
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
        
        // Artifacts to be published
        artifactRules = """
            target/*.jar
            target/site/jacoco => coverage-report.zip
        """.trimIndent()
        
        requirements {
            exists("env.JDK_17")
        }
    }
    
    // Deploy Configuration
    buildType {
        id("Deploy")
        name = "Deploy"
        
        vcs {
            root(vcsRoot)
        }
        
        // Depend on the Build configuration
        dependencies {
            snapshot(RelativeId("Build")) {
                onDependencyFailure = FailureAction.FAIL_TO_START
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
                    
                    echo "Deploying to ${'$'}ENVIRONMENT environment"
                    
                    # Example deployment commands (customize as needed)
                    if [ "${'$'}ENVIRONMENT" == "dev" ]; then
                        echo "Deploying to development environment"
                        # Add deployment commands for dev
                    elif [ "${'$'}ENVIRONMENT" == "staging" ]; then
                        echo "Deploying to staging environment"
                        # Add deployment commands for staging
                    elif [ "${'$'}ENVIRONMENT" == "prod" ]; then
                        echo "Deploying to production environment"
                        # Add deployment commands for prod
                    else
                        echo "Unknown environment: ${'$'}ENVIRONMENT"
                        exit 1
                    fi
                """.trimIndent()
            }
        }
        
        // Parameters for deployment
        params {
            select("env.DEPLOY_ENV", "dev", label = "Deployment Environment", 
                description = "Select the environment to deploy to",
                options = listOf("dev", "staging", "prod"))
        }
        
        requirements {
            exists("env.JDK_17")
            exists("docker")
        }
    }
}
