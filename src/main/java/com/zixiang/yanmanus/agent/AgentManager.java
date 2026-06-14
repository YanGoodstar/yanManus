package com.zixiang.yanmanus.agent;

import com.zixiang.yanmanus.agent.model.AgentType;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AgentManager {

    private final ConcurrentMap<AgentKey, ToolCallAgent> agents = new ConcurrentHashMap<>();

    private final AgentFactory agentFactory;

    public AgentManager(AgentFactory agentFactory) {
        this.agentFactory = agentFactory;
    }

    public ToolCallAgent getAgent(
            Long userId,
            String sessionId,
            AgentType agentType
    ) {
        AgentKey key = new AgentKey(userId, sessionId, agentType);

        return agents.computeIfAbsent(key, k ->
                agentFactory.createAgent(
                        k.agentType(),
                        k.userId(),
                        k.sessionId()
                )
        );
    }

    public void removeAgent(
            Long userId,
            String sessionId,
            AgentType agentType
    ) {
        agents.remove(new AgentKey(userId, sessionId, agentType));
    }
}
