package ru.tomindapps.entityit.sample

import ru.tomindapps.entityit.mapper.EntityMapper

class SampleMapper: EntityMapper<Long, Double> {
    override fun mapDomainToEntity(domain: Long): Double {
        return domain.toDouble()
    }

    override fun mapEntityToDomain(entity: Double): Long {
        return entity.toLong() ?: 0L
    }
}