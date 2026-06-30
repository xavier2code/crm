package com.cy.crm.module.admin.controller;

import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.admin.dto.DictionaryRequest;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.admin.vo.DictionaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "字典维护")
@RestController
@RequestMapping("/api/admin/dictionaries")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @Operation(summary = "获取所有字典按类型分组")
    @GetMapping
    public ApiResult<Map<String, List<DictionaryVO>>> all() {
        return ApiResult.ok(dictionaryService.allByTypes());
    }

    @Operation(summary = "根据类型获取字典列表")
    @GetMapping("/{type}")
    public ApiResult<List<DictionaryVO>> listByType(@PathVariable String type) {
        return ApiResult.ok(dictionaryService.listByType(type));
    }

    @Operation(summary = "创建字典")
    @PostMapping
    public ApiResult<Void> create(@Valid @RequestBody DictionaryRequest request) {
        dictionaryService.create(request);
        return ApiResult.ok();
    }

    @Operation(summary = "编辑字典")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody DictionaryRequest request) {
        request.setId(id);
        dictionaryService.update(request);
        return ApiResult.ok();
    }

    @Operation(summary = "删除字典")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        dictionaryService.delete(id);
        return ApiResult.ok();
    }
}
