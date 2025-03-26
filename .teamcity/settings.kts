import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.
*/

version = "2023.11"

project {
    description = "Spring PetClinic Sample Application"

    // Default parameters for all build configurations
    params {
        param("env.JAVA_HOME", "%env.JDK_17%")
    }

    // Build configuration
    val build = buildType {
        id("Build")
        name = "Build"
        description = "Builds the application, runs tests, and generates code coverage reports"

        vcs {
            root(DslContext.settingsRoot)
        }

        steps {
            maven {
                name = "Clean and Package"
                goals = "clean package"
                runnerArgs = "-Dmaven.test.failure.ignore=true"
                mavenVersion = auto()
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
            target/site/jacoco => coverage-report.zip
        """.trimIndent()

        requirements {
            exists("env.JDK_17")
        }
    }

    // Deploy to Dev configuration
    buildType {
        id("DeployToDev")
        name = "Deploy to Dev"
        description = "Builds a Docker image and deploys to the Dev environment"

        vcs {
            root(DslContext.settingsRoot)
        }

        steps {
            script {
                name = "Build Docker Image"
                scriptContent = """
                    ./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=spring-petclinic:dev
                """.trimIndent()
            }
            script {
                name = "Deploy to Dev Environment"
                scriptContent = """
                    echo "Deploying to Dev environment..."
                    # Deployment commands would go here
                    # For example:
                    # docker run -d -p 8080:8080 --name spring-petclinic-dev spring-petclinic:dev
                """.trimIndent()
            }
        }

        dependencies {
            snapshot(build) {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }
            artifacts(build) {
                artifactRules = "*.jar => target/"
            }
        }

        requirements {
            exists("env.JDK_17")
            exists("env.DOCKER_HOST")
        }
    }

    // Deploy to Staging configuration
    val deployToStaging = buildType {
        id("DeployToStaging")
        name = "Deploy to Staging"
        description = "Builds a Docker image and deploys to the Staging environment"

        vcs {
            root(DslContext.settingsRoot)
        }

        steps {
            script {
                name = "Build Docker Image"
                scriptContent = """
                    ./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=spring-petclinic:staging
                """.trimIndent()
            }
            script {
                name = "Deploy to Staging Environment"
                scriptContent = """
                    echo "Deploying to Staging environment..."
                    # Deployment commands would go here
                    # For example:
                    # docker run -d -p 8081:8080 --name spring-petclinic-staging spring-petclinic:staging
                """.trimIndent()
            }
        }

        dependencies {
            snapshot(build) {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }
            artifacts(build) {
                artifactRules = "*.jar => target/"
            }
        }

        requirements {
            exists("env.JDK_17")
            exists("env.DOCKER_HOST")
        }
    }

    // Deploy to Production configuration
    buildType {
        id("DeployToProd")
        name = "Deploy to Production"
        description = "Builds a Docker image and deploys to the Production environment"

        vcs {
            root(DslContext.settingsRoot)
        }

        steps {
            script {
                name = "Build Docker Image"
                scriptContent = """
                    ./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=spring-petclinic:prod
                """.trimIndent()
            }
            script {
                name = "Deploy to Production Environment"
                scriptContent = """
                    echo "Deploying to Production environment..."
                    # Deployment commands would go here
                    # For example:
                    # docker run -d -p 80:8080 --name spring-petclinic-prod spring-petclinic:prod
                """.trimIndent()
            }
        }

        dependencies {
            snapshot(deployToStaging) {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }
            artifacts(build) {
                artifactRules = "*.jar => target/"
            }
        }

        requirements {
            exists("env.JDK_17")
            exists("env.DOCKER_HOST")
        }
    }
}
