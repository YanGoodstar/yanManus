package com.zixiang.yanmanus.agent;

import com.zixiang.yanmanus.agent.model.AgentType;

public record AgentKey(Long userId, String sessionId, AgentType agentType) {
}