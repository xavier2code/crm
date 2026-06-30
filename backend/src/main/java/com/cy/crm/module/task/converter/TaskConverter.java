package com.cy.crm.module.task.converter;

import com.cy.crm.module.task.entity.Task;
import com.cy.crm.module.task.vo.TaskVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 任务对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface TaskConverter {

    TaskConverter INSTANCE = Mappers.getMapper(TaskConverter.class);

    /**
     * 实体 -> VO
     * ownerUserName/customerName/planStageName/statusName 由 Service 层查询后设置
     */
    @Mapping(target = "ownerUserName", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "planStageName", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    TaskVO entityToVO(Task task);

    /**
     * 实体列表 -> VO列表
     */
    List<TaskVO> entitiesToVOs(List<Task> tasks);
}
