package com.zixiang.yanmanus.agent;

import cn.hutool.core.util.StrUtil;
import com.zixiang.yanmanus.agent.model.AgentState;
import com.zixiang.yanmanus.agent.model.TaskLevel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

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

    //memory
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    //todo 添加流式响应
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)){
            throw new RuntimeException("User prompt cannot be empty");
        }
        //修改状态
        setAskUserCount(0);
        this.state = AgentState.RUNNING;
        //记录消息上下文
        this.messageList.add(new UserMessage(userPrompt));
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < this.maxStep && this.state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxStep);
                //单步执行
                String stepResult = this.step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            //检查是否超出步骤限制
            if (currentStep >= maxStep) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxStep + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent: " + e.getMessage(), e);
            return "执行错误" + e.getMessage();
        } finally {
            //清理资源
            this.cleanup();
        }
    }

    protected abstract void cleanup();

    public abstract String step();
}
