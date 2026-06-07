package com.zixiang.yanmanus.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Python脚本执行工具
 */
public class PythonScriptTool {

    private final String pythonCommand;

    public PythonScriptTool(String pythonCommand) {
        this.pythonCommand = pythonCommand;
    }

    @Tool(description = "Execute a Python script and return its output. " +
            "Use 'scriptPath' to run a .py file, or use 'scriptContent' to run inline Python code. " +
            "Optional 'arguments' can be passed to the script via sys.argv.")
    public String executePythonScript(
            @ToolParam(description = "Path to the Python script file (.py). Either scriptPath or scriptContent must be provided.") String scriptPath,
            @ToolParam(description = "Inline Python code to execute. Either scriptPath or scriptContent must be provided.") String scriptContent,
            @ToolParam(description = "Space-separated arguments to pass to the script (optional).") String arguments) {
        try {
            List<String> command = new ArrayList<>();
            command.add(pythonCommand);

            if (scriptPath != null && !scriptPath.isBlank()) {
                Path path = Path.of(scriptPath);
                if (!Files.exists(path)) {
                    return "Error: Script file not found: " + scriptPath;
                }
                command.add(scriptPath);
            } else if (scriptContent != null && !scriptContent.isBlank()) {
                command.add("-c");
                command.add(scriptContent);
            } else {
                return "Error: Either scriptPath or scriptContent must be provided.";
            }

            if (arguments != null && !arguments.isBlank()) {
                command.addAll(Arrays.asList(arguments.trim().split("\\s+")));
            }

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(false);
            builder.environment().put("PYTHONIOENCODING", "utf-8");

            Process process = builder.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            try (BufferedReader outReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = outReader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            }

            try (BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errReader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            String output = stdout.toString().trim();
            String errors = stderr.toString().trim();

            StringBuilder result = new StringBuilder();
            if (!output.isEmpty()) {
                result.append(output);
            }
            if (exitCode != 0) {
                result.append("\n[Exit code: ").append(exitCode).append("]");
            }
            if (!errors.isEmpty()) {
                result.append("\n[Stderr]\n").append(errors);
            }
            if (result.isEmpty()) {
                return exitCode == 0 ? "Script executed successfully (no output)" : "Script failed with exit code: " + exitCode;
            }
            return result.toString().trim();
        } catch (Exception e) {
            return "Error executing Python script: " + e.getMessage();
        }
    }
}
