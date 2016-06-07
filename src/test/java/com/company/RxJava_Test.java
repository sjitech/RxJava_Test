package com.company;

import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.lang.ref.ReferenceQueue;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

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
                    .subscribe(result -> {
                        println("---- consumer got " + result);
                    });

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("17:02:56.571 @CurrentThread enter test function");
        assertOut("17:02:56.667 @CurrentThread [SLOW producer] begin");
        assertOut("17:02:56.667 @CurrentThread [SLOW producer] sleep a while");
        assertOut("17:02:59.668 @CurrentThread [SLOW producer] notify consumer");
        assertOut("17:02:59.668 @CurrentThread ---- consumer got SLOW result");
        assertOut("17:02:59.668 @CurrentThread [SLOW producer] end");
        assertOut("17:02:59.669 @CurrentThread leave test function");
    }

    @Test
    public void test_rx_will_produce_in_an_RxScheduler_thread_and_consume_in_same_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowProducer
                    .subscribeOn(createTestScheduler("RxNewThread-1")) //cause producer run in new thread
                    .subscribe(result -> {
                        println("---- consumer got " + result);
                    });

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("11:49:51.169 @CurrentThread enter test function");
        assertOut("11:49:51.217 @CurrentThread leave test function");
        assertOut("11:49:51.218 @RxNewThread-1 [SLOW producer] begin");
        assertOut("11:49:51.218 @RxNewThread-1 [SLOW producer] sleep a while");
        assertOut("11:49:54.221 @RxNewThread-1 [SLOW producer] notify consumer");
        assertOut("11:49:54.221 @RxNewThread-1 ---- consumer got SLOW result");
        assertOut("11:49:54.222 @RxNewThread-1 [SLOW producer] end");
    }

    @Test
    public void test_rx_will_produce_in_an_thread_and_consume_in_another_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowProducer
                    .subscribeOn(createTestScheduler("RxNewThread-1")) //cause producer run in new thread
                    .observeOn(createTestScheduler("RxNewThread-2")) //cause consumer run in another new thread
                    .subscribe(result -> {
                        sleep(1); //sleep 1ms to just let other thread run so can get fixed output
                        println("---- consumer got " + result);
                    });

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("12:41:10.523 @CurrentThread enter test function");
        assertOut("12:41:10.676 @CurrentThread leave test function");
        assertOut("12:41:10.678 @RxNewThread-1 [SLOW producer] begin");
        assertOut("12:41:10.678 @RxNewThread-1 [SLOW producer] sleep a while");
        assertOut("12:41:13.682 @RxNewThread-1 [SLOW producer] notify consumer");
        assertOut("12:41:13.682 @RxNewThread-1 [SLOW producer] end");
        assertOut("12:41:13.683 @RxNewThread-2 ---- consumer got SLOW result");
    }

    @Test
    public void test_rx_toBlocking_publisher_consumer_all_in_current_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            String result = slowProducer
                    .toBlocking().first();
            println("---- got " + result);

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("00:31:52.775 @CurrentThread enter test function");
        assertOut("00:31:52.792 @CurrentThread [SLOW producer] begin");
        assertOut("00:31:52.793 @CurrentThread [SLOW producer] sleep a while");
        assertOut("00:31:55.798 @CurrentThread [SLOW producer] notify consumer");
        assertOut("00:31:55.799 @CurrentThread [SLOW producer] end");
        assertOut("00:31:55.800 @CurrentThread ---- got SLOW result");
        assertOut("00:31:55.800 @CurrentThread leave test function");
    }

    @Test
    public void test_rx_toBlocking_publisher_in_a_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            String result = slowProducer
                    .subscribeOn(createTestScheduler("RxNewThread-1")) //cause producer run in new thread
                    .observeOn(createTestScheduler("RxNewThread-2")) //cause consumer run in another new thread
                    .toBlocking().first();
            println("---- got " + result);

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("00:33:41.042 @CurrentThread enter test function");
        assertOut("00:33:41.210 @RxNewThread-1 [SLOW producer] begin");
        assertOut("00:33:41.210 @RxNewThread-1 [SLOW producer] sleep a while");
        assertOut("00:33:44.212 @RxNewThread-1 [SLOW producer] notify consumer");
        assertOut("00:33:44.213 @RxNewThread-1 [SLOW producer] end");
        assertOut("00:33:44.214 @CurrentThread ---- got SLOW result");
        assertOut("00:33:44.214 @CurrentThread leave test function");
    }

    @Test
    public void test_rx_toBlocking_more_complicated_yet_meaningless() throws Exception {
        new Thread(() -> {
            println("enter test function");

            String result = slowProducer
                    .subscribeOn(createTestScheduler("RxNewThread-1")) //cause producer run in new thread
                    .observeOn(createTestScheduler("RxNewThread-2")) //cause consumer run in another new thread
                    .toBlocking().first();
            println("---- got " + result);

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("23:49:54.227 @CurrentThread enter test function");
        assertOut("23:49:54.381 @RxNewThread-1 [SLOW producer] begin");
        assertOut("23:49:54.381 @RxNewThread-1 [SLOW producer] sleep a while");
        assertOut("23:49:57.385 @RxNewThread-1 [SLOW producer] notify consumer");
        assertOut("23:49:57.386 @RxNewThread-1 [SLOW producer] end");
        assertOut("23:49:57.387 @CurrentThread ---- got SLOW result");
        assertOut("23:49:57.387 @CurrentThread leave test function");
    }

    @Test
    public void test_toBlocking_simulation() throws Exception {
        new Thread(() -> {
            println("enter test function");

            AtomicReference<String> out_result = new AtomicReference<String>();

            slowProducer
                    .subscribeOn(createTestScheduler("RxNewThread-1")) //cause producer run in new thread
                    .observeOn(createTestScheduler("RxNewThread-2")) //cause consumer run in another new thread
                    .subscribe(result -> {
                        sleep(1); //sleep 1ms to just let other thread run so can get fixed output
                        println("---- consumer got " + result);
                        out_result.set(result);

                        synchronized (out_result) {
                            out_result.notifyAll();
                        }
                    });

            synchronized (out_result) {
                try {
                    out_result.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            println("---- got " + out_result.get());

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("00:35:22.900 @CurrentThread enter test function");
        assertOut("00:35:23.019 @RxNewThread-1 [SLOW producer] begin");
        assertOut("00:35:23.019 @RxNewThread-1 [SLOW producer] sleep a while");
        assertOut("00:35:26.023 @RxNewThread-1 [SLOW producer] notify consumer");
        assertOut("00:35:26.024 @RxNewThread-1 [SLOW producer] end");
        assertOut("00:35:26.026 @RxNewThread-2 ---- consumer got SLOW result");
        assertOut("00:35:26.026 @CurrentThread ---- got SLOW result");
        assertOut("00:35:26.027 @CurrentThread leave test function");
    }

    @Test
    public void test_rx_will_produce_if_subscribe_called_even_without_callback() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowProducer
                    .subscribe();

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("17:08:28.050 @CurrentThread enter test function");
        assertOut("17:08:28.145 @CurrentThread [SLOW producer] begin");
        assertOut("17:08:28.145 @CurrentThread [SLOW producer] sleep a while");
        assertOut("17:08:31.145 @CurrentThread [SLOW producer] notify consumer");
        assertOut("17:08:31.146 @CurrentThread [SLOW producer] end");
        assertOut("17:08:31.146 @CurrentThread leave test function");
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
        }, "CurrentThread" /*threadName*/).start();

        assertOut("17:08:28.050 @CurrentThread enter test function");
        assertOut("17:08:28.050 @CurrentThread leave test function");
    }

    @Test
    public void test_merge_in_parallel() throws Exception {
        new Thread(() -> {
            slowProducer
                    .subscribeOn(createTestScheduler("RxNewThread-1"))
                    .mergeWith(
                            fastProducer
                                    .subscribeOn(createTestScheduler("RxNewThread-2"))
                    )
                    .subscribe(result -> {
                        println("---- consumer got " + result);

                    });
        }, "CurrentThread" /*threadName*/).start();

        assertOut("13:21:01.706 @RxNewThread-1 [SLOW producer] begin");
        assertOut("13:21:01.711 @RxNewThread-1 [SLOW producer] sleep a while");
        assertOut("13:21:01.746 @RxNewThread-2 [FAST producer] begin");
        assertOut("13:21:01.746 @RxNewThread-2 ---- consumer got FAST result");
        assertOut("13:21:01.748 @RxNewThread-2 [FAST producer] end");
        assertOut("13:21:04.713 @RxNewThread-1 [SLOW producer] notify consumer");
        assertOut("13:21:04.713 @RxNewThread-1 ---- consumer got SLOW result");
        assertOut("13:21:04.713 @RxNewThread-1 [SLOW producer] end");
    }

    @Test
    public void test_merge_in_serial_if_not_specify_schedule_thread() throws Exception {
        new Thread(() -> {
            slowProducer
                    .mergeWith(
                            fastProducer
                    )
                    .subscribe(result -> {
                        println("---- consumer got " + result);

                    });
        }, "CurrentThread" /*threadName*/).start();

        assertOut("13:24:51.112 @CurrentThread [SLOW producer] begin");
        assertOut("13:24:51.114 @CurrentThread [SLOW producer] sleep a while");
        assertOut("13:24:54.118 @CurrentThread [SLOW producer] notify consumer");
        assertOut("13:24:54.119 @CurrentThread ---- consumer got SLOW result");
        assertOut("13:24:54.119 @CurrentThread [SLOW producer] end");
        assertOut("13:24:54.174 @CurrentThread [FAST producer] begin");
        assertOut("13:24:54.174 @CurrentThread ---- consumer got FAST result");
        assertOut("13:24:54.174 @CurrentThread [FAST producer] end");
    }

    @Test
    public void test_concat_will_always_serially() throws Exception {
        new Thread(() -> {
            slowProducer
                    .subscribeOn(createTestScheduler("RxNewThread-1"))
                    .concatWith(
                            fastProducer
                                    .subscribeOn(createTestScheduler("RxNewThread-2"))
                    )
                    .subscribe(result -> {
                        println("---- consumer got " + result);

                    });
        }, "CurrentThread" /*threadName*/).start();

        assertOut("13:22:26.701 @RxNewThread-1 [SLOW producer] begin");
        assertOut("13:22:26.702 @RxNewThread-1 [SLOW producer] sleep a while");
        assertOut("13:22:29.705 @RxNewThread-1 [SLOW producer] notify consumer");
        assertOut("13:22:29.706 @RxNewThread-1 ---- consumer got SLOW result");
        assertOut("13:22:29.707 @RxNewThread-1 [SLOW producer] end");
        assertOut("13:22:29.758 @RxNewThread-2 [FAST producer] begin");
        assertOut("13:22:29.758 @RxNewThread-2 ---- consumer got FAST result");
        assertOut("13:22:29.759 @RxNewThread-2 [FAST producer] end");
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////


    private static Scheduler createTestScheduler(String threadName) {
        //just for test, get predictable thread name instead unpredictable thread pool name
        return Schedulers.from(command -> new Thread(command, threadName).start());
    }


    private LinkedBlockingQueue<String> outQueue = new LinkedBlockingQueue<>();

    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private void println(String text) {
        text = LocalTime.now().format(timeFormatter) + " @" + Thread.currentThread().getName() + " " + text;
        outQueue.offer(text);
        System.out.println(text);
    }

    private String popLine() throws InterruptedException {
        return outQueue.take();
    }

    private void assertOut(String exceptedStr) throws InterruptedException {
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
