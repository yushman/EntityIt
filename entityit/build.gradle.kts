import java.util.*
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "1.9.21"
    id("com.vanniktech.maven.publish") version "0.29.0"
    id("signing")
}

val properties = Properties().apply {
    rootProject.file("local.properties").reader().use(::load)
}

lateinit var sourcesArtifact: PublishArtifact

val pGroupId = properties["groupId"] as String
val pArtifactId = properties["artifactId"] as String
val pVersion = properties["version"] as String


group = pGroupId
version = pVersion

dependencies {

    implementation("com.squareup:kotlinpoet:1.17.0")
    implementation("com.squareup:kotlinpoet-ksp:1.17.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.23-1.0.20")
}

tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(java.sourceSets["main"].java.srcDirs)
    }
    artifacts {
        sourcesArtifact = archives(sourcesJar)
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    configure(KotlinJvm(sourcesJar = true))
    coordinates(pGroupId, pArtifactId, pVersion)

    pom {
        name.set("Entity It")
        description.set("Java lib to generate Entity classes via ksp")
        inceptionYear.set("2024")
        url.set("https://github.com/yushman/EntityIt")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("yushman")
                name.set("Ivan Yush")
                url.set("https://github.com/yushman/")
            }
        }
        scm {
            url.set("https://github.com/yushman/EntityIt/")
            connection.set("scm:git:ssh://github.com/yushman/EntityIt.git")
            developerConnection.set("scm:git:ssh://github.com/yushman/EntityIt.git")
        }
    }
}

signing {
    useInMemoryPgpKeys(
        properties["signing.key"] as String,
        properties["signing.password"] as String,
    )
    sign(publishing.publications)
}