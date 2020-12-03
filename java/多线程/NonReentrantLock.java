package com.example.zklock;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class NonReentrantLock implements Lock, Serializable {

    private final Sync sync = new Sync();

    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.isHeldExclusively();
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1,unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }

    private static class Sync extends AbstractQueuedSynchronizer{
      @Override
      protected boolean isHeldExclusively() {
          return getState() == 1;
      }

      protected Sync() {
          super();
      }

        /**
         * 如果state为0，则尝试获取锁
         * @param arg
         * @return
         */
      @Override
      protected boolean tryAcquire(int arg) {
          assert arg == 1;
          if(compareAndSetState(0,1)){
              setExclusiveOwnerThread(Thread.currentThread());
              return true;
          }
          return false;
      }

        /**
         * 尝试释放锁，设置state为0
         * @param arg
         * @return
         */
      @Override
      protected boolean tryRelease(int arg) {
          assert arg == 1;
          if(getState() == 0){
              throw new IllegalMonitorStateException();

          }
          setExclusiveOwnerThread(null);
          setState(0);
          return true;
      }



      @Override
      protected int tryAcquireShared(int arg) {
          return super.tryAcquireShared(arg);
      }

      @Override
      protected boolean tryReleaseShared(int arg) {
          return super.tryReleaseShared(arg);
      }

        /**
         * 提供条件变量接口
         * @return
         */
      Condition newCondition(){
          return new ConditionObject();
      }

      @Override
      public String toString() {
          return super.toString();
      }
  }

}
