package com.zixiang.yanmanus.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * 命令行工具 - 执行系统命令
 */
public class BashTool {

    @Tool(description = "Execute a bash command and return its output.")
    public String executeCommand(
            @ToolParam(description = "the bash command to execute") String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                builder.command("cmd.exe", "/c", command);
            } else {
                builder.command("sh", "-c", command);
            }
            builder.redirectErrorStream(true);
            Process process = builder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            String result = output.toString().trim();
            if (result.isEmpty()) {
                return exitCode == 0 ? "Command executed successfully (no output)" : "Command failed with exit code: " + exitCode;
            }
            return result;
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }
}
