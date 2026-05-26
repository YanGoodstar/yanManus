package com.zixiang.yanmanus.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * @author yan
 * @create 2026-05-26-20:04
 */
public class TerminateTool {

    @Tool(description = """  
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.  
            "When you have finished all the tasks, call this tool to end the work.  
            """)
    public String doTerminate() {
        return "任务结束";
    }
}
