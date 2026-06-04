package com.zixiang.yanmanus.tools;

import cn.hutool.core.io.FileUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.nio.charset.Charset;

/**
 * 文件写入工具 - 将内容写入doc目录下的文件
 */
public class FileWriteTool {

    private static final String DOC_DIR = "doc";
    private static final File DOC_PATH = new File(DOC_DIR);

    @Tool(description = "Write content to a file under the doc directory.")
    public String writeFile(
            @ToolParam(description = "the file name to write, e.g. report.txt") String fileName,
            @ToolParam(description = "the content to write to the file") String content) {
        try {
            if (!DOC_PATH.exists()) {
                DOC_PATH.mkdirs();
            }
            File target = new File(DOC_PATH, fileName);
            FileUtil.writeString(content, target, Charset.forName("UTF-8"));
            return "Successfully wrote content to: " + target.getPath();
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }
}
