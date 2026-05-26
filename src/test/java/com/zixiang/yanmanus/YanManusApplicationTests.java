package com.zixiang.yanmanus;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class YanManusApplicationTests {

    @Autowired
    ChatModel dashScopeChatModel;

    ChatClient chatClient;
    @Test
    void contextLoads() {
        chatClient = ChatClient.builder(dashScopeChatModel).build();
        ChatResponse chatResponse = chatClient.prompt(new Prompt("你好呀"))
                .call()
                .chatResponse();

        String result = chatResponse.getResult().getOutput().getText();
        System.out.println(result);
    }

}
