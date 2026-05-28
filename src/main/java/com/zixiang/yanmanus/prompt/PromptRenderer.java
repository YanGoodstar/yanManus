package com.zixiang.yanmanus.prompt;

import java.util.Map;

/**
 * 提示词模板渲染器 - 将模板中的 {key} 占位符替换为实际值
 */
public class PromptRenderer {

    /**
     * 渲染提示词模板
     * @param template 提示词模板，包含 {key} 占位符
     * @param variables 占位符变量映射
     * @return 渲染后的提示词
     */
    public static String render(String template, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
