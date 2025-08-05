
# Cursor 接入 MCP 服务完整教程（基于 solon-ai-mcp）

本教程将详细介绍如何搭建基于 solon-ai-mcp 的 MCP（Model Context Protocol）服务器，并配置 Cursor 进行对接。以本项目为实际案例，提供完整的实现步骤。

## 📋 项目简介

本项目是一个基于 **Spring Boot + Solon AI MCP** 框架的 MCP 服务器，主要提供：
- 🔍 阿里云 SLS 日志查询服务
- 🛠️ 通用工具函数（计算器、字符串处理等）
- 🌤️ 天气查询服务  
- 📝 提示词管理
- 📊 资源管理

## 🚀 一、项目搭建与配置

### 1.1 依赖配置

在 `pom.xml` 中添加关键依赖：

```xml
<properties>
    <solon.version>3.4.0</solon.version>
    <aliyun-sls-sdk.version>1.4.0</aliyun-sls-sdk.version>
</properties>

<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>2.7.18</version>
    </dependency>
    
    <!-- Spring AOP -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
        <version>2.7.18</version>
    </dependency>
    
    <!-- Solon AI MCP (核心依赖) -->
    <dependency>
        <groupId>org.noear</groupId>
        <artifactId>solon-ai-mcp</artifactId>
        <version>${solon.version}</version>
    </dependency>
    
    <!-- 工具库 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.16</version>
    </dependency>
    
    <!-- 阿里云SLS SDK（可选，用于日志查询） -->
    <dependency>
        <groupId>com.aliyun</groupId>
        <artifactId>sls20201230</artifactId>
        <version>${aliyun-sls-sdk.version}</version>
    </dependency>
</dependencies>
```

### 1.2 应用配置

**`src/main/resources/application.yml`**：
```yaml
server:
  port: 9080

spring:
  application:
    name: sls-mcp-server

# 阿里云SLS配置（可选）
aliyun:
  sls:
    access-key-id: YOUR_ACCESS_KEY_ID
    access-key-secret: YOUR_ACCESS_KEY_SECRET
    sls-prompts:
      - keyword: 广告
        endpoint: cn-beijing.log.aliyuncs.com
        project: ads-sls
        logstore: ads-center,ads-api,ads-intelligent

# 日志配置
logging:
  level:
    root: INFO
    com.anker.sls: DEBUG
```

**`src/main/resources/mcpserver.yml`**：
```yaml
server:
  port: 9080

# MCP服务配置（可选配置项）
#mcp:
#  server:
#    enabled: true
#    timeout: 30000
#    max-connections: 100
```

### 1.3 主启动类

```java
package com.anker.sls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SolonAIMcpSlsServer {
    public static void main(String[] args) {
        SpringApplication.run(SolonAIMcpSlsServer.class, args);
    }
}
```

### 1.4 MCP 服务配置

**`McpServerConfig.java`**：
```java
@Slf4j
@Configuration
public class McpServerConfig {
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @PostConstruct
    public void start() {
        log.info("启动 Solon MCP 服务");
        System.setProperty("server.contextPath", contextPath);
        Solon.start(McpServerConfig.class, new String[]{"--cfg=mcpserver.yml"});
    }

    @Bean
    public FilterRegistrationBean mcpServerFilter() {
        FilterRegistrationBean<SolonServletFilter> filter = new FilterRegistrationBean<>();
        filter.setName("SolonFilter");
        filter.addUrlPatterns("/mcp/*");
        filter.setFilter(new SolonServletFilter());
        return filter;
    }
}
```

## 🛠️ 二、创建 MCP 工具

### 2.1 基础工具示例

```java
@Controller
@McpServerEndpoint(version = "1.0", sseEndpoint = "/mcp/sse/tool")
public class ToolController {
    
    @ToolMapping(description = "加法计算器，返回两个数字的和")
    public int add(@Param(description = "第一个数字") int a, 
                   @Param(description = "第二个数字") int b) {
        return a + b;
    }

    @ToolMapping(description = "字符串反转，返回反转后的字符串")
    public String reverseString(@Param(description = "要反转的字符串") String input) {
        return new StringBuilder(input).reverse().toString();
    }

    @ToolMapping(description = "获取当前时间，返回时间字符串")
    public String getCurrentTime() {
        return java.time.LocalDateTime.now().toString();
    }
}
```

