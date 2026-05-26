package com.hp.javabase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 应用基础测试，保持为不依赖外部环境的轻量级单元测试。
 */
class JavaBaseApplicationTests {

    @Test
    void applicationClassShouldExposeStableMainClassName() {
        assertEquals("JavaBaseApplication", JavaBaseApplication.class.getSimpleName());
    }
}
