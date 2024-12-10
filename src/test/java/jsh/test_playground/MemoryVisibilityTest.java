package jsh.test_playground;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class MemoryVisibilityTest {
    /*
    원인: JVM Memort Model의 동작방식 때문. 각 스레드는 자신만의 CPU 캐시가 쓸수 있음. 스레드 context.
    특히 지금 테스트에서는 하나의 스레드만 실행되기에 context switching이 발생안하는것도 원인이 될 수 있겠다.

    해결: volatile 사용, synchronized 사용, AtomicBoolean 사용
    */

    private static boolean running = true;
    private static final AtomicBoolean atomicRunning = new AtomicBoolean(true);

    @Test
    @DisplayName("running을 false로 변경했음에도 스레드가 종료가 안된다.")
    void test1() throws InterruptedException {
        Thread worker = new Thread(() -> {
            System.out.println("WorkerThread 시작");
            while (running) {
            }
            System.out.println("WorkerThread 종료");

        });

        worker.start();

        Thread.sleep(1000);
        System.out.println("메인 스레드: running을 false로 변경합니다.");
        running = false;
        worker.join();
        System.out.println("프로그램 종료");
    }

    @Test
    @DisplayName("running을 AtomicBoolean로 변경하니 의도대로 스레드가 동작한다.")
    void test2() throws InterruptedException {
        Thread worker = new Thread(() -> {
            System.out.println("WorkerThread 시작");
            while (atomicRunning.get()) {
            }
            System.out.println("WorkerThread 종료");

        });

        worker.start();

        Thread.sleep(1000);
        System.out.println("메인 스레드: running을 false로 변경합니다.");
        atomicRunning.set(false);
        worker.join();
        System.out.println("프로그램 종료");
    }
}
