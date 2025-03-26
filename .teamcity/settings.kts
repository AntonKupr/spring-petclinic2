import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.codeCoverage
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the
    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2023.11"

project {
    description = "Spring PetClinic Sample Application"

    buildType(Build)
    buildType(Deploy)
}

object Build : BuildType({
    name = "Build"
    description = "Build Spring PetClinic application"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        maven {
            name = "Clean and Package"
            goals = "clean package"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
            jdkHome = "%env.JDK_17%"
            mavenVersion = custom {
                path = "%teamcity.tool.maven.DEFAULT%"
            }
            userSettingsSelection = "settings-file"
            userSettingsPath = "settings.xml"
        }

        maven {
            name = "Run Tests with Coverage"
            goals = "test jacoco:report"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
            jdkHome = "%env.JDK_17%"
            mavenVersion = custom {
                path = "%teamcity.tool.maven.DEFAULT%"
            }
            userSettingsSelection = "settings-file"
            userSettingsPath = "settings.xml"
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

        // Add code coverage reporting
        codeCoverage {
            toolType = jacoco {
                classLocations = "+:target/classes/**"
                reportPath = "target/site/jacoco/jacoco.xml"
            }
        }
    }

    artifactRules = """
        target/*.jar
        target/site/jacoco/** => coverage-report.zip
    """.trimIndent()
})

object Deploy : BuildType({
    name = "Deploy"
    description = "Deploy Spring PetClinic application"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        // Step 1: Download artifacts from the Build configuration
        script {
            name = "Prepare Deployment"
            scriptContent = """
                echo "Preparing for deployment..."
                mkdir -p deploy
                cp %teamcity.build.checkoutDir%/target/*.jar deploy/
                echo "Preparation completed"
            """.trimIndent()
        }

        // Step 2: Build Docker image
        script {
            name = "Build Docker Image"
            scriptContent = """
                echo "Building Docker image..."
                cd deploy

                # Create a Dockerfile if it doesn't exist
                cat > Dockerfile << EOF
FROM eclipse-temurin:17-jdk
VOLUME /tmp
COPY *.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EOF

                # Build the Docker image
                docker build -t spring-petclinic:latest .

                echo "Docker image built successfully"
            """.trimIndent()
        }

        // Step 3: Deploy to environment (example for different environments)
        script {
            name = "Deploy to Environment"
            scriptContent = """
                echo "Deploying Spring PetClinic application..."

                # Example deployment to different environments based on a parameter
                if [ "%env.DEPLOY_ENV%" == "dev" ]; then
                    echo "Deploying to DEV environment"
                    # Example: Deploy to local Docker
                    docker stop petclinic || true
                    docker rm petclinic || true
                    docker run -d --name petclinic -p 8080:8080 spring-petclinic:latest

                elif [ "%env.DEPLOY_ENV%" == "staging" ]; then
                    echo "Deploying to STAGING environment"
                    # Example: Deploy to a staging server
                    # scp deploy/*.jar user@staging-server:/path/to/deployment/
                    # ssh user@staging-server "systemctl restart petclinic"

                elif [ "%env.DEPLOY_ENV%" == "prod" ]; then
                    echo "Deploying to PRODUCTION environment"
                    # Example: Deploy to Kubernetes
                    # kubectl apply -f k8s/deployment.yaml
                    # kubectl rollout restart deployment/petclinic

                else
                    echo "Unknown environment: %env.DEPLOY_ENV%"
                    exit 1
                fi

                echo "Deployment completed successfully"
            """.trimIndent()
        }
    }

    // Define parameters for the deployment
    params {
        select("env.DEPLOY_ENV", "dev", label = "Deployment Environment", 
            description = "Select the environment to deploy to",
            options = listOf("dev", "staging", "prod"))
    }

    dependencies {
        snapshot(Build) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }

    requirements {
        exists("docker.path")
    }

    artifactRules = """
        deploy/** => deploy.zip
    """.trimIndent()
})
