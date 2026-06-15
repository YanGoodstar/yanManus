package com.zixiang.yanmanus.agent;

import com.zixiang.yanmanus.agent.model.TaskLevel;
import com.zixiang.yanmanus.memory.ChatSessionManager;
import com.zixiang.yanmanus.prompt.AgentPrompts;
import com.zixiang.yanmanus.prompt.PromptRenderer;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author yan
 * @create 2026-05-26-20:13
 */
@Getter
public class YanManus extends ToolCallAgent{

    public YanManus(ToolCallback[] availableTools,
                    ChatModel dashScopeChatModel,
                    ChatSessionManager chatSessionManager,
                    Long userId,
                    String sessionId) {
        super(availableTools, userId, sessionId);
        setChatSessionManager(chatSessionManager);
        this.setTaskLevel(TaskLevel.MODERATE);
        this.setSystemPrompt(AgentPrompts.YAN_MANUS_PROMPT);
        this.setAgentName("YanManus");
        this.setNextStepPrompt(AgentPrompts.YAN_MANUS_NEXT_PROMPT);
        this.setMaxStep(20);
        //初始化客户端
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors()
                .build();
        setChatClient(chatClient);
    }

}
