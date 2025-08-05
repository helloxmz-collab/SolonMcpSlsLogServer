package com.anker.sls.service.impl;

import org.springframework.stereotype.Service;
import com.anker.sls.service.SLSService;

import java.util.*;

/**
 * 模拟SLS服务实现，用于测试
 */
@Service("mockSLSServiceImpl")
public class MockSLSServiceImpl implements SLSService {

    @Override
    public List<Map<String, Object>> getLogstoreList() {
        List<Map<String, Object>> mockLogstores = new ArrayList<>();
        Map<String, Object> logstore1 = new HashMap<>();
        logstore1.put("logstoreName", "app-logs");
        mockLogstores.add(logstore1);

        Map<String, Object> logstore2 = new HashMap<>();
        logstore2.put("logstoreName", "system-logs");
        mockLogstores.add(logstore2);
        
        return mockLogstores;
    }

    @Override
    public Map<String, Object> getLogs(String logstore, String query, int from, int to, int line, boolean reverse) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> logs = new ArrayList<>();
        
        Map<String, Object> log1 = new HashMap<>();
        log1.put("__time__", System.currentTimeMillis() / 1000);
        log1.put("level", "INFO");
        log1.put("message", "This is a mock log message");
        logs.add(log1);
        
        result.put("count", logs.size());
        result.put("logs", logs);
        result.put("progress", "Complete");
        
