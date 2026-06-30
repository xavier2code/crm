package com.cy.crm.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.admin.service.UserService;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.task.converter.TaskConverter;
import com.cy.crm.module.task.entity.Task;
import com.cy.crm.module.task.mapper.TaskMapper;
import com.cy.crm.module.task.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService extends ServiceImpl<TaskMapper, Task> {

    private final TaskMapper taskMapper;
    private final CustomerMapper customerMapper;
    private final UserService userService;
    private final DictionaryService dictionaryService;
    private final TaskConverter taskConverter;

    public Page<TaskVO> pageTasks(Long current, Long size, Integer status, Long userId) {
        QueryWrapper<Task> wrapper = new QueryWrapper<Task>()
                .eq("owner_user_id", userId)
                .eq(status != null, "status", status)
                .orderByAsc("plan_date");
        Page<Task> page = taskMapper.selectPage(new Page<>(current, size), wrapper);
        Page<TaskVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    public Page<TaskVO> pageTodayTasks(Long current, Long size, Long userId) {
        QueryWrapper<Task> wrapper = new QueryWrapper<Task>()
                .eq("owner_user_id", userId)
                .eq("status", 1)
                .le("plan_date", LocalDate.now())
                .orderByAsc("plan_date");
        Page<Task> page = taskMapper.selectPage(new Page<>(current, size), wrapper);
        Page<TaskVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.resourceNotFound("任务");
        }
        task.setStatus(2);
        taskMapper.updateById(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeTask(Long id, String reason) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.resourceNotFound("任务");
        }
        task.setStatus(3);
        task.setCloseReason(reason);
        taskMapper.updateById(task);
    }

    private TaskVO toVO(Task task) {
        TaskVO vo = taskConverter.entityToVO(task);

        Customer customer = customerMapper.selectById(task.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getName());
        }

        com.cy.crm.module.admin.entity.User owner = userService.getUserEntityById(task.getOwnerUserId());
        vo.setOwnerUserName(owner != null ? owner.getRealName() : null);
        vo.setPlanStageName(dictionaryService.getDictionaryName("stage_6", task.getPlanStage()));
        vo.setStatusName(getStatusName(task.getStatus()));

        return vo;
    }

    private String getStatusName(Integer status) {
        return switch (status) {
            case 1 -> "待完成";
            case 2 -> "已完成";
            case 3 -> "已关闭";
            default -> "未知";
        };
    }
}
