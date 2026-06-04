package com.zixiang.yanmanus.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AskUserTool {

    @Tool(description = "Ask the user a question when uncertain. Use this when you need clarification or more information to proceed.")
    public String askUser(
            @ToolParam(description = "The question to ask the user") String question) {
        System.out.println("[Agent asks]: " + question);
        System.out.print("[Your answer]: ");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            return reader.readLine();
        } catch (Exception e) {
            return "Error reading input: " + e.getMessage();
        }
    }
}
