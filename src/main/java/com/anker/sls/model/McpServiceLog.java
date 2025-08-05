package com.anker.sls.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

import java.util.Date;

/**
 * SLS服务方法调用日志表
 */
@Data
@TableName("mcp_service_log")
public class McpServiceLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 调用系统名称 */
    private String systemName;

    /** 调用project */
    private String project;

    /** 调用endpoint */
    private String endpoint;

    /** 被调用日志库 */
    private String logstore;

    /** 被调用方法 */
    private String method;

    /** 查询参数（JSON字符串） */
    private String queryParam;

    /** 查询结果（成功/失败） */
    private String result;

    /** 异常信息 */
    private String errorMsg;

    /** 查询耗时（毫秒） */
    private Long duration;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /** 是否删除 */
    private Boolean isDeleted;
} 