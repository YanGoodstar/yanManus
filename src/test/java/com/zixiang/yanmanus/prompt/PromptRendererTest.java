package com.zixiang.yanmanus.prompt;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yan
 * @create 2026-05-28-20:37
 */
class PromptRendererTest {

    @Test
    void render() {
        String renderPrompt = PromptRenderer.render(AgentPrompts.SYSTEM_PROMPT, Map.of(
                "agentName", "YanManus",
                "capabilities", "文件读写、网页搜索、命令行执行"
        ));
        System.out.println(renderPrompt);
    }
}