### 2.2 日志查询工具示例

```java
@Controller
@McpServerEndpoint(version = "1.0", sseEndpoint = "/mcp/sse/slslog")
public class SlsLogController {
    
    @Autowired
    private SlsLogService logService;

    @ToolMapping(description = "获取Logstore列表")
    public List<Map<String, Object>> getLogstoreList(
            @Param(description = "系统名称") String systemName) {
        return logService.getLogstoreList(systemName);
    }

    @ToolMapping(description = "查询日志，返回分析结果")
    public Map<String, Object> getLogsPro(
            @Param(description = "日志库名称") String logstore,
            @Param(description = "起始时间（格式：yyyy-MM-dd HH:mm:ss）") String from,
            @Param(description = "结束时间（格式：yyyy-MM-dd HH:mm:ss）") String to,
            @Param(description = "查询条件") String query,
            @Param(description = "返回行数", defaultValue = "100") Integer line,
            @Param(description = "偏移量", defaultValue = "0") Integer offset) {
        return logService.getLogsPro(logstore, from, to, query, line, offset, false, false, "", "");
    }
}
```

### 2.3 提示词和资源管理

```java
@Controller
@McpServerEndpoint(version = "1.0", sseEndpoint = "/mcp/sse/prompt")
public class PromptController {
    
    @PromptMapping(description = "生成关于某个主题的提问")
    public List<ChatPrompt> askQuestion(@Param(description = "主题") String topic) {
        return Arrays.asList(
            ChatPrompt.of(ChatMessage.ofSystem("请解释一下'" + topic + "'的概念？"))
        );
    }
}

@Controller
@McpServerEndpoint(version = "1.0", sseEndpoint = "/mcp/sse/resource")
public class ResourceController {
    
    @ResourceMapping(uri = "config://app-version", description = "获取应用版本号")
    public String getAppVersion() {
        return "v3.2.0";
    }

    @ResourceMapping(uri = "db://users/{user_id}/email", description = "根据用户ID查询邮箱")
    public String getEmail(@Param(description = "用户Id") @PathVariable String user_id) {
        return user_id + "@example.com";
    }
}
```

## 🔌 三、Cursor 配置接入

### 3.1 定位配置文件

Cursor 的 MCP 配置文件路径：
- **macOS**: `~/.cursor/mcp.json`
- **Windows**: `%APPDATA%\Cursor\mcp.json`
- **Linux**: `~/.cursor/mcp.json`

如果文件不存在，请手动创建。

### 3.2 配置 MCP 服务器

编辑 `mcp.json` 文件：

```json
{
  "mcpServers": {
    "solon-ai-mcp-sls": {
      "url": "http://localhost:9080/mcp/sse/slslog",
      "description": "阿里云SLS日志查询服务"
    },
    "solon-ai-mcp-tools": {
      "url": "http://localhost:9080/mcp/sse/tool", 
      "description": "通用工具函数"
    },
    "solon-ai-mcp-weather": {
      "url": "http://localhost:9080/mcp/sse/weather",
      "description": "天气查询服务"
    }
  }
}
```

### 3.3 多端点配置说明

本项目支持多个 MCP 端点：

| 端点路径 | 功能说明 | 主要工具 |
|---------|---------|----------|
| `/mcp/sse/slslog` | SLS日志查询 | getLogstoreList, getLogsPro, getLogsByUniqueId |
| `/mcp/sse/tool` | 通用工具 | add, reverseString, getCurrentTime, isEven |
| `/mcp/sse/weather` | 天气查询 | getWeather |
| `/mcp/sse/prompt` | 提示词管理 | askQuestion |
| `/mcp/sse/resource` | 资源管理 | getAppVersion, getEmail |

## 🚀 四、启动与测试

### 4.1 启动服务

```bash
# 方式1：直接运行主类
java -jar solon-ai-mcp-sls-demo.jar

# 方式2：Maven 启动
mvn spring-boot:run

# 方式3：IDEA 运行
# 直接运行 SolonAIMcpSlsServer.main() 方法
```

服务启动后会在 **9080** 端口监听。

### 4.2 验证服务

使用 curl 测试 MCP 端点：

