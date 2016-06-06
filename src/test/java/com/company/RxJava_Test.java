package com.company;

import com.company.Util;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executor;

public class RxJava_Test {

    Util.Output out = new Util.Output();

    private void assertOut(String exceptedStr) {
        Assert.assertEquals(exceptedStr.substring(12), out.popLine().substring(12));
    }

    public Observable<String> createSlowProducer() {
        return Observable.create((Subscriber<? super String> consumer) -> {
                    Util.sleep(1); //sleep 1ms to just let other thread run so can get fixed output
                    out.println("SLOW producer begin");
                    out.println("SLOW producer sleep a while");
                    Util.sleep(3000);
                    out.println("SLOW producer notify consumer");
                    consumer.onNext("SLOW result");
                    consumer.onCompleted();
                    out.println("SLOW producer end");
                }
        );
    }

    private static Scheduler createTestScheduler(String name) {
        return Schedulers.from(new Executor() {
            @Override
            public void execute(Runnable command) {
                new Thread(command, name).start();
            }
        });
    }

    @Test
    public void test_threading_default_in_current_thread() {
        new Thread(() -> {
            out.println("enter test function");

            createSlowProducer()
                    .subscribe(producedResult -> {
                        out.println("---- consumer got produced result");
                    });

            out.println("leave test function");
        }, "TestThread").start();

        assertOut("17:02:56.571 [TestThread] enter test function");
        assertOut("17:02:56.667 [TestThread] SLOW producer begin");
        assertOut("17:02:56.667 [TestThread] SLOW producer sleep a while");
        assertOut("17:02:59.668 [TestThread] SLOW producer notify consumer");
        assertOut("17:02:59.668 [TestThread] ---- consumer got produced result");
        assertOut("17:02:59.668 [TestThread] SLOW producer end");
        assertOut("17:02:59.669 [TestThread] leave test function");
    }

    @Test
    public void test_rx_will_produce_and_consume_in_an_RxScheduler_thread() {
        new Thread(() -> {
            out.println("enter test function");

            createSlowProducer()
                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-1")) //cause producer run in new thread
                    .subscribe(producedResult -> {
                        out.println("---- consumer got produced result");
                    });

            out.println("leave test function");
        }, "TestThread").start();

        assertOut("11:49:51.169 [TestThread] enter test function");
        assertOut("11:49:51.217 [TestThread] leave test function");
        assertOut("11:49:51.218 [RxNewThreadScheduler-1] SLOW producer begin");
        assertOut("11:49:51.218 [RxNewThreadScheduler-1] SLOW producer sleep a while");
        assertOut("11:49:54.221 [RxNewThreadScheduler-1] SLOW producer notify consumer");
        assertOut("11:49:54.221 [RxNewThreadScheduler-1] ---- consumer got produced result");
        assertOut("11:49:54.222 [RxNewThreadScheduler-1] SLOW producer end");
    }

    @Test
    public void test_rx_will_produce_in_an_RxScheduler_thread_and_consume_in_another_RxScheduler_thread() {
        new Thread(() -> {
            out.println("enter test function");

            createSlowProducer()
                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-1")) //cause producer run in new thread
                    .observeOn(createTestScheduler("RxNewThreadScheduler-2")) //cause consumer run in another new thread
                    .subscribe(producedResult -> {
                        Util.sleep(1); //sleep 1ms to just let other thread run so can get fixed output
                        out.println("---- consumer got produced result");
                    });

            out.println("leave test function");
        }, "TestThread").start();

        assertOut("12:41:10.523 [TestThread] enter test function");
        assertOut("12:41:10.676 [TestThread] leave test function");
        assertOut("12:41:10.678 [RxNewThreadScheduler-1] SLOW producer begin");
        assertOut("12:41:10.678 [RxNewThreadScheduler-1] SLOW producer sleep a while");
        assertOut("12:41:13.682 [RxNewThreadScheduler-1] SLOW producer notify consumer");
        assertOut("12:41:13.682 [RxNewThreadScheduler-1] SLOW producer end");
        assertOut("12:41:13.683 [RxNewThreadScheduler-2] ---- consumer got produced result");
    }

