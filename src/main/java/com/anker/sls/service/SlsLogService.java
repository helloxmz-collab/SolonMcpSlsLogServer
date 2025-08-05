package com.anker.sls.service;

import java.util.List;
import java.util.Map;

import org.noear.solon.ai.chat.message.ChatMessage;

import com.anker.sls.model.McpServiceLog;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Collection;

public interface SlsLogService {
    List<Map<String, Object>> getLogstoreList(String systemName);
    Map<String, Object> getLogs(String logstore, String query, int from, int to, int line, boolean reverse, String systemName);
    Map<String, Object> getIndex(String logstore, String systemName);
    Map<String, Object> getProject(String systemName);
    Map<String, Object> getHistograms(String logstore, long from, long to, String topic, String query, String systemName);
    Map<String, Object> diagnoseSLSConnection(String logstore, String systemName);
    Map<String, Object> getLogstore(String logstore, String systemName);
    Map<String, Object> listProject(String projectName, Integer offset, Integer size, String resourceGroupId, String systemName);
    Map<String, Object> queryLogsBySql(String query, Boolean powerSql, String systemName);
    Map<String, Object> getLogging(String systemName);
    List<Map<String, Object>> listShards(String logstore, String systemName);
    Map<String, Object> getLogsPro(String logstore, Long from, Long to, String query, Integer line, Integer offset, Boolean reverse, Boolean powerSql, String topic, String systemName);
    Map<String, Object> getCursor(String logstore, int shardId, String from, String systemName);
    Map<String, Object> getContextLogs(String logstore, String packId, String packMeta, int backLines, int forwardLines, String systemName);
    Map<String, Object> getCursorTime(String logstore, int shardId, String cursor, String systemName);
    Map<String, Object> getRawLogs(String logstore, String acceptEncoding, Map<String, Object> body, String systemName);
    Map<String, Object> getLogstoreMeteringMode(String logstore, String systemName);
    Map<String, Object> listMachineGroups(Integer offset, Integer size, String groupName, String systemName);
    Map<String, Object> listMachines(String machineGroup, Integer offset, Integer size, String systemName);
    Map<String, Object> getMachineGroup(String machineGroup, String systemName);
    Map<String, Object> getAppliedConfigs(String machineGroup, String systemName);
    Map<String, Object> getAppliedMachineGroups(String configName, String systemName);
    IPage<McpServiceLog> getMcpServiceLog(Integer page, Integer size);
} 