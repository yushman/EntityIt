package io.github.yushman.entityit.mapper

/**
 * Base class for mapper implementations.
 *
 * @author yushman
 */
interface EntityMapper<Domain, Entity>{
    fun mapDomainToEntity(domain: Domain): Entity
    fun mapEntityToDomain(entity: Entity): Domain
}