package com.zixiang.yanmanus.agent;

import com.zixiang.yanmanus.agent.model.TaskLevel;
import com.zixiang.yanmanus.prompt.AgentPrompts;
import com.zixiang.yanmanus.prompt.PromptRenderer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author yan
 * @create 2026-05-26-20:13
 */
@Component
public class YanManus extends ToolCallAgent{

    public YanManus(ToolCallback[] availableTools, ChatModel dashScopeChatModel) {
        super(availableTools);
        this.setTaskLevel(TaskLevel.MODERATE);
        String systemPrompt =
                PromptRenderer.render(AgentPrompts.SYSTEM_PROMPT, Map.of(
                        "agentName", "YanManus",
                        "capabilities", "文件读写、网页搜索、命令行执行",
                        "taskLevel", this.getTaskLevel().getDescription(),
                        "maxAskCount", String.valueOf(this.getTaskLevel().getMaxAskCount()),
                        "taskLevelGuidance", this.getTaskLevel().getGuidance()
                ));
        this.setSystemPrompt(systemPrompt);
        this.setAgentName("YanManus");
        this.setNextStepPrompt(AgentPrompts.NEXT_STEP_PROMPT);
        this.setMaxStep(20);
        //初始化客户端
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors()
                .build();
        setChatClient(chatClient);
    }

}
