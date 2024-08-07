# Entity It
[![Maven Central](https://img.shields.io/maven-central/v/io.github.yushman/entityit)](https://repo1.maven.org/maven2/io/github/yushman/entityit/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
## KSP processor that's generate entities and mappers for domain classes

Generates `data class SampleClassEntity(...)` from any domain `SampleClass` with two-way mappers, avoiding boilerplate
code.  

## Installation

```kotlin
// in build.gradle.kts module file
plugins{
    // ...
    id("com.google.devtools.ksp") version "1.9.21-1.0.16" // KSP support, version = kotlin plugin version
}

dependencies {
    // ...
    ksp("io.github.yushman:entityit:$version")
    implementation("io.github.yushman:entityit:$version")
}
```

## Usage

You can see **sample** folder for usage

Define domain class and annotate in with `@Entity`  
Customize the Entity class with other annotations and parameters

```kotlin
@Entity(
    kotlinSerializable = false,
)
internal data class SampleClass(
    @Entity.NotNull
    val id: String,
    val flag: Boolean,
    @Entity.Named("iAmFloat")
    val integer: Float,
    @Entity.Named("iAmDouble")
    @Entity.MapWith(SampleMapper::class)
    val long: Long,
)
```

Customizing source class annotation `@Entity`  
`kotlinSerializable` - adds `kotlinx.serialization.Serializable` annotation to result class, default - `true`  
`nullability` - make result class values nullable and adds default `= null` parameter, or inherit nullability from source class, default - `true`  
`generateMappers` - generates `fun Entity.toDomain(): Domain` and `fun Domain.toEntity(): Entity` annotation to result
file, default - `true`  

Customizing source class values  
`@Entity.NotNull` - value mast not be null in result class  
`@Entity.Named("name")` - changes the name of the value in result class  
`@Entity.@Entity.MapWith(in EntityMapper::class)` - maps the result values with defined mapper in result class  

## Result

Generated file tree like:

#### *In*

```kotlin
@Entity(
    kotlinSerializable = false,
)
internal data class SampleClass(
    @Entity.NotNull
    val id: String,
    val flag: Boolean,
    @Entity.Named("iAmFloat")
    val integer: Float,
    @Entity.Named("iAmDouble")
    @Entity.MapWith(SampleMapper::class)
    val long: Long,
)
```

#### *Result*

```kotlin
internal data class SampleClassEntity(
    val id: String,
    val flag: Boolean? = null,
    val iAmFloat: Float? = null,
    val iAmDouble: Double? = null,
) {
    fun toDomain(): SampleClass = SampleClass(
        id = id!!,
        flag = flag!!,
        integer = iAmFloat!!,
        long = SampleClassMappers.rutomindappsentityitsampleSampleMapper.mapEntityToDomain(iAmDouble!!),
    )
}

internal fun SampleClass.toEntity(): SampleClassEntity = SampleClassEntity(
    id = id,
    flag = flag,
    iAmFloat = integer,
    iAmDouble = SampleClassMappers.rutomindappsentityitsampleSampleMapper.mapDomainToEntity(long),
)
```
