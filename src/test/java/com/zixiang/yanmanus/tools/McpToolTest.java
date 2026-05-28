package com.zixiang.yanmanus.tools;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpToolTest {

    @Autowired
    ToolCallbackProvider mcpToolCallbacks;

    @Test
    void mcpToolsLoaded() {
        ToolCallback[] tools = mcpToolCallbacks.getToolCallbacks();
        System.out.println("MCP tools count: " + tools.length);
        for (ToolCallback tool : tools) {
            System.out.println("Tool: " + tool.getToolDefinition().name());
        }
        assertNotEquals(0, tools.length, "MCP tools should not be empty");
    }
}
