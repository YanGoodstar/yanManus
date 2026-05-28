package com.zixiang.yanmanus.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileReadToolTest {

    private final FileReadTool fileReadTool = new FileReadTool();

    @Test
    void readExistingFile(@TempDir Path tempDir) {
        File testFile = tempDir.resolve("test.txt").toFile();
        FileUtil.writeString("hello world", testFile, Charset.forName("UTF-8"));

        String result = fileReadTool.readFile(testFile.getAbsolutePath(), null);
        System.out.println(result);
        assertEquals("hello world", result);
    }

    @Test
    void readFileNotFound() {
        String result = fileReadTool.readFile("/nonexistent/file.txt", null);
        assertTrue(result.startsWith("Error"));
    }

    @Test
    void readWithEncoding(@TempDir Path tempDir) {
        File testFile = tempDir.resolve("encoding.txt").toFile();
        FileUtil.writeString("中文内容", testFile, Charset.forName("UTF-8"));

        String result = fileReadTool.readFile(testFile.getAbsolutePath(), "UTF-8");
        assertEquals("中文内容", result);
    }
}
