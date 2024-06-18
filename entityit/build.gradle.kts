import java.net.URI
import java.util.*

plugins {
    kotlin("jvm") version "1.9.21"
    id("maven-publish")
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

    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")
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

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = pGroupId
            artifactId = pArtifactId
            version = pVersion


            from(components["java"])
            artifact(sourcesArtifact)

            with(pom) {
                name = pArtifactId
                description = "Java lib to generate Entity classes via ksp"
                url = "https://github.com/yushman/EntityIt"
//                withXml {
//                    val root = asNode()
//                    root.appendNode("name", )
//                    root.appendNode("description", "Java lib to generate Entity classes via ksp")
//                    root.appendNode("url", "https://github.com/yushman/EntityIt")
//                }

                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }

                developers {
                    developer {
                        id = "yushman"
                        name = "Ivan Yush"
                        email = "tomindapps@gmail.com"
                    }
                }

                scm {
                    developerConnection = "scm:git:ssh://github.com/yushman/EntityIt.git"
                    url = "https://github.com/yushman/EntityIt"
                }
            }
        }
    }
    repositories {

        maven {
            name = "sonatype"
            url = URI.create("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = properties["sona-usr"] as String
                password = properties["sona-psw"] as String
            }
        }
    }
}

signing {
    sign(publishing.publications)
}

tasks.getByName("publishReleasePublicationToSonatypeRepository").dependsOn("assemble", "sourcesJar")