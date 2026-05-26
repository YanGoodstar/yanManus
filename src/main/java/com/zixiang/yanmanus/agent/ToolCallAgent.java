package com.zixiang.yanmanus.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zixiang.yanmanus.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptionsBuilder;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent{

    //可用的工具
    private final ToolCallback[] availableTools;

    //保存工具调用信息的响应
    private ChatResponse toolCallChatResponse;

    //工具调用管理器
    private final ToolCallingManager toolCallingManager;

    //聊天选项 禁用内置的工具调用机制，自己维护上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .internalToolExecutionEnabled(false).build();

    }

    /**
     * 处理当前状态并决定下一步行动
     * @return 是否执行行动
     */
    @Override
    public boolean think() {
        if (!StrUtil.isBlank(getNextStepPrompt())) {
            //将下一步的提示词添加进message列表喂给大模型
            getMessageList().add(new UserMessage(getNextStepPrompt()));
        }
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);
        try {
            //获取带工具的响应
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            //记录响应 用于Act
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            //输出提示信息
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            log.info("{}的思考{}", getAgentName(), result);
            log.info("{}选择了{}个工具", getAgentName(), toolCallList.size());
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s,工具参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            if (toolCallList.isEmpty()){
                //没有工具需要调用的时候记录助手消息
                getMessageList().add(assistantMessage);
                return false;
            } else {
                //需要执行Act
                return true;
            }
        }catch (Exception e){
            log.error("调用工具失败",e);
            getMessageList().add(new AssistantMessage("处理时遇到错误:" + e.getMessage()));
            return false;
        }
    }

    /**
     * 调用工具
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }
        //调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        //记录消息上下文 调用工具之后 conversationHistory包含了助手消息和调用工具信息
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(toolResponse -> String.format("工具：%s 完成了任务！结果是：%s", toolResponse.name(), toolResponse.responseData()))
                .collect(Collectors.joining("\n"));

        //判断是否执行了终止工具
        boolean terminateCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(toolResponse -> toolResponse.name().equals("TerminateTool"));
        if (terminateCalled){
            setState(AgentState.FINISHED);
        }
        log.info(results);
        return results;
    }

    @Override
    protected void cleanup() {

    }
}