    @Test
    public void test_rx_will_produce_if_subscribe_called_even_without_callback() {
        new Thread(() -> {
            out.println("enter test function");

            createSlowProducer()
                    .subscribe();

            out.println("leave test function");
        }, "TestThread").start();

        assertOut("17:08:28.050 [TestThread] enter test function");
        assertOut("17:08:28.145 [TestThread] SLOW producer begin");
        assertOut("17:08:28.145 [TestThread] SLOW producer sleep a while");
        assertOut("17:08:31.145 [TestThread] SLOW producer notify consumer");
        assertOut("17:08:31.146 [TestThread] SLOW producer end");
        assertOut("17:08:31.146 [TestThread] leave test function");
    }

    @Test
    public void test_rx_will_not_produce_if_not_subscribed() {
        new Thread(() -> {
            out.println("enter test function");

            createSlowProducer();

            out.println("leave test function");
        }, "TestThread").start();

        assertOut("17:06:31.746 [TestThread] enter test function");
        assertOut("17:06:31.834 [TestThread] leave test function");
    }

    public Observable<String> createFastProducer() {
        return Observable.create((Subscriber<? super String> consumer) -> {
                    Util.sleep(50); //sleep 50ms to just let other thread run so can get fixed output
                    out.println("FAST producer begin");
                    consumer.onNext("FAST result");
                    consumer.onCompleted();
                    out.println("FAST producer end");
                }
        );
    }

    @Test
    public void test_merge_parallelly() throws InterruptedException {
        new Thread(() -> {
            createSlowProducer()
                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-1"))
                    .mergeWith(
                            createFastProducer()
                                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-2"))
                    )
                    .subscribe(producedResult -> {
                        out.println("---- consumer got " + producedResult);

                    });
        }, "TestThread").start();

        assertOut("13:21:01.706 [RxNewThreadScheduler-1] SLOW producer begin");
        assertOut("13:21:01.711 [RxNewThreadScheduler-1] SLOW producer sleep a while");
        assertOut("13:21:01.746 [RxNewThreadScheduler-2] FAST producer begin");
        assertOut("13:21:01.746 [RxNewThreadScheduler-2] ---- consumer got FAST result");
        assertOut("13:21:01.748 [RxNewThreadScheduler-2] FAST producer end");
        assertOut("13:21:04.713 [RxNewThreadScheduler-1] SLOW producer notify consumer");
        assertOut("13:21:04.713 [RxNewThreadScheduler-1] ---- consumer got SLOW result");
        assertOut("13:21:04.713 [RxNewThreadScheduler-1] SLOW producer end");
    }

    @Test
    public void test_merge_serially_if_not_specify_schedule_thread() throws InterruptedException {
        new Thread(() -> {
            createSlowProducer()
                    .mergeWith(
                            createFastProducer()
                    )
                    .subscribe(producedResult -> {
                        out.println("---- consumer got " + producedResult);

                    });
        }, "TestThread").start();

        assertOut("13:24:51.112 [TestThread] SLOW producer begin");
        assertOut("13:24:51.114 [TestThread] SLOW producer sleep a while");
        assertOut("13:24:54.118 [TestThread] SLOW producer notify consumer");
        assertOut("13:24:54.119 [TestThread] ---- consumer got SLOW result");
        assertOut("13:24:54.119 [TestThread] SLOW producer end");
        assertOut("13:24:54.174 [TestThread] FAST producer begin");
        assertOut("13:24:54.174 [TestThread] ---- consumer got FAST result");
        assertOut("13:24:54.174 [TestThread] FAST producer end");
    }

    @Test
    public void test_concat_will_always_serially() throws InterruptedException {
        new Thread(() -> {
            createSlowProducer()
                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-1"))
                    .concatWith(
                            createFastProducer()
                                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-2"))
                    )
                    .subscribe(producedResult -> {
                        out.println("---- consumer got " + producedResult);

                    });
        }, "TestThread").start();

        assertOut("13:22:26.701 [RxNewThreadScheduler-1] SLOW producer begin");
        assertOut("13:22:26.702 [RxNewThreadScheduler-1] SLOW producer sleep a while");
        assertOut("13:22:29.705 [RxNewThreadScheduler-1] SLOW producer notify consumer");
        assertOut("13:22:29.706 [RxNewThreadScheduler-1] ---- consumer got SLOW result");
        assertOut("13:22:29.707 [RxNewThreadScheduler-1] SLOW producer end");
        assertOut("13:22:29.758 [RxNewThreadScheduler-2] FAST producer begin");
        assertOut("13:22:29.758 [RxNewThreadScheduler-2] ---- consumer got FAST result");
        assertOut("13:22:29.759 [RxNewThreadScheduler-2] FAST producer end");
    }
}
