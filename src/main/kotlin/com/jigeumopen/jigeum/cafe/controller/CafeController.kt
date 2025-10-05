package com.jigeumopen.jigeum.cafe.controller

import com.jigeumopen.jigeum.cafe.dto.request.CreateCafeRequest
import com.jigeumopen.jigeum.cafe.dto.response.ApiResponse
import com.jigeumopen.jigeum.cafe.service.CafeService
import com.jigeumopen.jigeum.common.constants.CafeConstants.Api.BASE_PATH
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_PATH/cafes")
@Validated
class CafeController(
    private val cafeService: CafeService
) {
    @GetMapping
    fun getAll() = ApiResponse.success(cafeService.getAll())

    @GetMapping("/{id}")
    fun get(@PathVariable @Min(1) id: Long) =
        ApiResponse.success(cafeService.get(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateCafeRequest) =
        ApiResponse.success(cafeService.create(request))

    @PutMapping("/{id}")
    fun update(
        @PathVariable @Min(1) id: Long,
        @Valid @RequestBody request: CreateCafeRequest
    ) = ApiResponse.success(cafeService.update(id, request))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable @Min(1) id: Long): ApiResponse<Unit> {
        cafeService.delete(id)
        return ApiResponse.success(Unit)
    }
}
