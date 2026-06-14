package com.zixiang.yanmanus.agent;

import com.zixiang.yanmanus.agent.model.AgentType;
import com.zixiang.yanmanus.memory.ChatSessionManager;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class AgentFactory {

    private final ToolCallback[] allToolCallbacks;
    private final ToolCallback[] yanManusToolCallbacks;
    private final ChatModel chatModel;
    private final ChatSessionManager chatSessionManager;

    public AgentFactory(
            ToolCallback[] allToolCallbacks,
            ToolCallback[] yanManusToolCallbacks,
            ChatModel dashScopeChatModel,
            ChatSessionManager chatSessionManager
    ) {
        this.allToolCallbacks = allToolCallbacks;
        this.yanManusToolCallbacks = yanManusToolCallbacks;
        this.chatModel = dashScopeChatModel;
        this.chatSessionManager = chatSessionManager;
    }

    public ToolCallAgent createAgent(
            AgentType agentType,
            Long userId,
            String sessionId
    ) {
        return switch (agentType) {
            case CHAT_AGENT -> new YanManus(
                    yanManusToolCallbacks,
                    chatModel,
                    chatSessionManager,
                    userId,
                    sessionId
            );

            case PLANNING_AGENT -> new PlanningAgent(
                    allToolCallbacks,
                    chatModel,
                    chatSessionManager,
                    userId,
                    sessionId
            );
            case ORDER_AGENT -> null;
            case KNOWLEDGE_AGENT -> null;
        };
    }
}
