package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_channel")
public class Channel {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String region;
    private Integer status;
}
