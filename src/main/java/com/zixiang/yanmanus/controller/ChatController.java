package com.zixiang.yanmanus.controller;

import com.zixiang.yanmanus.agent.AgentManager;
import com.zixiang.yanmanus.agent.PlanningAgent;
import com.zixiang.yanmanus.agent.ToolCallAgent;
import com.zixiang.yanmanus.agent.YanManus;
import com.zixiang.yanmanus.agent.model.AgentState;
import com.zixiang.yanmanus.agent.model.AgentType;
import com.zixiang.yanmanus.agent.model.RunResult;
import com.zixiang.yanmanus.dto.Response;
import com.zixiang.yanmanus.memory.ChatSessionManager;
import com.zixiang.yanmanus.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Tag(name = "会话管理", description = "智能体会话接口")
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final AgentManager agentManager;
    private final ChatSessionManager sessionManager;

    public ChatController(AgentManager agentManager, ChatSessionManager sessionManager) {
        this.agentManager = agentManager;
        this.sessionManager = sessionManager;
    }

    @Operation(summary = "流式发送消息（快速响应）", description = "使用 YanManus 流式输出，仅网页搜索")
    @GetMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendStream(
            @Parameter(description = "会话ID，为空则创建新会话")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "用户消息", required = true)
            @RequestParam String message) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        YanManus yanManus = (YanManus) agentManager.getAgent(userId, sessionId, AgentType.CHAT_AGENT);
        return yanManus.runWithSse(message);
    }

    @Operation(summary = "流式发送消息（深度思考）", description = "使用 PlanningAgent 流式输出，深度思考")
    @GetMapping("/send/planning/stream")
    public SseEmitter sendPlanningStream(
            @Parameter(description = "会话ID，为空则创建新会话")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "用户消息", required = true)
            @RequestParam String message) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        PlanningAgent planningAgent = (PlanningAgent) agentManager.getAgent(userId, sessionId, AgentType.PLANNING_AGENT);
        return planningAgent.runWithSse(message);
    }

    @Operation(summary = "获取会话历史")
    @GetMapping("/session/{sessionId}/history")
    public Response<Map<String, Object>> getHistory(@PathVariable String sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Message> messages = sessionManager.getMessages(userId, sessionId);
        List<Map<String, String>> history = messages.stream()
                .map(msg -> Map.of(
                        "type", msg.getClass().getSimpleName(),
                        "content", msg.getText() != null ? msg.getText() : ""
                ))
                .toList();
        return Response.ok(Map.of(
                "sessionId", sessionId,
                "messageCount", history.size(),
                "messages", history
        ));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/session/{sessionId}")
    public Response<Void> deleteSession(@PathVariable String sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        sessionManager.clearSession(userId, sessionId);
        return Response.ok();
    }
}
