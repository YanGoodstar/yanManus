package com.zixiang.yanmanus.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 为了避免ai遇到问题就调用询问工具
 * 1、询问工具只有planning Agent才可以调用 todo 添加询问策略
 * 2、工具添加结构化的参数 AskUserRequest
 * 3、将任务分类ABCD，根据任务的类型限制询问次数
 */
public class AskUserTool {

    @Tool(description = "Ask the user a question ONLY when you have exhausted all other options. " +
            "Before using this tool, you must have already tried all available tools and approaches. " +
            "Use this ONLY for truly critical missing information that prevents you from completing the task. " +
            "Excessive use of this tool will degrade the user experience.",
    returnDirect = true)
    public String askUser(
            @ToolParam(description = "The question to ask the user") String question,
            @ToolParam(description = "The reason for asking the question") String reason,
            @ToolParam(description = "The missing information needed to proceed") String missingInfo,
            @ToolParam(description = "Whether the question blocks progress until answered") Boolean blocking,
            @ToolParam(description = "The default value to use if the user does not answer") String defaultIfNotAnswered) {
        return question;
    }
}
