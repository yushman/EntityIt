package ru.tomindapps.entityit.mapper

interface EntityMapper<Domain, Entity>{
    fun mapDomainToEntity(domain: Domain): Entity
    fun mapEntityToDomain(entity: Entity): Domain
}