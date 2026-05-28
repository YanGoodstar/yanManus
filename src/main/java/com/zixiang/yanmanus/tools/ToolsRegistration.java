package com.zixiang.yanmanus.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ToolsRegistration {

    @Value("${serpapi.api-key:}")
    private String serpApiKey;

    private final ToolCallbackProvider mcpToolCallbacks;

    public ToolsRegistration(ToolCallbackProvider mcpToolCallbacks) {
        this.mcpToolCallbacks = mcpToolCallbacks;
    }

    @Bean
    public ToolCallback[] toolCallbacks() {
        // 内置工具
        TerminateTool terminateTool = new TerminateTool();
        FileReadTool fileReadTool = new FileReadTool();
        FileWriteTool fileWriteTool = new FileWriteTool();
        BashTool bashTool = new BashTool();
        WebSearchTool webSearchTool = new WebSearchTool(serpApiKey);

        List<ToolCallback> allTools = new ArrayList<>(List.of(ToolCallbacks.from(terminateTool, fileReadTool, fileWriteTool, bashTool, webSearchTool)));

        // MCP 工具（地图等）
        ToolCallback[] mcpTools = mcpToolCallbacks.getToolCallbacks();
        allTools.addAll(List.of(mcpTools));

        return allTools.toArray(new ToolCallback[0]);
    }
}
