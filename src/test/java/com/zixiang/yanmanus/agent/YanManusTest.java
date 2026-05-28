package com.zixiang.yanmanus.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yan
 * @create 2026-05-28-20:29
 */
@SpringBootTest
class YanManusTest {

    @Autowired
    YanManus yanManus;
    @Test
    void test() {
        String userPrompt = """  
                我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点，不要太累，悠闲一点，
                制定一份详细的约会计划，""";
        yanManus.run(userPrompt);
    }

}

