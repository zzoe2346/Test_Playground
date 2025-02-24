package jsh.test_playground;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class ObjectPoolTest {
    static long k = 0;
    private LongAdder longAdderCounter = new LongAdder();

    @Test
    void objectPoolTest() {
        Queue<Coupon> queue = new ArrayDeque<>();
        Stack<Coupon> s = new Stack<>();
        Queue<Coupon> save = new ArrayDeque();
        for (int i = 0; i < 10000000; i++) {
//            queue.add(new Coupon(i));
            Coupon c = new Coupon(i);
            s.push(c);
            save.add(c);
        }
        int logicCount = 0;
        double startTime = System.currentTimeMillis(); // 시작 시간 기록
        for (int i = 0; i < 10000000; i++) {
            //queue.poll().serviceLogic();
            s.pop();
        }
        double endTime = System.currentTimeMillis(); // 종료 시간 기록
        System.out.println("Execution time: " + (endTime - startTime) + " ms");

    }

    @Test
    void objectPoolTest2() {
//        Queue<Coupon> queue = new ArrayDeque<>();
//        for (int i = 0; i < 1000000; i++) {
//            queue.add(new Coupon(i));
//        }
        int logicCount = 0;
        Queue<Coupon> save = new ArrayDeque();
        long startTime = System.currentTimeMillis(); // 시작 시간 기록
        for (int i = 0; i < 10000000; i++) {
            Coupon coupon = new Coupon(i);
            coupon.serviceLogic();
            save.add(coupon);
        }
        long endTime = System.currentTimeMillis(); // 종료 시간 기록
        System.out.println("Execution time: " + (endTime - startTime) + " ms");

    }

    class Coupon {
        int id;

        public Coupon(int id) {
            this.id = id;
        }

        public void serviceLogic() {
            ++k;
        }
    }

    @Test
    void concurrentObjectPoolTest3() throws InterruptedException {
        int threadCount = 100; // 동시 요청 스레드 수
        int tasksPerThread = 100000; // 스레드당 요청 수
        ExecutorService executor = Executors.newFixedThreadPool(threadCount); // 스레드 풀 생성

        // 객체 풀 초기화
        Queue<Coupon> pool = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < threadCount * tasksPerThread; i++) {
            pool.add(new Coupon(i)); // 객체 미리 생성
        }
        Queue<Coupon> save = new ArrayDeque();

        CountDownLatch latch = new CountDownLatch(threadCount); // 동기화 장치

        long startTime = System.currentTimeMillis(); // 시작 시간 기록

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < tasksPerThread; i++) {
                        // 풀에서 객체 가져오기
                        Coupon coupon = pool.poll();
                        coupon.serviceLogic(); // 로직 수행
                        save.add(coupon);
                    }
                } finally {
                    latch.countDown(); // 작업 완료 신호
                }
            });
        }

        latch.await(); // 모든 스레드 작업 완료 대기
        long endTime = System.currentTimeMillis(); // 종료 시간 기록

        System.out.println("Concurrent Execution time: " + (endTime - startTime) + " ms");
        executor.shutdown(); // 스레드 풀 종료
    }//408

    @Test
    void concurrentObjectCreationTest4() throws InterruptedException {
        int threadCount = 100;
        int tasksPerThread = 100000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Queue<Coupon> save = new ArrayDeque();

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < tasksPerThread; i++) {
                        // 객체 생성 및 로직 수행
                        Coupon coupon = new Coupon(i);
                        coupon.serviceLogic();
                        save.add(coupon);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();

        System.out.println("Concurrent Creation Execution time: " + (endTime - startTime) + " ms");
        executor.shutdown();
    }//88

    @Test
    void concurrentObjectCreationTest6() throws InterruptedException {
        int threadCount = 100;
        int tasksPerThread = 100000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Queue<Coupon> save = new ArrayDeque();
        AtomicInteger atomicInteger = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < tasksPerThread; i++) {
                        // 객체 생성 및 로직 수행
                        atomicInteger.incrementAndGet();
                        //longAdderCounter.increment();
                        Coupon coupon = new Coupon(i);
                        coupon.serviceLogic();
                        save.add(coupon);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();

        System.out.println("Concurrent Creation Execution time: " + (endTime - startTime) + " ms");
        executor.shutdown();
    }//625 585
}
