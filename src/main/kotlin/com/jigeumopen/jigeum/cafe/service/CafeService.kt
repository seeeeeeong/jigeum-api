package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.cafe.dto.request.CreateCafeRequest
import com.jigeumopen.jigeum.cafe.dto.response.CafeResponse
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import com.jigeumopen.jigeum.cafe.service.mapper.CafeMapper
import com.jigeumopen.jigeum.common.exception.BusinessException
import com.jigeumopen.jigeum.common.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CafeService(
    private val cafeRepository: CafeRepository,
    private val cafeMapper: CafeMapper
) {
    fun getAll(): List<CafeResponse> =
        cafeRepository.findAll().map { CafeResponse.from(it) }

    fun get(id: Long): CafeResponse =
        CafeResponse.from(findCafeById(id))

    @Transactional
    fun create(request: CreateCafeRequest): CafeResponse {
        val cafe = cafeMapper.toEntity(request)
        return CafeResponse.from(cafeRepository.save(cafe))
    }

    @Transactional
    fun update(id: Long, request: CreateCafeRequest): CafeResponse {
        val existingCafe = findCafeById(id)
        val updatedCafe = cafeMapper.toEntity(request, existingCafe.id)
        return CafeResponse.from(cafeRepository.save(updatedCafe))
    }

    @Transactional
    fun delete(id: Long) {
        findCafeById(id)
        cafeRepository.deleteById(id)
    }

    private fun findCafeById(id: Long): Cafe =
        cafeRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.CAFE_NOT_FOUND) }
}
