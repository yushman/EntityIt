# Entity It
[![Maven Central](https://img.shields.io/maven-central/v/io.github.yushman/entityit)](https://repo1.maven.org/maven2/io/github/yushman/entityit/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
## KSP processor that's generate entities and mappers for domain classes

Generates `data class SampleClassEntity(...)` from any domain `SampleClass` with two-way mappers, avoiding boilerplate
code.  

## Example

#### *Domain class*

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
    val array: Array<Int>?,
    val mlist: Collection<String>?,
    val map: Map<String, String>?,
)
```

#### *Generated result*

```kotlin
internal data class SampleClassEntity(
    public val id: String,
    public val flag: Boolean,
    public val iAmFloat: Float,
    public val iAmDouble: Double,
    public val array: Array<Int>?,
    public val mlist: Collection<String>?,
    public val map: Map<String, String>?,
) {
    public fun toDomain(): SampleClass = SampleClass(
        id = id,
        flag = flag,
        integer = iAmFloat,
        long = SampleClassMappers.iogithubyushmanentityitsampleSampleMapper.mapEntityToDomain(iAmDouble),
        double = double,
        array = array,
        mlist = mlist,
        map = map,
    )
}

internal fun SampleClass.toEntity(): SampleClassEntity = SampleClassEntity(
    id = id,
    flag = flag,
    iAmFloat = integer,
    iAmDouble = SampleClassMappers.iogithubyushmanentityitsampleSampleMapper.mapDomainToEntity(long),
    array = array,
    mlist = mlist,
    map = map,
)
```

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

## Customizing source class

### Customizing source class annotation `@Entity`

+ `kotlinSerializable` - adds `kotlinx.serialization.Serializable` annotation to result class, default - `true`
+ `nullability` - make result class values nullable and adds default `= null` parameter, or inherit nullability from
  source class, default - `true`
+ `generateMappers` - generates `fun Entity.toDomain(): Domain` and `fun Domain.toEntity(): Entity` annotation to result
file, default - `true`

### Customizing source class values

+ `@Entity.NotNull` - value mast not be null in result class
+ `@Entity.Named("name")` - changes the name of the value in result class
+ `@Entity.@Entity.MapWith(in EntityMapper::class)` - maps the result values with defined mapper in result class
