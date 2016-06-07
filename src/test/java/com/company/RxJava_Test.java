package com.company;

import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;

public class RxJava_Test {

    private Observable<String> slowProducer = Observable.create((Subscriber<? super String> consumer) -> {
                sleep(1); //sleep 1ms to just let other thread run so can get fixed output
                println("[SLOW producer] begin");
                println("[SLOW producer] sleep a while");
                sleep(3000);
                println("[SLOW producer] notify consumer");
                consumer.onNext("SLOW result");
                consumer.onCompleted();
                println("[SLOW producer] end");
            }
    );

    private Observable<String> fastProducer = Observable.create((Subscriber<? super String> consumer) -> {
                sleep(50); //sleep 50ms to just let other thread run so can get fixed output
                println("[FAST producer] begin");
                consumer.onNext("FAST result");
                consumer.onCompleted();
                println("[FAST producer] end");
            }
    );

    @Test
    public void test_threading_default_in_current_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowProducer
                    .subscribe(producedResult -> {
                        println("---- consumer got produced result");
                    });

            println("leave test function");
        }, "TestThread").start();

        assertOut("17:02:56.571 @TestThread enter test function");
        assertOut("17:02:56.667 @TestThread [SLOW producer] begin");
        assertOut("17:02:56.667 @TestThread [SLOW producer] sleep a while");
        assertOut("17:02:59.668 @TestThread [SLOW producer] notify consumer");
        assertOut("17:02:59.668 @TestThread ---- consumer got produced result");
        assertOut("17:02:59.668 @TestThread [SLOW producer] end");
        assertOut("17:02:59.669 @TestThread leave test function");
    }

    @Test
    public void test_rx_will_produce_and_consume_in_an_RxScheduler_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowProducer
                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-1")) //cause producer run in new thread
                    .subscribe(producedResult -> {
                        println("---- consumer got produced result");
                    });

            println("leave test function");
        }, "TestThread").start();

        assertOut("11:49:51.169 @TestThread enter test function");
        assertOut("11:49:51.217 @TestThread leave test function");
        assertOut("11:49:51.218 @RxNewThreadScheduler-1 [SLOW producer] begin");
        assertOut("11:49:51.218 @RxNewThreadScheduler-1 [SLOW producer] sleep a while");
        assertOut("11:49:54.221 @RxNewThreadScheduler-1 [SLOW producer] notify consumer");
        assertOut("11:49:54.221 @RxNewThreadScheduler-1 ---- consumer got produced result");
        assertOut("11:49:54.222 @RxNewThreadScheduler-1 [SLOW producer] end");
    }

    @Test
    public void test_rx_will_produce_in_an_RxScheduler_thread_and_consume_in_another_RxScheduler_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowProducer
                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-1")) //cause producer run in new thread
                    .observeOn(createTestScheduler("RxNewThreadScheduler-2")) //cause consumer run in another new thread
                    .subscribe(producedResult -> {
                        sleep(1); //sleep 1ms to just let other thread run so can get fixed output
                        println("---- consumer got produced result");
                    });

            println("leave test function");
        }, "TestThread").start();

        assertOut("12:41:10.523 @TestThread enter test function");
        assertOut("12:41:10.676 @TestThread leave test function");
        assertOut("12:41:10.678 @RxNewThreadScheduler-1 [SLOW producer] begin");
        assertOut("12:41:10.678 @RxNewThreadScheduler-1 [SLOW producer] sleep a while");
        assertOut("12:41:13.682 @RxNewThreadScheduler-1 [SLOW producer] notify consumer");
        assertOut("12:41:13.682 @RxNewThreadScheduler-1 [SLOW producer] end");
        assertOut("12:41:13.683 @RxNewThreadScheduler-2 ---- consumer got produced result");
    }

    @Test
    public void test_rx_will_produce_if_subscribe_called_even_without_callback() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowProducer
                    .subscribe();

            println("leave test function");
        }, "TestThread").start();

        assertOut("17:08:28.050 @TestThread enter test function");
        assertOut("17:08:28.145 @TestThread [SLOW producer] begin");
        assertOut("17:08:28.145 @TestThread [SLOW producer] sleep a while");
        assertOut("17:08:31.145 @TestThread [SLOW producer] notify consumer");
        assertOut("17:08:31.146 @TestThread [SLOW producer] end");
        assertOut("17:08:31.146 @TestThread leave test function");
    }

    @Test
    public void test_rx_will_no_nothing_if_not_subscribed() throws Exception {
        new Thread(() -> {
            println("enter test function");

            Observable.create((Subscriber<? super String> consumer) -> {
                        println("anything can be ok");
                        consumer.onNext("result");
                        consumer.onCompleted();
                    }
            );

            println("leave test function");
        }, "TestThread").start();

        assertOut("17:08:28.050 @TestThread enter test function");
        assertOut("17:08:28.050 @TestThread leave test function");
    }

    @Test
    public void test_merge_in_parallel() throws Exception {
        new Thread(() -> {
            slowProducer
                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-1"))
                    .mergeWith(
                            fastProducer
                                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-2"))
                    )
                    .subscribe(producedResult -> {
                        println("---- consumer got " + producedResult);

                    });
        }, "TestThread").start();

        assertOut("13:21:01.706 @RxNewThreadScheduler-1 [SLOW producer] begin");
        assertOut("13:21:01.711 @RxNewThreadScheduler-1 [SLOW producer] sleep a while");
        assertOut("13:21:01.746 @RxNewThreadScheduler-2 [FAST producer] begin");
        assertOut("13:21:01.746 @RxNewThreadScheduler-2 ---- consumer got FAST result");
        assertOut("13:21:01.748 @RxNewThreadScheduler-2 [FAST producer] end");
        assertOut("13:21:04.713 @RxNewThreadScheduler-1 [SLOW producer] notify consumer");
        assertOut("13:21:04.713 @RxNewThreadScheduler-1 ---- consumer got SLOW result");
        assertOut("13:21:04.713 @RxNewThreadScheduler-1 [SLOW producer] end");
    }

    @Test
    public void test_merge_in_serial_if_not_specify_schedule_thread() throws Exception {
        new Thread(() -> {
            slowProducer
                    .mergeWith(
                            fastProducer
                    )
                    .subscribe(producedResult -> {
                        println("---- consumer got " + producedResult);

                    });
        }, "TestThread").start();

        assertOut("13:24:51.112 @TestThread [SLOW producer] begin");
        assertOut("13:24:51.114 @TestThread [SLOW producer] sleep a while");
        assertOut("13:24:54.118 @TestThread [SLOW producer] notify consumer");
        assertOut("13:24:54.119 @TestThread ---- consumer got SLOW result");
        assertOut("13:24:54.119 @TestThread [SLOW producer] end");
        assertOut("13:24:54.174 @TestThread [FAST producer] begin");
        assertOut("13:24:54.174 @TestThread ---- consumer got FAST result");
        assertOut("13:24:54.174 @TestThread [FAST producer] end");
    }

    @Test
    public void test_concat_will_always_serially() throws Exception {
        new Thread(() -> {
            slowProducer
                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-1"))
                    .concatWith(
                            fastProducer
                                    .subscribeOn(createTestScheduler("RxNewThreadScheduler-2"))
                    )
                    .subscribe(producedResult -> {
                        println("---- consumer got " + producedResult);

                    });
        }, "TestThread").start();

        assertOut("13:22:26.701 @RxNewThreadScheduler-1 [SLOW producer] begin");
        assertOut("13:22:26.702 @RxNewThreadScheduler-1 [SLOW producer] sleep a while");
        assertOut("13:22:29.705 @RxNewThreadScheduler-1 [SLOW producer] notify consumer");
        assertOut("13:22:29.706 @RxNewThreadScheduler-1 ---- consumer got SLOW result");
        assertOut("13:22:29.707 @RxNewThreadScheduler-1 [SLOW producer] end");
        assertOut("13:22:29.758 @RxNewThreadScheduler-2 [FAST producer] begin");
        assertOut("13:22:29.758 @RxNewThreadScheduler-2 ---- consumer got FAST result");
        assertOut("13:22:29.759 @RxNewThreadScheduler-2 [FAST producer] end");
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////


    private static Scheduler createTestScheduler(String name) {
        return Schedulers.from(command -> new Thread(command, name).start());
    }


    private LinkedBlockingQueue<String> outQueue = new LinkedBlockingQueue<>();

    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private void println(String text) {
        text = LocalTime.now().format(timeFormatter) + " @" + Thread.currentThread().getName() + " " + text;
        outQueue.offer(text);
        System.out.println(text);
    }

    private String popLine() throws Exception {
        return outQueue.take();
    }

    private void assertOut(String exceptedStr) throws Exception {
        Assert.assertEquals(exceptedStr.substring(12), popLine().substring(12));
    }

    private static void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
