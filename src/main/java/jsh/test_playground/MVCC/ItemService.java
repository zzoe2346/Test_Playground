package jsh.test_playground.MVCC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    //for update lock을 획득하고, 가져온 item의 수량을 -1 하고, 5초동안 락을 소유한 후 commit.
    @Transactional
    public void forUpdateLockAndSubtractOneAfterLockDuring2Sec(Long itemId) {
        Item item = itemRepository.findByIdForUpdate(itemId).get();
        item.subtractQuantity(1);

        try {
            Thread.sleep(2000); // 2초 동안 트랜잭션 유지 (락 지속)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    //for update lock을 획득하고, 가져온 item의 수량을 -1 하고, 5초동안 락을 소유한 후 commit.
    @Transactional
    public void forUpdateLockAndSubtractOneAfterLockDuring2SecAndRollback(Long itemId) {
        Item item = itemRepository.findByIdForUpdate(itemId).get();
        item.subtractQuantity(1);

        try {
            Thread.sleep(2000); // 2초 동안 트랜잭션 유지 (락 지속)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new RuntimeException("강제 롤백");
    }

    //for update lock을 획득하고, 가져온 item의 수량을 -1 하고 바로 commit.
    @Transactional
    public void forUpdateLockAndSubtractOne(Long itemId) {
        Item item = itemRepository.findByIdForUpdate(itemId).get();
        item.subtractQuantity(1);
    }

    //단순히 조회한다.
    @Transactional(readOnly = true)
    public int justSelect(Long itemId) {
        Item item = itemRepository.findById(itemId).get();
        return item.getQuantity();
    }

    //단순히 조회하고, 가져온 item의 수량을 -1 하고 바로 commit
    @Transactional
    public void justSelectAndAndSubtractOne(Long itemId) {
        Item item = itemRepository.findById(itemId).get();
        item.subtractQuantity(1);
    }

    @Transactional
    public void justSelectAndAndSubtractOneDuring2Sec(Long itemId) {
        Item item = itemRepository.findById(itemId).get();
        item.subtractQuantity(1);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public void saverNewItemDuring2Sec(Item item) {
        itemRepository.save(item);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


