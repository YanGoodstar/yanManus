package com.zixiang.yanmanus.agent;

import cn.hutool.core.util.StrUtil;
import com.zixiang.yanmanus.agent.model.AgentState;
import com.zixiang.yanmanus.agent.model.RunResult;
import com.zixiang.yanmanus.agent.model.TaskLevel;
import com.zixiang.yanmanus.memory.ChatSessionManager;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Data
public abstract class BaseAgent {
    private String agentName;

    private final Long userId;

    private final String sessionId;

    //提示词
    private String systemPrompt;
    private String nextStepPrompt;

    //状态
    private AgentState state = AgentState.IDLE;

    //执行控制
    private int maxStep = 10;
    private int currentStep = 0;

    //询问用户控制
    private TaskLevel taskLevel = TaskLevel.MODERATE;
    private int askUserCount = 0;

    //LLM
    private ChatClient chatClient;

    private List<Message> messageList = new ArrayList<>();

    //会话管理
    private ChatSessionManager chatSessionManager;

    /**
     * 运行代理
     * 流式输出
     * @param userPrompt 用户提示词
     * @return 最终回复和步骤日志
     */
    public SseEmitter runWithSse(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(5 * 60 * 1000L);

        CompletableFuture.runAsync(() -> {

            try {
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("Cannot run agent from state " + this.state);
                }
                if (StrUtil.isBlank(userPrompt)){
                    sseEmitter.send("User prompt cannot be empty");
                }
            } catch (IOException e) {
                sseEmitter.completeWithError(e);
                return;
            }
            setAskUserCount(0);
            this.state = AgentState.RUNNING;
            //从 Redis/缓存 加载会话消息
            List<Message> sessionMessages = getChatSessionManager().getMessages(userId, sessionId);
            List<Message> messageList = new ArrayList<>(sessionMessages);
            messageList.add(new UserMessage(userPrompt));
            setMessageList(messageList);

            try {
                for (int i = 0; i < this.maxStep && this.state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxStep);
                    String stepResult = this.step();
                    log.info("Step {}: {}", stepNumber, stepResult);
                    //每步执行后持久化消息到 Redis
                    getChatSessionManager().saveMessages(userId, sessionId, getMessageList());
                    sseEmitter.send(stepResult);
                }
                if (currentStep >= maxStep) {
                    state = AgentState.FINISHED;
                    log.warn("Terminated: Reached max steps ({})", maxStep);
                    sseEmitter.send("执行结束，步骤达到最大步骤");
                }
                //最终持久化
                getChatSessionManager().saveMessages(userId, sessionId, getMessageList());
                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing agent: " + e.getMessage(), e);
            } finally {
                this.cleanup();
            }

        });

        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
        });

        sseEmitter.onCompletion(() -> {
            if (this.state != AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
        });
        return sseEmitter;
    }

    protected abstract void cleanup();

    public abstract String step();
}
