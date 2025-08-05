package com.anker.sls.controller;

import com.anker.sls.model.McpServiceLog;
import com.anker.sls.service.SlsLogService;

import org.noear.solon.ai.annotation.PromptMapping;
import org.noear.solon.ai.annotation.ToolMapping;
import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.annotation.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import org.noear.solon.ai.mcp.server.annotation.McpServerEndpoint;

import com.anker.sls.util.ParamValidationUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;



@Slf4j
@RestController
@McpServerEndpoint(version = "1.0", sseEndpoint = "/mcp/sse/slslog")
public class SlsLogController {
    
    // 常量定义
    private static final String DEFAULT_LINE_COUNT = "100";
    private static final String DEFAULT_OFFSET = "0";
    private static final String DEFAULT_BACK_LINES = "70";
    private static final String DEFAULT_FORWARD_LINES = "30";
    private static final String DEFAULT_PAGE_SIZE = "10";
    
    @Autowired
    @Qualifier("SlsLogServiceImpl")
    private SlsLogService logService;

    @GetMapping("/getMcpServiceLog")
    @ToolMapping(description = "获取SLS日志服务调用记录")
    public IPage<McpServiceLog> getMcpServiceLog(
        @Param(description = "页码", defaultValue = "1", required = false) @RequestParam(defaultValue = "1") Integer page,
        @Param(description = "每页行数", defaultValue = "10", required = false) @RequestParam(defaultValue = "10") Integer size) {
        return logService.getMcpServiceLog(page, size);
    }
    
    @GetMapping("/getLogstoreList")
    @ToolMapping(description = "获取指定系统的日志库(Logstore)列表，支持系统别名自动识别，便于日志查询分支选择。")
    public List<Map<String, Object>> getLogstoreList(
            @Param(description = "系统名称") String systemName) {
        return logService.getLogstoreList(systemName);
    }

