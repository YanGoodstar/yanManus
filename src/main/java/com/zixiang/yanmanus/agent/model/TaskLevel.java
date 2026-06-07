package com.zixiang.yanmanus.agent.model;

import lombok.Getter;

/**
 * 任务复杂度等级，决定 askUser 工具的调用上限
 */
@Getter
public enum TaskLevel {

    SIMPLE("简单任务", 0, "尽量自主完成，不要询问用户"),
    MODERATE("中等任务", 2, "可以适度询问，但优先自主尝试"),
    COMPLEX("复杂任务", 5, "允许较多询问，但每次询问前应尽力尝试");

    private final String description;
    private final int maxAskCount;
    private final String guidance;

    TaskLevel(String description, int maxAskCount, String guidance) {
        this.description = description;
        this.maxAskCount = maxAskCount;
        this.guidance = guidance;
    }

}
