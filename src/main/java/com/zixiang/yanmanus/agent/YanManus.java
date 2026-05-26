package com.zixiang.yanmanus.agent;

import com.zixiang.yanmanus.prompt.AgentPrompts;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * @author yan
 * @create 2026-05-26-20:13
 */
@Component
public class YanManus extends ToolCallAgent{
    public YanManus(ToolCallback[] availableTools, ChatModel dashScopeChatModel) {
        super(availableTools);

        this.setSystemPrompt(AgentPrompts.SYSTEM_PROMPT);
        this.setNextStepPrompt(AgentPrompts.NEXT_STEP_PROMPT);
        this.setMaxStep(20);
        //初始化客户端
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors()
                .build();
        setChatClient(chatClient);
    }

}
