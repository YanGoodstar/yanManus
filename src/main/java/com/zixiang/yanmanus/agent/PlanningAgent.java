package com.zixiang.yanmanus.agent;

import com.zixiang.yanmanus.agent.model.TaskLevel;
import com.zixiang.yanmanus.memory.ChatSessionManager;
import com.zixiang.yanmanus.prompt.PromptRenderer;
import com.zixiang.yanmanus.prompt.PlanningPrompts;
import com.zixiang.yanmanus.prompt.PromptRenderer;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Component
public class PlanningAgent extends ToolCallAgent {

    private final ChatSessionManager chatSessionManager;

    public PlanningAgent(@Qualifier("allToolCallbacks") ToolCallback[] availableTools, ChatModel dashScopeChatModel, ChatSessionManager chatSessionManager) {
        super(availableTools);
        this.chatSessionManager = chatSessionManager;
        this.setTaskLevel(TaskLevel.COMPLEX);
        this.setSystemPrompt(PlanningPrompts.SYSTEM_PROMPT);
        this.setAgentName("PlanningAgent");
        this.setNextStepPrompt(PlanningPrompts.NEXT_STEP_PROMPT);
        this.setMaxStep(30);
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors()
                .build();
        setChatClient(chatClient);
    }

}
