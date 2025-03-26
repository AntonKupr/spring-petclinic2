import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.
*/

version = "2023.11"

project {
    description = "Spring PetClinic Sample Application"

    // Define VCS Root
    val vcsRoot = GitVcsRoot {
        id("SpringPetClinicVcs")
        name = "spring-petclinic"
        url = "https://github.com/spring-projects/spring-petclinic.git"
        branch = "main"
    }
    vcsRoot(vcsRoot)

    // Build configuration for Maven
    buildType {
        id("BuildWithMaven")
        name = "Build with Maven"
        
        vcs {
            root(vcsRoot)
        }
        
        steps {
            maven {
                name = "Build and Test"
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
        
        requirements {
            exists("env.JDK_17")
        }
    }
    
    // Build configuration for Gradle
    buildType {
        id("BuildWithGradle")
        name = "Build with Gradle"
        
        vcs {
            root(vcsRoot)
        }
        
        steps {
            gradle {
                name = "Build and Test"
                tasks = "clean build"
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
        
        requirements {
            exists("env.JDK_17")
        }
    }
    
    // Docker image build and deploy
    buildType {
        id("DockerBuildAndDeploy")
        name = "Build and Deploy Docker Image"
        
        vcs {
            root(vcsRoot)
        }
        
        steps {
            maven {
                name = "Build Docker Image"
                goals = "spring-boot:build-image"
                jdkHome = "%env.JDK_17%"
            }
            
            script {
                name = "Deploy Docker Image"
                scriptContent = """
                    echo "Deploying Docker image..."
                    # Add deployment commands here
                    echo "Docker image deployed successfully"
                """.trimIndent()
            }
        }
        
        dependencies {
            snapshot(RelativeId("BuildWithMaven")) {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }
        }
        
        requirements {
            exists("env.JDK_17")
            exists("docker")
        }
    }
}
