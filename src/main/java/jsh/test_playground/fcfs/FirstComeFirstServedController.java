package jsh.test_playground.fcfs;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RestController
public class FirstComeFirstServedController {
    static int READY_COUPON_COUNT = 10000;
    static int requestCount = 0;
    static Queue<Integer> reserveQueue = new LinkedList<>();
    static int leftCoupon = 0;
    static AtomicInteger i = new AtomicInteger(100);
    static Set<Integer> set = ConcurrentHashMap.newKeySet();

    static ConcurrentLinkedQueue<Coupon> coupons = new ConcurrentLinkedQueue<>();
    static AtomicInteger issuedCouponCount = new AtomicInteger(0);


    //AtomicInteger 활용 (CRS)
    @GetMapping("/atomic")
    public ResponseEntity<Void> atomic() {
        issuedCouponCount.incrementAndGet();

        if (READY_COUPON_COUNT < issuedCouponCount.get()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/reset-atomic")
    public void resetAtomic() {
        issuedCouponCount = new AtomicInteger(0);
    }

    //Object Pool 활용
    @GetMapping("/object-pool")
    public ResponseEntity<Void> objectPool() {
        Coupon issuedCoupon = coupons.poll();

        if (issuedCoupon == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/charge-object-pool")
    public void chargeObjectPool() {
        for (int j = 1; j <= READY_COUPON_COUNT; j++) {
            coupons.add(new Coupon(j));
        }
    }


    @GetMapping("/reserve100")
    public ResponseEntity<String> test() {
        Integer ik = 3;
        int i1 = i.get();

        // CPU 부하를 일정하게 주기 위한 연산
        if (reserveQueue.size() < 100) {
            reserveQueue.add(1);
            return ResponseEntity.ok("reserve success");
        } else {
            System.out.println("e");
            return ResponseEntity.badRequest().build();
        }
    }
    // light weight FCFS microservice

    private boolean check() {
        Lock lock = new ReentrantLock();
        lock.lock();

        if (lock.tryLock()) {

            lock.unlock();
        } else {

        }
        return true;
    }
    //비즈니스로직 통합 하나의 클래스파일에 통합. 명확한 목적이 있는 코드라서 나누는것보다 합쳐놓는게 더 이득이라는 판단

    static class Coupon {
        int issueNumber;
        //LocalDateTime issuedDate;

        public Coupon(int issueNumber) {
            this.issueNumber = issueNumber;
            //this.issuedDate = LocalDateTime.now();
        }


    }
}
