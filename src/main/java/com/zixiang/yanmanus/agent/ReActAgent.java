package com.zixiang.yanmanus.agent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ReActAgent extends BaseAgent{
    /**
     *
     * @return 是否执行行动，true需要，false不需要
     */
    public abstract boolean think();

    /**
     * 执行决定的行动
     */
    public abstract String act();

    /**
     * 执行单个步骤，思考和行动
     * @return 步骤执行结果
     */
    @Override
    public String step() {
        try {
            if(!think()){
                return "思考完成 - 无需行动";
            }
            return act();
        }catch (Exception e){
            //记录异常日志
            log.error("步骤执行失败：", e);
            return "步骤执行失败：" + e.getMessage();
        }

    }
}
