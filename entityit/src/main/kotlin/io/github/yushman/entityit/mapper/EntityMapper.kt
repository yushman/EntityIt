package io.github.yushman.entityit.mapper

interface EntityMapper<Domain, Entity>{
    fun mapDomainToEntity(domain: Domain): Entity
    fun mapEntityToDomain(entity: Entity): Domain
}