package com.zixiang.yanmanus.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;

/**
 * 计划工具 - 用于创建和管理执行计划
 */
public class PlanTool {

    private final List<String> planSteps = new ArrayList<>();
    private final List<Boolean> completedSteps = new ArrayList<>();

    @Tool(description = "Create a new execution plan with steps. This replaces any existing plan.")
    public String createPlan(
            @ToolParam(description = "List of steps to execute, separated by newlines") String steps) {
        planSteps.clear();
        completedSteps.clear();

        String[] stepArray = steps.split("\n");
        for (String step : stepArray) {
            String trimmedStep = step.trim();
            if (!trimmedStep.isEmpty()) {
                planSteps.add(trimmedStep);
                completedSteps.add(false);
            }
        }

        if (planSteps.isEmpty()) {
            return "Error: No valid steps provided.";
        }

        StringBuilder result = new StringBuilder("Plan created with " + planSteps.size() + " steps:\n");
        for (int i = 0; i < planSteps.size(); i++) {
            result.append((i + 1)).append(". ").append(planSteps.get(i)).append("\n");
        }
        return result.toString();
    }

    @Tool(description = "Mark a step as completed and return the next step to execute.")
    public String completeStep(
            @ToolParam(description = "The step number to mark as completed (1-based)") int stepNumber) {
        if (stepNumber < 1 || stepNumber > planSteps.size()) {
            return "Error: Invalid step number. Valid range is 1-" + planSteps.size();
        }

        int index = stepNumber - 1;
        if (completedSteps.get(index)) {
            return "Step " + stepNumber + " was already completed.";
        }

        completedSteps.set(index, true);

        // Find next incomplete step
        for (int i = 0; i < planSteps.size(); i++) {
            if (!completedSteps.get(i)) {
                int nextStep = i + 1;
                int completed = (int) completedSteps.stream().filter(b -> b).count();
                return "Step " + stepNumber + " completed.\n" +
                       "Progress: " + completed + "/" + planSteps.size() + " steps done.\n" +
                       "Next step (" + nextStep + "): " + planSteps.get(i);
            }
        }

        return "Step " + stepNumber + " completed.\nAll steps completed!";
    }

    @Tool(description = "Get the current status of the plan, including which steps are done.")
    public String getPlanStatus() {
        if (planSteps.isEmpty()) {
            return "No plan exists. Use createPlan to create one.";
        }

        int completed = (int) completedSteps.stream().filter(b -> b).count();
        StringBuilder result = new StringBuilder("Plan Status: " + completed + "/" + planSteps.size() + " steps completed\n\n");

        for (int i = 0; i < planSteps.size(); i++) {
            String status = completedSteps.get(i) ? "[DONE]" : "[TODO]";
            result.append(status).append(" Step ").append(i + 1).append(": ").append(planSteps.get(i)).append("\n");
        }

        // Show next step if not all done
        if (completed < planSteps.size()) {
            for (int i = 0; i < planSteps.size(); i++) {
                if (!completedSteps.get(i)) {
                    result.append("\nNext step to execute: Step ").append(i + 1).append(": ").append(planSteps.get(i));
                    break;
                }
            }
        }

        return result.toString();
    }

    @Tool(description = "Get all steps in the plan.")
    public String getAllSteps() {
        if (planSteps.isEmpty()) {
            return "No plan exists. Use createPlan to create one.";
        }

        StringBuilder result = new StringBuilder("All plan steps:\n");
        for (int i = 0; i < planSteps.size(); i++) {
            String status = completedSteps.get(i) ? "[DONE]" : "[TODO]";
            result.append(status).append(" Step ").append(i + 1).append(": ").append(planSteps.get(i)).append("\n");
        }
        return result.toString();
    }
}
