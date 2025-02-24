package jsh.test_playground.mvcc;

import jsh.test_playground.MVCC.Item;
import jsh.test_playground.MVCC.ItemRepository;
import jsh.test_playground.MVCC.ItemService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest
public class MVCCTest {

    private static int SLEEP_TIME = 100;
    private static int TOTAL_THREAD_WAIT_TIME = 3;

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemService itemService;
    private Long itemId;

    @BeforeEach
    public void setup() {
        Item item = new Item("Test Item", 10);
        itemId = itemRepository.save(item).getId();
    }

    //Test Database: MySQL
    //디폴트 isolation level: repeatable read

    /**
     * t1: 먼저 락을 건다.
     * t2: t1 이후에 락을 건다.
     * <p>
     * 결과:t2는 t1의 락이 회수될때 까지 Blocking 된다! 정합성이 확실히 보장되는 경우임
     */
    @Test
    public void test1() throws InterruptedException {
        //given
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        //when
        executorService.submit(() -> {
            try {
                itemService.forUpdateLockAndSubtractOneAfterLockDuring2Sec(itemId);
            } catch (Exception e) {
                System.out.println("스레드 1 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                Thread.sleep(SLEEP_TIME);
                itemService.forUpdateLockAndSubtractOne(itemId);
            } catch (Exception e) {
                System.out.println("스레드 2 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        executorService.awaitTermination(TOTAL_THREAD_WAIT_TIME, TimeUnit.SECONDS);

        //then
        Assertions.assertThat(itemRepository.findById(itemId).get().getQuantity()).isEqualTo(8);
    }

    /**
     * t1: 2초동안 락을 소유하고 1 차감
     * t2: 락이 필요없는 조회 요청
     * <p>
     * 결과: 아직 락인 상태에서 t2가 아이템 남은 개수 조회시 10개로 조회되면 MVCC에 의한 일관된 조회 성공
     * 또한, 결국 남은 아이템 개수는 9개가 되야함.
     */
    @Test
    public void test2() throws InterruptedException {
        //given
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        //when
        executorService.submit(() -> {
            try {
                itemService.forUpdateLockAndSubtractOneAfterLockDuring2Sec(itemId);
            } catch (Exception e) {
                System.out.println("스레드 1 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        AtomicInteger selectedQuantity = new AtomicInteger(-1);
        executorService.submit(() -> {
            try {
                Thread.sleep(SLEEP_TIME);
                selectedQuantity.set(itemService.justSelect(itemId));
            } catch (Exception e) {
                System.out.println("스레드 2 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        executorService.awaitTermination(TOTAL_THREAD_WAIT_TIME, TimeUnit.SECONDS);

        //then
        Assertions.assertThat(selectedQuantity.get()).isEqualTo(10);
        Assertions.assertThat(itemRepository.findById(itemId).get().getQuantity()).isEqualTo(9);
    }

    /**
     * Dirty Read 가 방지됨!
     * t1: 락이 없이 아이템 수량 1차감 시도. 차감된 상태로 약 2초동안 실행됨
     * t2: 락이 없이 조회 요청
     * 결과: t1이 이미 차감되었어도 본인만의 영역에서 차감하였고, 아직 커밋도 안되었기 때문에
     * t2는 아이템 수량을 10으로 조회한다
     */
    @Test
    public void test3() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executorService.submit(() -> {
            try {
                itemService.justSelectAndAndSubtractOneDuring2Sec(itemId);
            } catch (Exception e) {
                System.out.println("스레드 1 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        AtomicInteger selectedQuantity = new AtomicInteger(-1);
        executorService.submit(() -> {
            try {
                Thread.sleep(100);
                selectedQuantity.set(itemService.justSelect(itemId));
            } catch (Exception e) {
                System.out.println("스레드 2 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await(TOTAL_THREAD_WAIT_TIME, TimeUnit.SECONDS);

        //then
        Assertions.assertThat(selectedQuantity.get()).isEqualTo(10);
        Assertions.assertThat(itemRepository.findById(itemId).get().getQuantity()).isEqualTo(9);
    }

    /**
     * MVCC는 커밋된 데이터만 read한다.
     * !!! 그런데 이 테스트 흠...
     * t1: 새로운 아이템 추가하고 2초동안 트랜잭션
     * t2: t1에의해 아이템이 추가되고나서(아직 커밋안된 상태인것_ 전체 아이템수를 조회한다.
     * 결과: t2는 t1이 추가전의 아이템 개수를 read한다. MVCC는 커밋된 데이터를 read하기 때문!
     */
    @Test
    public void test4() throws InterruptedException {
        //givne
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        long originalItemCount = itemRepository.count();

        //when
        executorService.submit(() -> {
            try {
                Item item = new Item("Phantom Item", 5);
                itemService.saverNewItemDuring2Sec(item);
            } catch (Exception e) {
                System.out.println("스레드 1 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        AtomicLong itemCount = new AtomicLong(-1L);
        executorService.submit(() -> {
            try {
                Thread.sleep(300);
                itemCount.set(itemRepository.count());
            } catch (Exception e) {
                System.out.println("스레드 2 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await(3, TimeUnit.SECONDS);

        //then
        Assertions.assertThat(itemCount.get()).isEqualTo(originalItemCount);
        Assertions.assertThat(itemRepository.count()).isEqualTo(originalItemCount + 1);
    }

    /**
     * 두 트랜잭션을 락없이 동시에 수정시키면 Lost Update 가 발생해서 정합성 문제가 생길 가능성이 존재함
     * t1: 락없이 수량을 1차감
     * t2: 락없이 수량을 1차감
     * 결과: 정상적으로 트랜잭션이 serialize하게 되었다면 8이 되야되는데 lost update가 발생하면 9가 될수있다.
     */
    @Test
    public void test5() throws InterruptedException {
        //given
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executorService.submit(() -> {
            try {
                itemService.justSelectAndAndSubtractOne(itemId);
            } catch (Exception e) {
                System.out.println("스레드 1 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                itemService.justSelectAndAndSubtractOne(itemId);
            } catch (Exception e) {
                System.out.println("스레드 2 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await(1, TimeUnit.SECONDS);

        //then
        Item updatedItem = itemRepository.findById(itemId).get();
        System.out.println(updatedItem.getQuantity());// 8 또는 9가 나옴. 정합성 충족 불가함. 거의 대부분 결과가 9임
        Assertions.assertThat(updatedItem.getQuantity()).isLessThanOrEqualTo(9);
    }

    /**
     * t1: 롤백되는 긴 트랜잭션
     * t2: t1에서 일단 값이 하나 빼진후에 조회하면 MVCC로 인
     * 결과: MVCC는 커밋된 값을 읽기에 롤백되든 말든 어차피 커밋이 안된거라 t2는 10을 읽음. 그리고 최종적으로 t1, t2
     * 모두 커밋 후에는 아이템 수량은 10으로 변함이 없음.
     */
    @Test
    public void test6() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executorService.submit(() -> {
            try {
                itemService.forUpdateLockAndSubtractOneAfterLockDuring2SecAndRollback(itemId);
            } catch (Exception e) {
                System.out.println("스레드 1 롤백 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        AtomicInteger beforeRollbackCount = new AtomicInteger(-1);
        executorService.submit(() -> {
            try {
                Thread.sleep(100);
                beforeRollbackCount.set(itemService.justSelect(itemId));
            } catch (Exception e) {
                System.out.println("스레드 2 예외 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await(6, TimeUnit.SECONDS);

        //then
        Assertions.assertThat(itemRepository.findById(itemId).get().getQuantity()).isEqualTo(10);
        Assertions.assertThat(beforeRollbackCount.get()).isEqualTo(10);
    }
}
