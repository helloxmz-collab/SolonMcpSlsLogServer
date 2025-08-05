package com.anker.sls.service;

import java.util.List;
import java.util.Map;

/**
 * SLS服务接口
 */
public interface SLSService {
    /**
     * 获取日志库列表
     */
    List<Map<String, Object>> getLogstoreList();
    
    /**
     * 查询日志
     */
    Map<String, Object> getLogs(String logstore, String query, int from, int to, int line, boolean reverse);
    
    /**
     * 获取索引配置
     */
    Map<String, Object> getIndex(String logstore);
    
    /**
     * 获取项目信息
     */
    Map<String, Object> getProject();
    
    /**
     * 获取直方图数据
     */
    Map<String, Object> getHistograms(String logstore, long from, long to, String topic, String query);
    
    /**
     * 诊断SLS连接问题
     */
    Map<String, Object> diagnoseSLSConnection(String logstore);

    /**
     * 获取Logstore详细信息
     * @param logstore Logstore名称
     * @return Logstore详细信息
     */
    Map<String, Object> getLogstore(String logstore);

    /**
     * 列出Project信息
     * @param projectName 可选，项目名称
     * @param offset 可选，分页起始
     * @param size 可选，分页大小
     * @param resourceGroupId 可选，资源组ID
     * @return Project列表及分页信息
     */
    Map<String, Object> listProject(String projectName, Integer offset, Integer size, String resourceGroupId);

    /**
     * 通过SQL语句查询日志
     * @param query SQL查询语句
     * @param powerSql 是否使用SQL独享版
     * @return 查询结果
     */
    Map<String, Object> queryLogsBySql(String query, Boolean powerSql);

    /**
     * 获取服务日志信息
     * @return 服务日志信息
     */
    Map<String, Object> getLogging();

    /**
     * 获取Logstore下所有Shard信息
     * @param logstore Logstore名称
     * @return Shard列表
     */
    List<Map<String, Object>> listShards(String logstore);

    /**
     * 查询Logstore日志数据
     * @param logstore Logstore名称
     * @param from 查询开始时间（Unix时间戳，秒）
     * @param to 查询结束时间（Unix时间戳，秒）
     * @param query 查询语句（可选）
     * @param line 返回行数（可选）
     * @param offset 查询起始行（可选）
     * @param reverse 是否倒序（可选）
     * @param powerSql 是否使用SQL独享版（可选）
     * @param topic 日志主题（可选）
     * @return 查询结果
     */
    Map<String, Object> queryLogstoreLogs(
        String logstore,
        int from,
        int to,
        String query,
        Integer line,
        Integer offset,
        Boolean reverse,
        Boolean powerSql,
        String topic
    );

    /**
     * 获取指定Shard的Cursor
     * @param logstore Logstore名称
     * @param shardId Shard ID
     * @param from 时间点（Unix时间戳字符串或"begin"/"end"）
     * @return Cursor信息
     */
    Map<String, Object> getCursor(String logstore, int shardId, String from);

    /**
     * 查询日志上下文（GetContextLogs）
     * @param logstore Logstore名称
     * @param backLines 向前（上文）查询的日志条数
     * @param forwardLines 向后（下文）查询的日志条数
     * @return 上下文日志查询结果
     */
    Map<String, Object> getContextLogs(String logstore, int backLines, int forwardLines);

    /**
     * 根据Cursor获取服务端时间
     * @param logstore Logstore名称
     * @param shardId Shard ID
     * @param cursor 游标
     * @return 包含cursor_time的结果
     */
    Map<String, Object> getCursorTime(String logstore, int shardId, String cursor);

    /**
     * 查询Logstore原始日志数据（POST /logstores/{logstore}/logs）
     * @param logstore Logstore名称
     * @param acceptEncoding 压缩方式（如 lz4、gzip）
     * @param body 查询参数（from、to、query、line、offset、reverse、powerSql、session、topic、forward、highlight等）
     * @return 查询结果
     */
    Map<String, Object> getRawLogs(String logstore, String acceptEncoding, Map<String, Object> body);

    /**
     * 获取Logstore计量模式
     * @param logstore Logstore名称
     * @return 计量模式信息
     */
    Map<String, Object> getLogstoreMeteringMode(String logstore);

    /**
     * 列出目标Project下的机器组
     * @param offset 查询开始行（可选）
     * @param size 每页行数（可选）
     * @param groupName 机器组名称（可选，支持部分匹配）
     * @return 机器组列表及分页信息
     */
    Map<String, Object> listMachineGroups(Integer offset, Integer size, String groupName);

    /**
     * 列出目标机器组中与日志服务连接正常的机器列表
     * @param machineGroup 机器组名称
     * @param offset 查询开始行（可选）
     * @param size 每页行数（可选）
     * @return 机器信息列表及分页信息
     */
    Map<String, Object> listMachines(String machineGroup, Integer offset, Integer size);

    /**
     * 获取目标机器组的详细信息
     * @param machineGroup 机器组名称
     * @return 机器组详细信息
     */
    Map<String, Object> getMachineGroup(String machineGroup);

    /**
     * 获取目标机器组上已应用的Logtail配置列表
     * @param machineGroup 机器组名称
     * @return 配置列表及数量
     */
    Map<String, Object> getAppliedConfigs(String machineGroup);

    /**
     * 获取已绑定指定Logtail配置的机器组列表
     * @param configName Logtail配置名称
     * @return 机器组列表及数量
     */
    Map<String, Object> getAppliedMachineGroups(String configName);
}