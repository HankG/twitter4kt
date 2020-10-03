package com.codingfeline.twitter4kt.buildsrc

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.withConvention
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.util.*

const val TESTCONFIG_DIR = "testconfig"

@Suppress("unused")
class BuildHelperPlugin : Plugin<Project> {

    private val ignoreModules = listOf(":core", ":v1")

    private var _secrets: Properties? = null

    private val Project.secrets: Properties
        get() = _secrets ?: kotlin.run { loadSecrets().also { _secrets = it } }

    override fun apply(target: Project) {
        target.subprojects {
            if (ignoreModules.contains(it.path)) return@subprojects
            it.afterEvaluate { prj ->
                prj.configureKotlin()
                prj.configureApiModules()
                prj.configureMavenPublications(prj.secrets)
            }
        }
    }

    private fun Project.configureKotlin() {
        tasks.withType<KotlinCompile>().all {
            it.sourceCompatibility = "1.8"
            it.targetCompatibility = "1.8"
            it.kotlinOptions.jvmTarget = "1.8"
        }
        withConvention(JavaPluginConvention::class) {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        kotlinMultiplatformExtension.apply {
            sourceSets.all { sourceSet ->
                sourceSet.languageSettings
                    .useExperimentalAnnotation("kotlin.RequiresOptIn")
            }
            targets.all { target ->
                when (target.platformType) {
                    KotlinPlatformType.jvm -> {
                        (target as KotlinJvmTarget).compilations.all {
                            it.compileKotlinTask.sourceCompatibility = "1.8"
                            it.compileKotlinTask.targetCompatibility = "1.8"
                            it.kotlinOptions.jvmTarget = "1.8"
                        }
                    }
                    else -> {
                        // no-op
                    }
                }
            }
        }
    }

    private fun Project.configureApiModules() {
        if (path.endsWith("-api")) {
            setupTestConfig()
        }
    }

    private fun Project.setupTestConfig() {
        val task = createTestConfigTask()
        // TODO depends on test tasks
        tasks.findByName("compileTestKotlinJvm")?.dependsOn(task)

        kotlinMultiplatformExtension.sourceSets.getByName("commonTest")
            .kotlin.srcDir("${buildDir}/$TESTCONFIG_DIR")
    }

    private fun Project.createTestConfigTask(): TaskProvider<Task> {
        val task = tasks.register("createTestConfig") {
            val outputDir = File("${buildDir}/$TESTCONFIG_DIR")
            val consumerKey = secrets["twitter_consumer_key"]
            val consumerSecret = secrets["twitter_consumer_secret"]
            val accessToken = secrets["twitter_access_token"]
            val accessTokenSecret = secrets["twitter_access_token_secret"]
            val userId = secrets["twitter_user_id"]
            val screenName = secrets["twitter_screen_name"]

            it.inputs.property("consumerKey", consumerKey)
            it.inputs.property("consumerSecret", consumerSecret)
            it.inputs.property("accessToken", accessToken)
            it.inputs.property("accessTokenSecret", accessTokenSecret)
            it.inputs.property("userId", userId)
            it.inputs.property("screenName", screenName)
            it.outputs.dir(outputDir)
            group = "twitter4kt"

            it.doLast {
                val configFile = file("$outputDir/com/codingfeline/twitter4kt/TestConfig.kt")
                if (configFile.exists()) configFile.delete()

                configFile.parentFile.mkdirs()
                configFile.writeText(
                    """// Generated file. Do not edit!
                        |package com.codingfeline.twitter4kt
                        |
                        |val TEST_CONSUMER_KEY = "$consumerKey"
                        |val TEST_CONSUMER_SECRET = "$consumerSecret"
                        |val TEST_ACCESS_TOKEN = "$accessToken"
                        |val TEST_ACCESS_TOKEN_SECRET = "$accessTokenSecret"
                        |val TEST_USER_ID = "$userId"
                        |val TEST_SCREEN_NAME = "$screenName"
                    """.trimMargin()
                )
            }
        }
        return task
    }

    private val Project.kotlinMultiplatformExtension: KotlinMultiplatformExtension
        get() = requireNotNull(extensions.getByType(KotlinMultiplatformExtension::class.java))
}
