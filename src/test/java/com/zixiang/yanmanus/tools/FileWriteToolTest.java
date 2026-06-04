package com.zixiang.yanmanus.tools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.*;

class FileWriteToolTest {

    private final FileWriteTool fileWriteTool = new FileWriteTool();

    @AfterEach
    void cleanup() {
        FileUtil.del(new File("doc"));
    }

    @Test
    void writeNewFile() {
        String result = fileWriteTool.writeFile("output.txt", "test content");
        assertTrue(result.startsWith("Successfully"));

        String content = FileUtil.readString(new File("doc/output.txt"), Charset.forName("UTF-8"));
        assertEquals("test content", content);
    }

    @Test
    void overwriteExistingFile() {
        FileUtil.writeString("old content", new File("doc/overwrite.txt"), Charset.forName("UTF-8"));

        fileWriteTool.writeFile("overwrite.txt", "new content");
        String content = FileUtil.readString(new File("doc/overwrite.txt"), Charset.forName("UTF-8"));
        assertEquals("new content", content);
    }

    @Test
    void writeChineseContent() {
        fileWriteTool.writeFile("chinese.txt", "你好世界");
        String content = FileUtil.readString(new File("doc/chinese.txt"), Charset.forName("UTF-8"));
        assertEquals("你好世界", content);
    }

    @Test
    void createDocDirAutomatically() {
        FileUtil.del(new File("doc"));
        fileWriteTool.writeFile("test.txt", "hello");
        assertTrue(new File("doc").exists());
    }
}
