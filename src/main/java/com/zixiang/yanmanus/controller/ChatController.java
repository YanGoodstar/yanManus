package com.zixiang.yanmanus.controller;

import com.zixiang.yanmanus.agent.YanManus;
import com.zixiang.yanmanus.agent.model.AgentState;
import com.zixiang.yanmanus.agent.model.RunResult;
import com.zixiang.yanmanus.memory.ChatSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "会话管理", description = "智能体会话接口")
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final YanManus yanManus;
    private final ChatSessionManager sessionManager;

    public ChatController(YanManus yanManus, ChatSessionManager sessionManager) {
        this.yanManus = yanManus;
        this.sessionManager = sessionManager;
    }

    @Operation(summary = "发送消息", description = "发送消息给智能体，支持多轮对话")
    @PostMapping("/send")
    public Map<String, Object> send(
            @Parameter(description = "会话ID，为空则创建新会话")
            @RequestParam(required = false) String sessionId,
            @Parameter(description = "用户消息", required = true)
            @RequestParam String message) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        //重置 agent 状态，确保可以重新运行
        yanManus.setState(AgentState.IDLE);
        RunResult runResult = yanManus.run(message, sessionId, yanManus.getChatSessionManager());
        return Map.of(
                "sessionId", sessionId,
                "result", runResult.result(),
                "steps", runResult.steps()
        );
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