```bash
# 测试工具端点
curl "http://localhost:9080/mcp/sse/tool"

# 测试日志端点 
curl "http://localhost:9080/mcp/sse/slslog"

# 测试天气端点
curl "http://localhost:9080/mcp/sse/weather"
```

### 4.3 Cursor 中使用

1. **重启 Cursor** 以加载新配置
2. **新建对话**，选择配置的 MCP 服务器
3. **测试工具调用**：
   ```
   用户：帮我计算 123 + 456
   用户：请反转字符串 "Hello World"
   用户：查询广告系统的错误日志
   用户：杭州今天天气怎么样？
   ```

## 🔧 五、高级配置

### 5.1 认证配置

添加简单的认证机制：

```java
@Component
public class McpServerAuth implements Filter {
    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        String authStr = ctx.param("user");
        boolean authSuccess = !"no".equals(authStr);
        
        if (!authSuccess) {
            ctx.status(401);
            ctx.setHandled(true);
            return;
        }
        chain.doFilter(ctx);
    }
}
```

在 `mcp.json` 中添加认证参数：
```json
{
  "mcpServers": {
    "solon-ai-mcp-sls": {
      "url": "http://localhost:9080/mcp/sse/slslog?user=admin"
    }
  }
}
```

### 5.2 日志配置

在 `application.yml` 中配置详细日志：

```yaml
logging:
  level:
    root: INFO
    com.anker.sls: DEBUG
    org.noear.solon: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 5.3 性能优化

```yaml
# mcpserver.yml
mcp:
  server:
    enabled: true
    timeout: 30000
    max-connections: 100
    thread-pool-size: 10
```

## 📚 六、扩展开发

### 6.1 添加新工具

1. **创建新的 Controller**：
```java
@Controller
@McpServerEndpoint(version = "1.0", sseEndpoint = "/mcp/sse/custom")
public class CustomController {
    
    @ToolMapping(description = "自定义工具描述")
    public String customTool(@Param(description = "参数描述") String param) {
        // 工具实现逻辑
        return "result";
    }
}
```

2. **更新 mcp.json**：
```json
{
  "mcpServers": {
    "custom-tools": {
      "url": "http://localhost:9080/mcp/sse/custom"
    }
  }
}
```

### 6.2 集成外部服务

参考项目中的 SLS 集成方式：

1. 添加相关 SDK 依赖
2. 配置服务参数
3. 实现服务接口
4. 创建 MCP 工具封装

## ❗ 七、常见问题

### 7.1 连接问题

**问题**：Cursor 无法连接到 MCP 服务
**解决**：
- 检查端口是否正确（9080）
- 确认服务是否正常启动
- 检查防火墙设置
- 验证 URL 路径是否正确

### 7.2 工具不显示

**问题**：Cursor 中看不到自定义工具
**解决**：
- 确认 `@ToolMapping` 注解正确
- 检查 `@McpServerEndpoint` 配置
- 重启 Cursor 重新加载配置
- 查看服务端日志确认工具注册成功

### 7.3 参数传递问题

**问题**：工具调用时参数错误
**解决**：
- 使用 `@Param` 注解明确参数描述
- 添加编译参数 `-parameters`
- 检查参数类型是否匹配

### 7.4 中文乱码

**问题**：返回结果中文显示异常
**解决**：
```yaml
server:
  servlet:
    encoding:
      charset: UTF-8
      force: true
```

## 🎯 八、最佳实践

1. **工具设计**：
   - 保持工具功能单一、明确
   - 提供详细的参数和功能描述
   - 返回结构化的 JSON 数据

2. **错误处理**：
   - 使用统一的异常处理机制
   - 返回有意义的错误信息
   - 记录详细的操作日志

3. **性能优化**：
   - 合理设置超时时间
   - 使用连接池管理资源
   - 实现缓存机制

4. **安全考虑**：
   - 实现适当的认证机制
   - 验证输入参数
   - 限制访问频率

---

## 📞 技术支持

如遇问题，可以：
- 查看项目日志文件
- 参考 [Solon 官方文档](https://solon.noear.org)
- 检查 [MCP 协议规范](https://modelcontextprotocol.io)

项目示例完整展示了从基础配置到高级功能的完整实现，可直接参考使用。