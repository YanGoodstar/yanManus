package com.zixiang.yanmanus.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BashToolTest {

    private final BashTool bashTool = new BashTool();

    @Test
    void executeSimpleCommand() {
        String result = bashTool.executeCommand("echo hello");
        assertEquals("hello", result);
    }

    @Test
    void executeCommandWithChinese() {
        String result = bashTool.executeCommand("echo 你好");
        assertEquals("你好", result);
    }

    @Test
    void executeInvalidCommand() {
        String result = bashTool.executeCommand("invalid_command_xyz");
        assertNotNull(result);
    }
}
