package client;

import org.junit.jupiter.api.Test;
import org.noear.solon.ai.mcp.client.McpClientProvider;
import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.test.SolonTest;
import com.anker.sls.SolonAIMcpSlsServer;

import java.util.List;

@SolonTest(SolonAIMcpSlsServer.class)
public class SlsPromptTest {
    
    @Test
    public void testGetSlsPrompt() throws Exception {
        McpClientProvider toolProvider = McpClientProvider.builder()
                .apiUrl("http://localhost:9080/mcp/sse/slslog")
                .build();

        try {
            // 测试获取提示词
            List<ChatMessage> prompt = toolProvider.getPromptAsMessages(
                "getSlsPrompt",
                null
            );
            
            System.out.println("=== SLS 提示词测试结果 ===");
            System.out.println("提示词数量: " + prompt.size());
            
            for (int i = 0; i < prompt.size(); i++) {
                ChatMessage msg = prompt.get(i);
                System.out.println("消息 " + (i+1) + " [" + msg.getRole() + "]: ");
                System.out.println(msg.getContent());
                System.out.println("---");
            }
            
        } catch (Exception e) {
            System.err.println("获取提示词失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test 
    public void testGetSlsPromptAsTool() throws Exception {
        McpClientProvider toolProvider = McpClientProvider.builder()
                .apiUrl("http://localhost:9080/mcp/sse/slslog")
                .build();

        try {
            // 测试作为工具调用
            Object result = toolProvider.callTool(
                "getSlsPrompt", 
                null
            ).getContent();
            
            System.out.println("=== SLS 提示词工具调用结果 ===");
            System.out.println(result);
            
        } catch (Exception e) {
            System.err.println("工具调用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 