package com.zixiang.yanmanus.controller;

import com.zixiang.yanmanus.agent.PlanningAgent;
import com.zixiang.yanmanus.agent.YanManus;
import com.zixiang.yanmanus.agent.model.AgentState;
import com.zixiang.yanmanus.agent.model.RunResult;
import com.zixiang.yanmanus.memory.ChatSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "会话管理", description = "智能体会话接口")
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final YanManus yanManus;
    private final PlanningAgent planningAgent;
    private final ChatSessionManager sessionManager;

    public ChatController(YanManus yanManus, PlanningAgent planningAgent, ChatSessionManager sessionManager) {
        this.yanManus = yanManus;
        this.planningAgent = planningAgent;
        this.sessionManager = sessionManager;
    }

    @Operation(summary = "发送消息（快速响应）", description = "使用 YanManus 快速响应，仅网页搜索")
    @PostMapping("/send")
    public Map<String, Object> send(
            @Parameter(description = "会话ID，为空则创建新会话")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "用户消息", required = true)
            @RequestParam String message) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        yanManus.setState(AgentState.IDLE);
        RunResult runResult = yanManus.run(message, sessionId, yanManus.getChatSessionManager());
        return Map.of(
                "sessionId", sessionId,
                "agent", "YanManus",
                "result", runResult.result(),
                "steps", runResult.steps()
        );
    }

    @Operation(summary = "流式发送消息（快速响应）", description = "使用 YanManus 流式输出，仅网页搜索")
    @GetMapping("/send/stream")
    public SseEmitter sendStream(
            @Parameter(description = "会话ID，为空则创建新会话")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "用户消息", required = true)
            @RequestParam String message) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        yanManus.setState(AgentState.IDLE);
        return yanManus.runWithSse(message, sessionId, yanManus.getChatSessionManager());
    }

    @Operation(summary = "发送消息（深度思考）", description = "使用 PlanningAgent 深度思考，使用所有工具")
    @PostMapping("/send/planning")
    public Map<String, Object> sendPlanning(
            @Parameter(description = "会话ID，为空则创建新会话")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "用户消息", required = true)
            @RequestParam String message) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        planningAgent.setState(AgentState.IDLE);
        RunResult runResult = planningAgent.run(message, sessionId, planningAgent.getChatSessionManager());
        return Map.of(
                "sessionId", sessionId,
                "agent", "PlanningAgent",
                "result", runResult.result(),
                "steps", runResult.steps()
        );
    }

    @Operation(summary = "流式发送消息（深度思考）", description = "使用 PlanningAgent 流式输出，深度思考")
    @GetMapping("/send/planning/stream")
    public SseEmitter sendPlanningStream(
            @Parameter(description = "会话ID，为空则创建新会话")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "用户消息", required = true)
            @RequestParam String message) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        planningAgent.setState(AgentState.IDLE);
        return planningAgent.runWithSse(message, sessionId, planningAgent.getChatSessionManager());
    }

    @Operation(summary = "获取会话历史")
    @GetMapping("/session/{sessionId}/history")
    public Map<String, Object> getHistory(@PathVariable String sessionId) {
        List<Message> messages = sessionManager.getMessages(sessionId);
        List<Map<String, String>> history = messages.stream()
                .map(msg -> Map.of(
                        "type", msg.getClass().getSimpleName(),
                        "content", msg.getText() != null ? msg.getText() : ""
                ))
                .toList();
        return Map.of(
                "sessionId", sessionId,
                "messageCount", history.size(),
                "messages", history
        );
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/session/{sessionId}")
    public Map<String, String> deleteSession(@PathVariable String sessionId) {
        sessionManager.clearSession(sessionId);
        return Map.of("message", "会话已清除");
    }
}
