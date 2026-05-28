package com.zixiang.yanmanus.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author yan
 * @create 2026-05-26-20:10
 */
@Component
public class ToolsRegistration {

    @Bean
    public ToolCallback[] getToolCallbacks() {
        TerminateTool terminateTool = new TerminateTool();
        FileReadTool fileReadTool = new FileReadTool();
        FileWriteTool fileWriteTool = new FileWriteTool();
        BashTool bashTool = new BashTool();
        return ToolCallbacks.from(terminateTool, fileReadTool, fileWriteTool, bashTool);
    }
}
