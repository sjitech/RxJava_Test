package com.company;

import org.junit.Assert;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class RxJava_Test {

    private Observable<String> slowPublisher = Observable.create(subscriber -> {
                sleep(1); //sleep 1ms to just let other thread run so can get predictable output
                println("[SLOW publisher] begin");
                println("[SLOW publisher] do some work");
                sleep(3000);
                println("[SLOW publisher] publish");
                subscriber.onNext("SLOW result");
                subscriber.onCompleted();
                println("[SLOW publisher] end");
            }
    );

    private Observable<String> fastPublisher = Observable.create(subscriber -> {
                sleep(50); //sleep 50ms to just let other thread run so can get predictable output
                println("[FAST publisher] begin");
                subscriber.onNext("FAST result");
                subscriber.onCompleted();
                println("[FAST publisher] end");
            }
    );

    private Observable<StringBuilder> incompatiblePublisher = Observable.create(subscriber -> {
                sleep(50); //sleep 50ms to just let other thread run so can get predictable output
                println("[Incompatible publisher] begin");
                subscriber.onNext(new StringBuilder("incompatible result"));
                subscriber.onCompleted();
                println("[Incompatible publisher] end");
            }
    );

    @Test
    public void test_publisher_subscriber_in_current_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowPublisher
                    .subscribe(result -> {
                        println("---- subscriber got " + result);
                    });

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("17:02:56.571 @CurrentThread enter test function");
        assertOut("17:02:56.667 @CurrentThread [SLOW publisher] begin");
        assertOut("17:02:56.667 @CurrentThread [SLOW publisher] do some work");
        assertOut("17:02:59.668 @CurrentThread [SLOW publisher] publish");
        assertOut("17:02:59.668 @CurrentThread ---- subscriber got SLOW result");
        assertOut("17:02:59.668 @CurrentThread [SLOW publisher] end");
        assertOut("17:02:59.669 @CurrentThread leave test function");
    }

    @Test
    public void test_publisher_in_a_thread_and_subscriber_also_in_same() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowPublisher
                    .subscribeOn(createTestScheduler("RxNewThread-1")) //cause publisher run in new thread
                    .subscribe(result -> {
                        println("---- subscriber got " + result);
                    });

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("11:49:51.169 @CurrentThread enter test function");
        assertOut("11:49:51.217 @CurrentThread leave test function");
        assertOut("11:49:51.218 @RxNewThread-1 [SLOW publisher] begin");
        assertOut("11:49:51.218 @RxNewThread-1 [SLOW publisher] do some work");
        assertOut("11:49:54.221 @RxNewThread-1 [SLOW publisher] publish");
        assertOut("11:49:54.221 @RxNewThread-1 ---- subscriber got SLOW result");
        assertOut("11:49:54.222 @RxNewThread-1 [SLOW publisher] end");
    }

    @Test
    public void test_publisher_in_a_thread_and_subscriber_in_another() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowPublisher
                    .subscribeOn(createTestScheduler("RxNewThread-1")) //cause publisher run in new thread
                    .observeOn(createTestScheduler("RxNewThread-2")) //cause subscriber run in another new thread
                    .subscribe(result -> {
                        sleep(1); //sleep 1ms to just let other thread run so can get predictable output
                        println("---- subscriber got " + result);
                    });

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("12:41:10.523 @CurrentThread enter test function");
        assertOut("12:41:10.676 @CurrentThread leave test function");
        assertOut("12:41:10.678 @RxNewThread-1 [SLOW publisher] begin");
        assertOut("12:41:10.678 @RxNewThread-1 [SLOW publisher] do some work");
        assertOut("12:41:13.682 @RxNewThread-1 [SLOW publisher] publish");
        assertOut("12:41:13.682 @RxNewThread-1 [SLOW publisher] end");
        assertOut("12:41:13.683 @RxNewThread-2 ---- subscriber got SLOW result");
    }

    @Test
    public void test_rx_toBlocking_publisher_subscriber_all_in_current_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            String result = slowPublisher
                    .toBlocking().first();
            println("---- got " + result);

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("00:31:52.775 @CurrentThread enter test function");
        assertOut("00:31:52.792 @CurrentThread [SLOW publisher] begin");
        assertOut("00:31:52.793 @CurrentThread [SLOW publisher] do some work");
        assertOut("00:31:55.798 @CurrentThread [SLOW publisher] publish");
        assertOut("00:31:55.799 @CurrentThread [SLOW publisher] end");
        assertOut("00:31:55.800 @CurrentThread ---- got SLOW result");
        assertOut("00:31:55.800 @CurrentThread leave test function");
    }

    @Test
    public void test_rx_toBlocking_publisher_in_a_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            String result = slowPublisher
                    .subscribeOn(createTestScheduler("RxNewThread-1")) //cause publisher run in new thread
                    .observeOn(createTestScheduler("RxNewThread-2")) //cause subscriber run in another new thread
                    .toBlocking().first();
            println("---- got " + result);

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("00:33:41.042 @CurrentThread enter test function");
        assertOut("00:33:41.210 @RxNewThread-1 [SLOW publisher] begin");
        assertOut("00:33:41.210 @RxNewThread-1 [SLOW publisher] do some work");
        assertOut("00:33:44.212 @RxNewThread-1 [SLOW publisher] publish");
        assertOut("00:33:44.213 @RxNewThread-1 [SLOW publisher] end");
        assertOut("00:33:44.214 @CurrentThread ---- got SLOW result");
        assertOut("00:33:44.214 @CurrentThread leave test function");
    }

    @Test
    public void test_rx_toBlocking_more_complicated_yet_meaningless() throws Exception {
        new Thread(() -> {
            println("enter test function");

            String result = slowPublisher
                    .subscribeOn(createTestScheduler("RxNewThread-1")) //cause publisher run in new thread
                    .observeOn(createTestScheduler("RxNewThread-2")) //cause subscriber run in another new thread
                    .toBlocking().first();
            println("---- got " + result);

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("23:49:54.227 @CurrentThread enter test function");
        assertOut("23:49:54.381 @RxNewThread-1 [SLOW publisher] begin");
        assertOut("23:49:54.381 @RxNewThread-1 [SLOW publisher] do some work");
        assertOut("23:49:57.385 @RxNewThread-1 [SLOW publisher] publish");
        assertOut("23:49:57.386 @RxNewThread-1 [SLOW publisher] end");
        assertOut("23:49:57.387 @CurrentThread ---- got SLOW result");
        assertOut("23:49:57.387 @CurrentThread leave test function");
    }

    @Test
    public void test_toBlocking_simulation() throws Exception {
        new Thread(() -> {
            println("enter test function");

            AtomicReference<String> out_result = new AtomicReference<>();

            slowPublisher
                    .subscribeOn(createTestScheduler("RxNewThread-1")) //cause publisher run in new thread
                    .observeOn(createTestScheduler("RxNewThread-2")) //cause subscriber run in another new thread
                    .subscribe(result -> {
                        sleep(1); //sleep 1ms to just let other thread run so can get predictable output
                        println("---- subscriber got " + result);
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
        assertOut("00:35:23.019 @RxNewThread-1 [SLOW publisher] begin");
        assertOut("00:35:23.019 @RxNewThread-1 [SLOW publisher] do some work");
        assertOut("00:35:26.023 @RxNewThread-1 [SLOW publisher] publish");
        assertOut("00:35:26.024 @RxNewThread-1 [SLOW publisher] end");
        assertOut("00:35:26.026 @RxNewThread-2 ---- subscriber got SLOW result");
        assertOut("00:35:26.026 @CurrentThread ---- got SLOW result");
        assertOut("00:35:26.027 @CurrentThread leave test function");
    }

    @Test
    public void test_merge_in_parallel_but_result_order_not_predictable() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowPublisher
                    .subscribeOn(createTestScheduler("RxNewThread-1"))
                    .mergeWith(
                            fastPublisher
                                    .subscribeOn(createTestScheduler("RxNewThread-2"))
                    )
                    .subscribe(result -> {
                        println("---- subscriber got " + result);

                    });

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("10:50:56.507 @CurrentThread enter test function");
        assertOut("10:50:56.637 @CurrentThread leave test function");
        assertOut("10:50:56.638 @RxNewThread-1 [SLOW publisher] begin");
        assertOut("10:50:56.638 @RxNewThread-1 [SLOW publisher] do some work");
        assertOut("10:50:56.690 @RxNewThread-2 [FAST publisher] begin");
        assertOut("10:50:56.690 @RxNewThread-2 ---- subscriber got FAST result");
        assertOut("10:50:56.691 @RxNewThread-2 [FAST publisher] end");
        assertOut("10:50:59.640 @RxNewThread-1 [SLOW publisher] publish");
        assertOut("10:50:59.640 @RxNewThread-1 ---- subscriber got SLOW result");
        assertOut("10:50:59.641 @RxNewThread-1 [SLOW publisher] end");
    }

    @Test
    public void test_merge_in_serial_if_not_specify_schedule_thread() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowPublisher
                    .mergeWith(
                            fastPublisher
                    )
                    .subscribe(result -> {
                        println("---- subscriber got " + result);

                    });

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("10:52:59.765 @CurrentThread enter test function");
        assertOut("10:52:59.846 @CurrentThread [SLOW publisher] begin");
        assertOut("10:52:59.846 @CurrentThread [SLOW publisher] do some work");
        assertOut("10:53:02.849 @CurrentThread [SLOW publisher] publish");
        assertOut("10:53:02.850 @CurrentThread ---- subscriber got SLOW result");
        assertOut("10:53:02.850 @CurrentThread [SLOW publisher] end");
        assertOut("10:53:02.904 @CurrentThread [FAST publisher] begin");
        assertOut("10:53:02.904 @CurrentThread ---- subscriber got FAST result");
        assertOut("10:53:02.904 @CurrentThread [FAST publisher] end");
        assertOut("10:53:02.905 @CurrentThread leave test function");
    }

    @Test
    public void test_concat_will_always_serially_so_predictable_order() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowPublisher
                    .subscribeOn(createTestScheduler("RxNewThread-1"))
                    .concatWith(
                            fastPublisher
                                    .subscribeOn(createTestScheduler("RxNewThread-2"))
                    )
                    .subscribe(result -> {
                        println("---- subscriber got " + result);

                    });

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("10:53:55.314 @CurrentThread enter test function");
        assertOut("10:53:55.454 @CurrentThread leave test function");
        assertOut("10:53:55.456 @RxNewThread-1 [SLOW publisher] begin");
        assertOut("10:53:55.456 @RxNewThread-1 [SLOW publisher] do some work");
        assertOut("10:53:58.461 @RxNewThread-1 [SLOW publisher] publish");
        assertOut("10:53:58.461 @RxNewThread-1 ---- subscriber got SLOW result");
        assertOut("10:53:58.463 @RxNewThread-1 [SLOW publisher] end");
        assertOut("10:53:58.516 @RxNewThread-2 [FAST publisher] begin");
        assertOut("10:53:58.516 @RxNewThread-2 ---- subscriber got FAST result");
        assertOut("10:53:58.517 @RxNewThread-2 [FAST publisher] end");
    }

    @Test
    public void test_merge_different_type_rx_by_self_idea() throws Exception {
        new Thread(() -> {
            println("enter test function");

            AtomicReference<String> out_result1 = new AtomicReference<>();
            AtomicReference<StringBuilder> out_result2 = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(2);

            slowPublisher
                    .subscribeOn(createTestScheduler("RxNewThread-1"))
                    .doOnNext(out_result1::set) //same as result -> out_result1.set(result)
                    .doOnCompleted(latch::countDown) //same as () -> latch.countDown()
                    .subscribe();

            incompatiblePublisher
                    .subscribeOn(createTestScheduler("RxNewThread-2"))
                    .doOnNext(out_result2::set)
                    .doOnCompleted(latch::countDown)
                    .subscribe();

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            sleep(1); //sleep 1
            println("---- got " + out_result1.get() + " and " + out_result2.get());

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("11:12:36.037 @CurrentThread enter test function");
        assertOut("11:12:36.122 @RxNewThread-1 [SLOW publisher] begin");
        assertOut("11:12:36.123 @RxNewThread-1 [SLOW publisher] do some work");
        assertOut("11:12:36.172 @RxNewThread-2 [Incompatible publisher] begin");
        assertOut("11:12:36.174 @RxNewThread-2 [Incompatible publisher] end");
        assertOut("11:12:39.127 @RxNewThread-1 [SLOW publisher] publish");
        assertOut("11:12:39.127 @RxNewThread-1 [SLOW publisher] end");
        assertOut("11:12:39.128 @CurrentThread ---- got SLOW result and incompatible result");
        assertOut("11:12:39.128 @CurrentThread leave test function");
    }

    @Test
    public void test_merge_different_type_rx() throws Exception {
        new Thread(() -> {
            println("enter test function");

            AtomicReference<String> out_result1 = new AtomicReference<>();
            AtomicReference<StringBuilder> out_result2 = new AtomicReference<>();

            slowPublisher
                    .subscribeOn(createTestScheduler("RxNewThread-1"))
                    .doOnNext(out_result1::set) //same as result -> out_result1.set(result)
                    .toCompletable() //so can mergeWith other type rx
                    .mergeWith(
                            incompatiblePublisher
                                    .subscribeOn(createTestScheduler("RxNewThread-2"))
                                    .doOnNext(out_result2::set)
                                    .toCompletable()
                    )
                    .toObservable()
                    .toBlocking()
                    .subscribe();

            sleep(1); //sleep 1ms to let other thread run so can get predictable output
            println("---- got " + out_result1.get() + " and " + out_result2.get());

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("19:35:08.299 @CurrentThread enter test function");
        assertOut("19:35:08.358 @RxNewThread-1 [SLOW publisher] begin");
        assertOut("19:35:08.359 @RxNewThread-1 [SLOW publisher] do some work");
        assertOut("19:35:08.410 @RxNewThread-2 [Incompatible publisher] begin");
        assertOut("19:35:08.414 @RxNewThread-2 [Incompatible publisher] end");
        assertOut("19:35:11.364 @RxNewThread-1 [SLOW publisher] publish");
        assertOut("19:35:11.364 @RxNewThread-1 [SLOW publisher] end");
        assertOut("19:35:11.364 @CurrentThread ---- got SLOW result and incompatible result");
        assertOut("19:35:11.364 @CurrentThread leave test function");
    }

    @Test
    public void test_rx_will_produce_if_subscribe_called_even_without_callback() throws Exception {
        new Thread(() -> {
            println("enter test function");

            slowPublisher
                    .subscribe();

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("17:08:28.050 @CurrentThread enter test function");
        assertOut("17:08:28.145 @CurrentThread [SLOW publisher] begin");
        assertOut("17:08:28.145 @CurrentThread [SLOW publisher] do some work");
        assertOut("17:08:31.145 @CurrentThread [SLOW publisher] publish");
        assertOut("17:08:31.146 @CurrentThread [SLOW publisher] end");
        assertOut("17:08:31.146 @CurrentThread leave test function");
    }

    @Test
    public void test_rx_will_no_nothing_if_not_subscribed() throws Exception {
        new Thread(() -> {
            println("enter test function");

            Observable.create(subscriber -> {
                        println("anything can be ok");
                        subscriber.onNext("result");
                        subscriber.onCompleted();
                    }
            );

            println("leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("17:08:28.050 @CurrentThread enter test function");
        assertOut("17:08:28.050 @CurrentThread leave test function");
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////


    private static Scheduler createTestScheduler(String threadName) {
        //Normally, you can just return Schedulers.io() etc to get a cached thread from pool.
        //In this test program, i want get predictable thread name, so use customized implementation
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
