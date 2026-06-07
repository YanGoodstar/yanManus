package com.zixiang.yanmanus.test;

import com.zixiang.yanmanus.agent.YanManus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AgentTestRunner implements ApplicationRunner {

    @Autowired
    YanManus yanManus;
    @Override
    public void run(ApplicationArguments args) {
//        yanManus.run("生成的文件放在哪了呢");
    }
}
