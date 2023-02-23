package builds.kotlinlang.buidTypes

import jetbrains.buildServer.configs.kotlin.AbsoluteId
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.FailureAction
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.schedule
import vcsRoots.KotlinLangOrg

const val SearchAppId = "7961PKYRXV"

object BuildSearchIndex : BuildType({
  id = AbsoluteId("Kotlin_KotlinSites_Builds_KotlinlangOrg_BuildBetaSerachIndex")

  name = "Build Site Search Index"
  description = "Build search index for Algolia using Google Analytics data"

  params {
    param("env.KEY_FILE_LOCATION", "/secrets/google-credentials.json")
    param("virtualenv.folder", "_environment")
    param("env.WH_INDEX_NAME", "prod_KOTLINLANG_WEBHELP")
    param("env.WH_SEARCH_USER", SearchAppId)
    param("env.WH_SEARCH_KEY", "%ALGOLIA_WRITE_API_KEY%")
  }

  vcs {
    root(KotlinLangOrg)

    cleanCheckout = true
    showDependenciesChanges = true
  }

  steps {
    script {
      scriptContent = """
        #!/bin/bash
        
        ## refresh packages
        pip install -r requirements.txt
        
        python kotlin-website.py index
      """.trimIndent()
      dockerImage = "%dep.Kotlin_KotlinSites_Builds_KotlinlangOrg_BuildPythonContainer.kotlin-website-image%"
      dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
      dockerPull = true
    }
  }

  triggers {
    schedule {
      schedulingPolicy = cron {
        hours = "3"
        dayOfMonth = "*/2"
      }
      branchFilter = "+:<default>"
      triggerBuild = always()
    }
  }

  features {
    dockerSupport {
      loginToRegistry = on {
        dockerRegistryId = "PROJECT_EXT_357"
      }
    }
  }

  dependencies {
    snapshot(AbsoluteId("Kotlin_KotlinSites_Builds_KotlinlangOrg_BuildPythonContainer")) {
      onDependencyFailure = FailureAction.FAIL_TO_START
      onDependencyCancel = FailureAction.CANCEL
    }
    dependency(BuildSitePages) {
      snapshot {}

      artifacts {
        artifactRules = "+:pages.zip!** => dist"
      }
    }
  }
})
