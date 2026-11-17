import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal object Sdk {
    const val COMPILE = 37
    const val TARGET = 35
    const val MIN = 23
}

/** Shared Android settings for application and library modules. */
internal fun Project.configureAndroid(extension: CommonExtension) {
    extension.apply {
        compileSdk = Sdk.COMPILE
        defaultConfig.minSdk = Sdk.MIN
        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
        // java.time (and kotlinx-datetime, used by the Adhan library) on API 23-25.
        compileOptions.isCoreLibraryDesugaringEnabled = true
        testOptions.unitTests.isReturnDefaultValues = true
        lint.abortOnError = true
        lint.warningsAsErrors = false
        lint.disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
        // Media3's caching, data sources and session service are marked @UnstableApi; the app uses
        // them deliberately (pinned version), so the androidx opt-in lint check is not useful here.
        lint.disable += "UnsafeOptInUsageError"
    }
    configureKotlin()
    configureTestJvm()
    dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("desugar-jdk-libs").get())
    }
}

internal fun Project.configureKotlin() {
    extensions.getByType<KotlinBaseExtension>().jvmToolchain(17)
    tasks.withType(KotlinCompilationTask::class.java).configureEach {
        val opts = compilerOptions
        if (opts is KotlinJvmCompilerOptions) opts.jvmTarget.set(JvmTarget.JVM_17)
        opts.freeCompilerArgs.addAll(
            "-opt-in=kotlin.time.ExperimentalTime",
            "-Xannotation-default-target=param-property",
        )
    }
}

/**
 * The Adhan library ships Java 21 bytecode. D8 handles that for the app, but JVM unit tests
 * need a newer runtime than the JDK 17 toolchain used for compiling.
 */
internal fun Project.configureTestJvm() {
    val toolchains = extensions.getByType<JavaToolchainService>()
    tasks.withType(Test::class.java).configureEach {
        javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(TEST_JVM)) })
    }
}

private const val TEST_JVM = 21
