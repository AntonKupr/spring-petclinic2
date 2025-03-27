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

To debug settings scripts in IntelliJ IDEA, open the 'TeamCity'
tool window (View -> Tool Windows -> TeamCity), then click the
'Debug' button in the toolbar and select the desired settings file.
*/

version = "2019.2"

project {
    description = "Spring PetClinic Sample Application"

    // Define Main VCS Root
    val mainVcsRoot = DslContext.settingsRoot

    // Build Configuration
    buildType {
        id("PetClinicBuild")
        name = "Build and Test"
        description = "Compiles code and runs tests"

        vcs {
            root(mainVcsRoot)
        }

        steps {
            maven {
                name = "Compile and Test"
                goals = "clean package"
                runnerArgs = "-Dmaven.test.failure.ignore=true"
                userSettingsSelection = "local-proxy"
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
            target/*.jar => petclinic.zip
            target/classes => classes.zip
            target/test-classes => test-classes.zip
        """.trimIndent()
    }
}
