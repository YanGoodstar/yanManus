package com.zixiang.yanmanus.agent;

import cn.hutool.core.util.StrUtil;
import com.zixiang.yanmanus.agent.model.AgentState;
import com.zixiang.yanmanus.agent.model.RunResult;
import com.zixiang.yanmanus.agent.model.TaskLevel;
import com.zixiang.yanmanus.memory.ChatSessionManager;
import lombok.Data;
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

@Data
@Slf4j
public abstract class BaseAgent {
    private String agentName;

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

    //ThreadLocal 管理会话消息列表，支持 sessionId 隔离
    private static final ThreadLocal<List<Message>> currentMessageList = new ThreadLocal<>();

    //ThreadLocal 记录每步执行日志
    private static final ThreadLocal<List<String>> currentStepLogs = new ThreadLocal<>();

    protected static List<Message> getMessageList() {
        List<Message> list = currentMessageList.get();
        return list != null ? list : new ArrayList<>();
    }

    protected static void setMessageList(List<Message> messages) {
        currentMessageList.set(messages);
    }

    /**
     * 运行代理
     * @param userPrompt 用户提示词
     * @param sessionId  会话ID
     * @param sessionManager 会话管理器
     * @return 最终回复和步骤日志
     */
    public RunResult run(String userPrompt, String sessionId, ChatSessionManager sessionManager) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)){
            throw new RuntimeException("User prompt cannot be empty");
        }
        setAskUserCount(0);
        this.state = AgentState.RUNNING;
        //从 Redis/缓存 加载会话消息
        List<Message> sessionMessages = sessionManager.getMessages(sessionId);
        List<Message> messageList = new ArrayList<>(sessionMessages);
        messageList.add(new UserMessage(userPrompt));
        currentMessageList.set(messageList);
        currentStepLogs.set(new ArrayList<>());

        try {
            for (int i = 0; i < this.maxStep && this.state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxStep);
                String stepResult = this.step();
                log.info("Step {}: {}", stepNumber, stepResult);
                currentStepLogs.get().add("Step " + stepNumber + ": " + stepResult);
                //每步执行后持久化消息到 Redis
                sessionManager.saveMessages(sessionId, getMessageList());
            }
            if (currentStep >= maxStep) {
                state = AgentState.FINISHED;
                log.warn("Terminated: Reached max steps ({})", maxStep);
            }
            //最终持久化
            sessionManager.saveMessages(sessionId, getMessageList());
            String finalResult = extractLastAssistantText();
            return new RunResult(finalResult, currentStepLogs.get());
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent: " + e.getMessage(), e);
            return new RunResult("执行错误" + e.getMessage(), currentStepLogs.get());
        } finally {
            this.cleanup();
            currentMessageList.remove();
            currentStepLogs.remove();
        }
    }


    /**
     * 运行代理
     * 流式输出
     * @param userPrompt 用户提示词
     * @param sessionId  会话ID
     * @param sessionManager 会话管理器
     * @return 最终回复和步骤日志
     */
    public SseEmitter runWithSse(String userPrompt, String sessionId, ChatSessionManager sessionManager) {
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
            List<Message> sessionMessages = sessionManager.getMessages(sessionId);
            List<Message> messageList = new ArrayList<>(sessionMessages);
            messageList.add(new UserMessage(userPrompt));
            currentMessageList.set(messageList);
            currentStepLogs.set(new ArrayList<>());

            try {
                for (int i = 0; i < this.maxStep && this.state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxStep);
                    String stepResult = this.step();
                    log.info("Step {}: {}", stepNumber, stepResult);
                    currentStepLogs.get().add("Step " + stepNumber + ": " + stepResult);
                    //每步执行后持久化消息到 Redis
                    sessionManager.saveMessages(sessionId, getMessageList());
                    sseEmitter.send(stepResult);
                }
                if (currentStep >= maxStep) {
                    state = AgentState.FINISHED;
                    log.warn("Terminated: Reached max steps ({})", maxStep);
                    sseEmitter.send("执行结束，步骤达到最大步骤");
                }
                //最终持久化
                sessionManager.saveMessages(sessionId, getMessageList());
                String finalResult = extractLastAssistantText();
                sseEmitter.send(finalResult);
                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing agent: " + e.getMessage(), e);
            } finally {
                this.cleanup();
                currentMessageList.remove();
                currentStepLogs.remove();
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

    /**
     * 从消息列表中提取最后一条 AssistantMessage 的文本作为最终回复
     */
    private String extractLastAssistantText() {
        List<Message> messages = getMessageList();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof AssistantMessage assistant) {
                if (assistant.getText() != null && !assistant.getText().isBlank()) {
                    return assistant.getText();
                }
            }
        }
        return "任务已完成";
    }

    protected abstract void cleanup();

    public abstract String step();
}