        return result;
    }

    @Override
    public Map<String, Object> getIndex(String logstore) {
        Map<String, Object> mockIndex = new HashMap<>();
        Map<String, Object> keys = new HashMap<>();
        
        Map<String, Object> level = new HashMap<>();
        level.put("type", "text");
        level.put("token", new String[]{"\\s+"});
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "text");
        message.put("token", new String[]{"\\s+"});
        
        keys.put("level", level);
        keys.put("message", message);
        
        mockIndex.put("keys", keys);
        mockIndex.put("ttl", 7);
        mockIndex.put("lastModifyTime", System.currentTimeMillis() / 1000);
        
        return mockIndex;
    }

    @Override
    public Map<String, Object> getProject() {
        Map<String, Object> mockProject = new HashMap<>();
        mockProject.put("projectName", "mock-project");
        mockProject.put("region", "cn-hangzhou");
        mockProject.put("owner", "user");
        mockProject.put("description", "Mock project for testing");
        mockProject.put("createTime", System.currentTimeMillis() / 1000);
        mockProject.put("lastModifyTime", System.currentTimeMillis() / 1000);
        
        return mockProject;
    }

    @Override
    public Map<String, Object> getAppliedMachineGroups(String configName) {
        Map<String, Object> result = new HashMap<>();
        result.put("configName", configName);
        result.put("machineGroups", Arrays.asList("groupA", "groupB"));
        result.put("count", 2);
        return result;
    }

    @Override
    public Map<String, Object> diagnoseSLSConnection(String logstore) {
        Map<String, Object> result = new HashMap<>();
        result.put("configValid", true);
        result.put("connectionValid", true);
        result.put("message", "SLS连接正常（模拟数据）");
        result.put("logstores", getLogstoreList());
        result.put("logstoreCount", getLogstoreList().size());
        
        if (logstore != null && !logstore.isEmpty()) {
            result.put("logstoreTestResult", getIndex(logstore));
        } else {
            result.put("projectTestResult", getProject());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> listProject(String projectName, Integer offset, Integer size, String resourceGroupId) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getHistograms(String logstore, long from, long to, String topic, String query) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> histograms = new ArrayList<>();
        long interval = (to - from) / 10;
        for (int i = 0; i < 10; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("from", from + i * interval);
            point.put("to", from + (i + 1) * interval);
            point.put("count", (int)(Math.random() * 100));
            histograms.add(point);
        }
        result.put("histograms", histograms);
        result.put("progress", "Complete");
        result.put("count", histograms.stream().mapToInt(h -> (int)h.get("count")).sum());
        return result;
    }

    @Override
    public Map<String, Object> getRawLogs(String logstore, String acceptEncoding, Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("logstore", logstore);
        result.put("acceptEncoding", acceptEncoding);
        result.put("body", body);
        result.put("data", Arrays.asList("raw log 1", "raw log 2"));
        return result;
    }

    @Override
    public Map<String, Object> getContextLogs(String logstore, int backLines, int forwardLines) {
        Map<String, Object> result = new HashMap<>();
        result.put("logstore", logstore);
        result.put("backLines", backLines);
        result.put("forwardLines", forwardLines);
        result.put("logs", Arrays.asList("context log 1", "context log 2"));
        result.put("note", "packId/packMeta fetched internally in real impl");
        return result;
    }

    @Override
    public Map<String, Object> queryLogstoreLogs(String logstore, int from, int to, String query, Integer line, Integer offset, Boolean reverse, Boolean powerSql, String topic) {
        Map<String, Object> result = new HashMap<>();
        result.put("logstore", logstore);
        result.put("from", from);
        result.put("to", to);
        result.put("query", query);
        result.put("line", line);
        result.put("offset", offset);
        result.put("reverse", reverse);
        result.put("powerSql", powerSql);
        result.put("topic", topic);
        result.put("data", Arrays.asList("log1", "log2"));
        return result;
    }

    @Override
    public Map<String, Object> getAppliedConfigs(String machineGroup) {
        Map<String, Object> result = new HashMap<>();
        result.put("machineGroup", machineGroup);
        result.put("configs", Arrays.asList("configA", "configB"));
        return result;
    }

    @Override
    public List<Map<String, Object>> listShards(String logstore) {
        List<Map<String, Object>> shards = new ArrayList<>();
        Map<String, Object> shard = new HashMap<>();
        shard.put("shardId", 0);
        shard.put("status", "active");
        shards.add(shard);
        return shards;
    }

    @Override
    public Map<String, Object> getCursorTime(String logstore, int shardId, String cursor) {
        Map<String, Object> result = new HashMap<>();
        result.put("logstore", logstore);
        result.put("shardId", shardId);
        result.put("cursor", cursor);
        result.put("cursor_time", System.currentTimeMillis() / 1000);
        return result;
    }

    @Override
    public Map<String, Object> getCursor(String logstore, int shardId, String from) {
        Map<String, Object> result = new HashMap<>();
        result.put("logstore", logstore);
        result.put("shardId", shardId);
        result.put("from", from);
        result.put("cursor", "mock-cursor");
        return result;
    }

    @Override
    public Map<String, Object> getLogstoreMeteringMode(String logstore) {
        Map<String, Object> result = new HashMap<>();
        result.put("logstore", logstore);
        result.put("meteringMode", "paybytraffic");
        return result;
    }

    @Override
    public Map<String, Object> listMachineGroups(Integer offset, Integer size, String groupName) {
        Map<String, Object> result = new HashMap<>();
        result.put("offset", offset);
        result.put("size", size);
        result.put("groupName", groupName);
        result.put("groups", Arrays.asList("groupA", "groupB"));
        return result;
    }

    @Override
    public Map<String, Object> listMachines(String machineGroup, Integer offset, Integer size) {
        Map<String, Object> result = new HashMap<>();
        result.put("machineGroup", machineGroup);
        result.put("offset", offset);
        result.put("size", size);
        result.put("machines", Arrays.asList("machine1", "machine2"));
        return result;
    }

    @Override
    public Map<String, Object> getLogstore(String logstore) {
        Map<String, Object> result = new HashMap<>();
        result.put("logstore", logstore);
        result.put("detail", "mock logstore detail");
        return result;
    }

    @Override
    public Map<String, Object> queryLogsBySql(String query, Boolean powerSql) {
        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("powerSql", powerSql);
        result.put("data", Arrays.asList("sql log 1", "sql log 2"));
        return result;
    }

    @Override
    public Map<String, Object> getLogging() {
        Map<String, Object> result = new HashMap<>();
        result.put("logging", Arrays.asList("logA", "logB"));
        return result;
    }

    @Override
    public Map<String, Object> getMachineGroup(String machineGroup) {
        Map<String, Object> result = new HashMap<>();
        result.put("machineGroup", machineGroup);
        result.put("detail", "mock machine group detail");
        return result;
    }
} 