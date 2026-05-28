package com.zixiang.yanmanus.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TerminateToolTest {

    private final TerminateTool terminateTool = new TerminateTool();

    @Test
    void terminateReturnsMessage() {
        String result = terminateTool.doTerminate();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
