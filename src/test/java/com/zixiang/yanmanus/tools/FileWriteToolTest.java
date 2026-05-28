package com.zixiang.yanmanus.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileWriteToolTest {

    private final FileWriteTool fileWriteTool = new FileWriteTool();

    @Test
    void writeNewFile(@TempDir Path tempDir) {
        String path = tempDir.resolve("output.txt").toString();

        String result = fileWriteTool.writeFile(path, "test content");
        assertTrue(result.startsWith("Successfully"));

        String content = FileUtil.readString(path, Charset.forName("UTF-8"));
        assertEquals("test content", content);
    }

    @Test
    void overwriteExistingFile(@TempDir Path tempDir) {
        String path = tempDir.resolve("overwrite.txt").toFile().getAbsolutePath();
        FileUtil.writeString("old content", new File(path), Charset.forName("UTF-8"));

        fileWriteTool.writeFile(path, "new content");
        String content = FileUtil.readString(path, Charset.forName("UTF-8"));
        assertEquals("new content", content);
    }

    @Test
    void writeChineseContent(@TempDir Path tempDir) {
        String path = tempDir.resolve("chinese.txt").toString();

        fileWriteTool.writeFile(path, "你好世界");
        String content = FileUtil.readString(path, Charset.forName("UTF-8"));
        assertEquals("你好世界", content);
    }
}
