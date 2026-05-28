package com.zixiang.yanmanus.tools;

import cn.hutool.core.io.FileUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.charset.Charset;

/**
 * 文件读取工具 - 读取指定文件的内容
 */
public class FileReadTool {

    @Tool(description = "Read file content. Specify the file path to read.")
    public String readFile(
            @ToolParam(description = "the path of the file to read") String path,
            @ToolParam(description = "the encoding of the file, default is UTF-8", required = false) String encoding) {
        if (encoding == null || encoding.isEmpty()) {
            encoding = "UTF-8";
        }
        if (!FileUtil.exist(path)) {
            return "Error: File not found: " + path;
        }
        try {
            return FileUtil.readString(path, Charset.forName(encoding));
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }
}