    @GetMapping("/getLogsPro")
    @ToolMapping(description = "日志关键字/traceId/ID/时间范围查询：自动分支，traceId/ID时仅用ID参数，其余按异常关键字或用户关键词/时间查询，严格遵循参数补全和分析规则。")
    public Map<String, Object> getLogsPro(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "起始时间（格式：yyyy-MM-dd HH:mm:ss）",required = false) String from,
            @Param(description = "结束时间（格式：yyyy-MM-dd HH:mm:ss）", required = false) String to,
            @Param(description = "查询条件（例如：\"Exception\" 或 \"ERROR\"）") String query,
            @Param(description = "返回行数", defaultValue = DEFAULT_LINE_COUNT) Integer line,
            @Param(description = "偏移量", defaultValue = DEFAULT_OFFSET) Integer offset,
            @Param(description = "系统名称") String systemName) {
        
        // 使用工具类处理时间参数和默认值
        long[] timeRange = ParamValidationUtil.processTimeParams(from, to);
        Boolean reverse = ParamValidationUtil.getBooleanWithDefault(null, false);
        Boolean powerSql = ParamValidationUtil.getBooleanWithDefault(null, false);
        String topic = ParamValidationUtil.getStringWithDefault(null, "");
        
        return logService.getLogsPro(logstore, timeRange[0], timeRange[1], query, line, offset, reverse, powerSql, topic, systemName);
    }
    
    @GetMapping("/context")
    @ToolMapping(description = "上下文调用链查询：根据packId和packMeta获取日志上下文，展示完整调用链，便于根因分析。仅在同时提供packId和packMeta时使用。")
    public Map<String, Object> getContextLogs(
            @Param(description = "包ID（pack_id）") String packId,
            @Param(description = "包元数据（pack_meta）") String packMeta,
            @Param(description = "日志库名称") String logstore,
            @Param(description = "查询开始行", defaultValue = DEFAULT_BACK_LINES) Integer backLines,
            @Param(description = "查询结束行", defaultValue = DEFAULT_FORWARD_LINES) Integer forwardLines,
            @Param(description = "系统名称") String systemName) {
        return logService.getContextLogs(logstore, packId, packMeta, backLines, forwardLines, systemName);    
    }

    @GetMapping("/getIndex")
    @ToolMapping(description = "获取指定日志库的索引配置，辅助日志字段分析和查询优化。")
    public Map<String, Object> getIndex(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "系统名称") String systemName) {
        return logService.getIndex(logstore, systemName);
    }

    @GetMapping("/getProject")
    @ToolMapping(description = "获取指定系统的项目配置信息，便于日志库映射和系统识别。")
    public Map<String, Object> getProject(
            @Param(description = "系统名称") String systemName) {
        return logService.getProject(systemName);
    }

    @GetMapping("/getHistograms")
    @ToolMapping(description = "获取日志直方图数据，支持时间范围、主题、查询语句参数，辅助日志趋势分析。")
    public Map<String, Object> getHistograms(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "起始时间（格式：yyyy-MM-dd HH:mm:ss）", required = false) String from,
            @Param(description = "结束时间（格式：yyyy-MM-dd HH:mm:ss）", required = false) String to,
            @Param(description = "日志主题", defaultValue = "") String topic,
            @Param(description = "查询语句") String query,
            @Param(description = "系统名称") String systemName) {
        // 使用工具类处理时间参数
        long[] timeRange = ParamValidationUtil.processTimeParams(from, to);
        return logService.getHistograms(logstore, timeRange[0], timeRange[1], topic, query, systemName);
    }

    @GetMapping("/diagnoseSLSConnection")
    @ToolMapping(description = "诊断指定日志库的SLS连接状态，辅助排查日志采集与查询异常。")
    public Map<String, Object> diagnoseSLSConnection(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "系统名称") String systemName) {
        return logService.diagnoseSLSConnection(logstore, systemName);
    }

    @GetMapping("/listProject")
    @ToolMapping(description = "列出指定项目下的所有Project信息，支持分页和资源组筛选，便于日志资源管理。")
    public Map<String, Object> listProject(
            @Param(description = "项目名称") String projectName,
            @Param(description = "分页起始", defaultValue = "0") Integer offset,
            @Param(description = "分页大小", defaultValue = "10") Integer size,
            @Param(description = "资源组ID") String resourceGroupId,
            @Param(description = "系统名称") String systemName) {
        return logService.listProject(projectName, offset, size, resourceGroupId, systemName);
    }

    @GetMapping("/listShards")
    @ToolMapping(description = "获取指定日志库下所有Shard分片信息，辅助日志分区与性能分析。")
    public List<Map<String, Object>> listShards(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "系统名称") String systemName) {
        return logService.listShards(logstore, systemName);
    }

    @GetMapping("/getRawLogs")
    @ToolMapping(description = "获取日志库原始日志数据，支持压缩方式和自定义参数，适用于特殊日志分析场景。")
    public Map<String, Object> getRawLogs(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "压缩方式", defaultValue = "gzip") String acceptEncoding,
            @Param(description = "查询参数") Map<String, Object> body,
            @Param(description = "系统名称") String systemName) {
        return logService.getRawLogs(logstore, acceptEncoding, body, systemName);
    }

    @GetMapping("/getCursor")
    @ToolMapping(description = "获取指定日志库分片(Shard)的Cursor游标，支持时间点定位，便于日志流式读取。")
    public Map<String, Object> getCursor(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "Shard ID") int shardId,
            @Param(description = "时间点（Unix时间戳字符串或\"begin\"/\"end\"）") String from,
            @Param(description = "系统名称") String systemName) {
        return logService.getCursor(logstore, shardId, from, systemName);
    }

    @GetMapping("/getCursorTime")
    @ToolMapping(description = "根据Cursor游标获取服务端时间，辅助日志定位与时间同步分析。")
    public Map<String, Object> getCursorTime(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "Shard ID") int shardId,
            @Param(description = "游标") String cursor,
            @Param(description = "系统名称") String systemName) {
        return logService.getCursorTime(logstore, shardId, cursor, systemName);
    }

    @GetMapping("/getLogstoreMeteringMode")
    @ToolMapping(description = "获取指定日志库的计量模式信息，辅助日志存储与计费分析。")
    public Map<String, Object> getLogstoreMeteringMode(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "系统名称") String systemName) {
        return logService.getLogstoreMeteringMode(logstore, systemName);
    }

    @GetMapping("/listMachineGroups")
    @ToolMapping(description = "列出目标Project下的机器组，支持分页和名称筛选，便于日志采集资源管理。")
    public Map<String, Object> listMachineGroups(
            @Param(description = "查询开始行", defaultValue = DEFAULT_OFFSET) Integer offset,
            @Param(description = "每页行数", defaultValue = DEFAULT_PAGE_SIZE) Integer size,
            @Param(description = "机器组名称", defaultValue = "") String groupName,
            @Param(description = "系统名称") String systemName) {
        return logService.listMachineGroups(offset, size, groupName, systemName);
    }

    @GetMapping("/listMachines")
    @ToolMapping(description = "列出目标机器组中与日志服务连接正常的机器列表，支持分页，辅助采集节点健康检查。")
    public Map<String, Object> listMachines(
            @Param(description = "机器组名称") String machineGroup,
            @Param(description = "查询开始行", defaultValue = DEFAULT_OFFSET) Integer offset,
            @Param(description = "每页行数", defaultValue = DEFAULT_PAGE_SIZE) Integer size,
            @Param(description = "系统名称") String systemName) {
        return logService.listMachines(machineGroup, offset, size, systemName);
    }

    @GetMapping("/getMachineGroup")
    @ToolMapping(description = "获取目标机器组的详细信息，辅助采集配置与资源管理。")
    public Map<String, Object> getMachineGroup(
            @Param(description = "机器组名称") String machineGroup,
            @Param(description = "系统名称") String systemName) {
        return logService.getMachineGroup(machineGroup, systemName);
    }

    @GetMapping("/getAppliedConfigs")
    @ToolMapping(description = "获取目标机器组上已应用的Logtail采集配置（内部调用），辅助采集策略分析。")
    public Map<String, Object> getAppliedConfigs(
            @Param(description = "机器组名称") String machineGroup,
            @Param(description = "系统名称") String systemName) {
        return logService.getAppliedConfigs(machineGroup, systemName);
    }

    @GetMapping("/getAppliedMachineGroups")
    @ToolMapping(description = "获取目标机器组上已应用的Logtail配置列表，辅助采集策略与资源分配分析。")
    public Map<String, Object> getAppliedMachineGroups(
            @Param(description = "机器组名称") String machineGroup,
            @Param(description = "系统名称") String systemName) {
        return logService.getAppliedMachineGroups(machineGroup, systemName);
    }

    @GetMapping("/getLogstore")
    @ToolMapping(description = "获取指定日志库(Logstore)的详细信息，辅助日志库映射与参数补全。")
    public Map<String, Object> getLogstore(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "系统名称") String systemName) {
        return logService.getLogstore(logstore, systemName);
    }
} 