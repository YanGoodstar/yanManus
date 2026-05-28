package com.zixiang.yanmanus.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebSearchToolTest {

    @Value("${serpapi.api-key:}")
    private String serpApiKey;

    @Test
    void searchWithBlankApiKey() {
        WebSearchTool tool = new WebSearchTool(serpApiKey);
        String result = tool.search("coffee");
        System.out.println(result);
    }

    @Test
    void searchWithNullApiKey() {
        WebSearchTool tool = new WebSearchTool(serpApiKey);
        String result = tool.search("test");
        assertTrue(result.contains("Error"));
    }
}
