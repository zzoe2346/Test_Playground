package jsh.test_playground;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrencyTest {

    @Test
    @DisplayName("Thread 실습")
    void practiceThread() throws InterruptedException {
        Thread thread = new Thread(() -> System.out.println("Hello from new thread"));

        thread.start();
        Thread.yield();//현재 실행중인 스레드가 프로세서를 양보할 용의가 있음을 스케줄러에 알려주는 힌트
        System.out.print("Hello from main thread");
        thread.join();//해당 스레드가 동작을 멈출 때 까지 기다린다. run()이 리턴할 때 까지.
    }

    @Nested
    @DisplayName("동기화의 필요성")
    class ThreadSynchronizationTests {
        @Test
        void counterWithoutSynchronization() throws InterruptedException {
            Counter counter = new Counter();
            class CountingThread extends Thread {
                public void run() {
                    for (int x = 0; x < 10000; x++) {
                        counter.increment();
                    }
                }
            }
            CountingThread t1 = new CountingThread();
            CountingThread t2 = new CountingThread();
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            System.out.println(counter.getCount());
            // 출력 결과: 15118 (동기화가 없을 때 발생하는 문제)
            // 왜? 접근이 동기화가 안되었기 때문. 두 스레드가 동시에 42를 읽는경우 동시에 43을 저장할 것임. 그게 이유.
            // 즉, 두 번이 증가해야하는데 한 번만 증가가 됨
        }

        @Test
        void counterWithSynchronization() throws InterruptedException {
            SyncCounter counter = new SyncCounter();
            class CountingThread extends Thread {
                public void run() {
                    for (int x = 0; x < 10000; x++) {
                        counter.increment();
                    }
                }
            }
            CountingThread t1 = new CountingThread();
            CountingThread t2 = new CountingThread();
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            System.out.println(counter.getCount());
            // 출력 결과: 20000 (동기화로 문제 해결)
            // 예측이 되는 결과가 나온다!
            // increment() 에 synchronized 붙이니 해결. 내재화된 락을 걸어준다.
        }

        static class Counter {
            private int count = 0;

            public void increment() {
                ++count;
            }

            public int getCount() {
                return count;
            }
        }

        static class SyncCounter {
            private int count = 0;

            public synchronized void increment() {
                ++count;
            }

            public int getCount() {
                return count;
            }
        }
    }

    /*
     책에서는 `The meaning of life is: 0` 라는 버그가 나오는걸 보여주는데 지금 아래 코드에서는
      `I don't know the answer`, `The meaning of life is: 42`
      반복하여도 이렇게 2가지만 나온다. 흔히 발생 안하는거 같음.

     `The meaning of life is: 0` 이런 버그의 이유가 자바 메모리 모델과 관련되어있음. 42페이지부터 참고...
     메모리 가시성 관련한 사항
     */
    @Nested
    @DisplayName("경쟁상태")
    class RaceConditionTest {
        static boolean answerReady = false;
        static int answer = 0;
        static Thread t1 = new Thread(() -> {
            answer = 42;
            answerReady = true;
        });
        static Thread t2 = new Thread(() -> {
            if (answerReady) {
                System.out.println("The meaning of life is: " + answer);
            } else {
                System.out.println("I don't know the answer");
            }
        });

        @Test
        void threadStart() throws InterruptedException {
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        }
    }

    @RepeatedTest(10)
    void Qtest() throws InterruptedException {
        Queue<Integer> queue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < 100000; i++) {
            queue.add(i);
        }
        int numberOfThreads = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // 시간 측정 시작
        long startTime = System.nanoTime();

        for (int i = 0; i < 100000; i++) {
            executorService.submit(() -> {
                queue.poll();
            });
        }

        // ExecutorService 종료
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // 시간 측정 종료
        long endTime = System.nanoTime();

        // 검증
        assertEquals(0,queue.size());
//        assertTrue(queue.isEmpty());

        System.out.println("Execution time: " + (endTime - startTime) / 1_000_000 + " ms");
        triggerGC();
    }
    @RepeatedTest(10)
    void Qtest3() throws InterruptedException {
        Queue<Integer> queue = new ArrayBlockingQueue<>(100000);
        for (int i = 0; i < 100000; i++) {
            queue.add(i);
        }
        int numberOfThreads = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // 시간 측정 시작
        long startTime = System.nanoTime();

        for (int i = 0; i < 100000; i++) {
            executorService.submit(() -> {
                queue.poll();
            });
        }

        // ExecutorService 종료
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // 시간 측정 종료
        long endTime = System.nanoTime();

        // 검증
        assertEquals(0,queue.size());
//        assertTrue(queue.isEmpty());

        System.out.println("Execution time: " + (endTime - startTime) / 1_000_000 + " ms");
    }
    @Test
    void Qtest2() throws InterruptedException {
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < 50; i++) {
            queue.add(i);
        }
        int numberOfThreads = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // 시간 측정 시작
        long startTime = System.nanoTime();

        for (int i = 0; i < 50; i++) {
            executorService.submit(() -> {
                queue.poll();
            });
        }

        // ExecutorService 종료
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // 시간 측정 종료
        long endTime = System.nanoTime();

        // 검증
        System.out.println("Execution time: " + (endTime - startTime) / 1_000_000 + " ms");
        System.out.println(queue.size());
        assertTrue(queue.isEmpty());

    }

    @Test
    void QtestMultipleRuns() throws InterruptedException {
        int numberOfRuns = 10; // 반복 횟수
        List<Long> executionTimes = new ArrayList<>();

        for (int run = 0; run < numberOfRuns; run++) {
           // Queue<Integer> queue = new ArrayBlockingQueue<>(100000);
            Queue<Integer> queue = new ConcurrentLinkedQueue<>();
            for (int i = 0; i < 100000; i++) {
                queue.add(i);
            }

            int numberOfThreads = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

            // 시간 측정 시작
            long startTime = System.nanoTime();

            for (int i = 0; i < 100000; i++) {
                executorService.submit(queue::poll);
            }

            // ExecutorService 종료
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);

            // 시간 측정 종료
            long endTime = System.nanoTime();

            // 검증
            assertEquals(0, queue.size());

            // 실행 시간 저장
            executionTimes.add((endTime - startTime) / 1_000_000);
            triggerGC();
        }

        // 실행 시간 출력
        System.out.println("Execution times (ms): " + executionTimes);

        // 간단한 통계
        long min = executionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = executionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        double average = executionTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("Min: " + min + " ms");
        System.out.println("Max: " + max + " ms");
        System.out.println("Average: " + average + " ms");
    }

    void triggerGC() {
        System.gc();
        try {
            Thread.sleep(1000); // GC가 완료될 시간을 주기 위해 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
