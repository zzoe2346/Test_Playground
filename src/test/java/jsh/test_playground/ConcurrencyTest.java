package jsh.test_playground;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
