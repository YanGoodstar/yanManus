package com.zixiang.yanmanus.tools;

import cn.hutool.core.io.FileUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.charset.Charset;

/**
 * 文件写入工具 - 写入内容到指定文件
 */
public class FileWriteTool {

    @Tool(description = "Write content to a file. Will overwrite existing files.")
    public String writeFile(
            @ToolParam(description = "the path of the file to write") String path,
            @ToolParam(description = "the content to write to the file") String content) {
        try {
            FileUtil.writeString(content, path, Charset.forName("UTF-8"));
            return "Successfully wrote content to: " + path;
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }
}
