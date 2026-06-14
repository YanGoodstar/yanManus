package com.zixiang.yanmanus.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zixiang.yanmanus.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Setter
@Getter
@Slf4j
public class ToolCallAgent extends ReActAgent{

    private static final ObjectMapper objectMapper = new ObjectMapper();

    //可用的工具
    private final ToolCallback[] availableTools;

    //保存工具调用信息的响应
    private ChatResponse toolCallChatResponse;

    //工具调用管理器
    private final ToolCallingManager toolCallingManager;

    //聊天选项 禁用内置的工具调用机制，自己维护上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools, Long userId, String sessionId) {
        super(userId, sessionId);
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

        // 调用工具之前检查是否调用了 askUser，解析结构化参数
        AskUserRequest askRequest = findAskUserRequest();

        String results = executeToolCalls();
        log.info(results);
        // 处理 askUser 逻辑：计数、限流、blocking/default 处理
        if (askRequest != null) {
            handleAskUser(askRequest);
        }

        checkTerminate();
        return results;
    }

    /**
     * 执行所有工具调用并返回结果摘要
     */
    private String executeToolCalls() {
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        //记录消息上下文 调用工具之后 conversationHistory包含了助手消息和调用工具信息
        setMessageList(toolExecutionResult.conversationHistory());

        //返回的 conversationHistory() 是完整的对话历史（包含所有之前的 user/assistant/tool 消息）
        //在工具执行的语境下，最后一轮追加的消息就是工具响应，所以是正确的取法
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        return toolResponseMessage.getResponses().stream()
                .map(toolResponse -> String.format("工具：%s 完成了任务！结果是：%s", toolResponse.name(), toolResponse.responseData()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 处理 askUser 逻辑：计数、限流、blocking/default 处理
     */
    private void handleAskUser(AskUserRequest askRequest) {
        setAskUserCount(getAskUserCount() + 1);
        log.info("askUser 调用 #{}（上限 {}）", getAskUserCount(), getTaskLevel().getMaxAskCount());

        if (getAskUserCount() > getTaskLevel().getMaxAskCount()) {
            log.info("askUser 已达上限, 跳过询问");
            this.getMessageList().add(new UserMessage(buildSkipMessage()));
            return;
        }

        // 非阻塞且有默认值时直接使用默认值，不打断用户
        if (!askRequest.blocking() && askRequest.defaultIfNotAnswered() != null
                && !askRequest.defaultIfNotAnswered().isBlank()) {
            log.info("askUser 非阻塞且有默认值, 使用默认值: {}", askRequest.defaultIfNotAnswered());
            this.getMessageList().add(new UserMessage(askRequest.defaultIfNotAnswered()));
            return;
        }

        // 阻塞式询问或无默认值：需要用户输入
        //todo 前端传用户的问题
        System.out.println(askRequest.question());
        Scanner scanner = new Scanner(System.in);
        String humanAnswer = scanner.nextLine();
        this.getMessageList().add(new UserMessage(humanAnswer));
    }

    /**
     * 检查是否执行了终止工具
     */
    private void checkTerminate() {
        List<Message> messageList = getMessageList();
        Message lastMessage = messageList.getLast();
        if (lastMessage instanceof ToolResponseMessage msg) {
            boolean terminateCalled = msg.getResponses().stream()
                    .anyMatch(toolResponse -> toolResponse.name().equals("doTerminate"));
            if (terminateCalled) {
                setState(AgentState.FINISHED);
            }
        }
    }

    /**
     * 从工具调用参数中解析 askUser 的结构化信息
     * @return 询问请求，如果不是 askUser 调用则返回 null
     */
    private AskUserRequest findAskUserRequest() {
        AssistantMessage assistantMessage = this.toolCallChatResponse.getResult().getOutput();
        for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
            if ("askUser".equals(toolCall.name())) {
                try {
                    JsonNode args = objectMapper.readTree(toolCall.arguments());
                    String question = args.has("question") ? args.get("question").asText() : "请补充必要信息。";
                    boolean blocking = args.has("blocking") && args.get("blocking").asBoolean(false);
                    String defaultVal = args.has("defaultIfNotAnswered") ? args.get("defaultIfNotAnswered").asText(null) : null;
                    return new AskUserRequest(question, blocking, defaultVal);
                } catch (Exception e) {
                    return new AskUserRequest("请补充必要信息。", false, null);
                }
            }
        }
        return null;
    }

    private String buildSkipMessage() {
        int max = getTaskLevel().getMaxAskCount();
        return String.format(
                "[系统提示] 询问用户次数已达上限(%d/%d)。你已使用了全部询问配额。"
                + "请根据已有信息自主做出决策并继续执行任务，不要再次调用 askUser 工具。"
                + "如果信息确实不足以完成任务，请直接调用 terminate 工具结束并说明原因。",
                max, max);
    }

    private record AskUserRequest(String question, boolean blocking, String defaultIfNotAnswered) {}

    @Override
    protected void cleanup() {

    }
}
