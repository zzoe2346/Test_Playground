package jsh.test_playground;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegerCachingTest {
    /**
     * Constant Pool: JVM은 -128에서 127 범위의 정수(Short, Integer, Long 등)를 미리 캐싱하여, 같은 값을 반복해서 생성하지 않고, 해당 범위의 정수는 동일 객체를 재사용
     * <p>
     * 동일한 숫자 객체를 자주 사용하므로 메모리 사용량을 줄이고, 객체 생성 오버헤드를 줄이는 효과
     */
    @Test
    @DisplayName("JVM 의 Integer 캐싱 테스트")
    void integerCacheTest() {
        Integer integer1 = 127;
        Integer integer2 = 127;

        assertTrue(integer1 == integer2);

        integer1 = -128;
        integer2 = -128;

        assertTrue(integer1 == integer2);

        integer1 = 128;
        integer2 = 128;

        assertFalse(integer1 == integer2);

        integer1 = -129;
        integer2 = -129;

        assertFalse(integer1 == integer2);
    }

    @Test
    @DisplayName("JVM 의 Long 캐싱 테스트")
    void longCacheTest() {
        Long long1 = 127L;
        Long long2 = 127L;

        assertTrue(long1 == long2);

        long1 = -128L;
        long2 = -128L;

        assertTrue(long1 == long2);

        long1 = 128L;
        long2 = 128L;

        assertFalse(long1 == long2);

        long1 = -129L;
        long2 = -129L;

        assertFalse(long1 == long2);
    }
}
