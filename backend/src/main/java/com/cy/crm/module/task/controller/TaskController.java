package com.cy.crm.module.task.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.task.service.TaskService;
import com.cy.crm.module.task.vo.TaskVO;
import com.cy.crm.module.auth.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "任务管理")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "分页查询任务")
    @GetMapping
    public ApiResult<Page<TaskVO>> pageTasks(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status
    ) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResult.success(taskService.pageTasks(current, size, status, userId));
    }

    @Operation(summary = "查询今日待办任务")
    @GetMapping("/today")
    public ApiResult<Page<TaskVO>> pageTodayTasks(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size
    ) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResult.success(taskService.pageTodayTasks(current, size, userId));
    }

    @Operation(summary = "完成任务")
    @PostMapping("/{id}/complete")
    public ApiResult<Void> completeTask(@PathVariable Long id) {
        taskService.completeTask(id);
        return ApiResult.success();
    }

    @Operation(summary = "关闭任务")
    @PostMapping("/{id}/close")
    public ApiResult<Void> closeTask(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        taskService.closeTask(id, reason);
        return ApiResult.success();
    }
}
