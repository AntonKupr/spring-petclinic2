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
                   description = "Select the environment to deploy to",
                   options = listOf("dev", "staging", "prod"))
        }
        
        steps {
            // Build Docker image
            script {
                name = "Build Docker Image"
                scriptContent = """
                    #!/bin/bash
                    
                    echo "Building Docker image for Spring PetClinic"
                    docker build -t spring-petclinic:%build.number% .
                """.trimIndent()
            }
            
            // Deploy to selected environment
            script {
                name = "Deploy to Environment"
                scriptContent = """
                    #!/bin/bash
                    
                    echo "Deploying to %env.DEPLOY_ENV% environment"
                    
                    case "%env.DEPLOY_ENV%" in
                      dev)
                        echo "Deploying to development environment"
                        # Add deployment commands for dev environment
                        ;;
                      staging)
                        echo "Deploying to staging environment"
                        # Add deployment commands for staging environment
                        ;;
                      prod)
                        echo "Deploying to production environment"
                        # Add deployment commands for production environment
                        ;;
                      *)
                        echo "Unknown environment: %env.DEPLOY_ENV%"
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
    }
}
