package com.company;

import org.junit.Assert;
import org.junit.Test;
import rx.*;
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
                sleep(5); //sleep 5ms to just let other thread run so can get predictable output
                println("[FAST publisher] begin");
                subscriber.onNext("FAST result");
                subscriber.onCompleted();
                println("[FAST publisher] end");
            }
    );

    private Observable<StringBuilder> incompatiblePublisher = Observable.create(subscriber -> {
                sleep(10); //sleep 10ms to just let other thread run so can get predictable output
                println("[Incompatible publisher] begin");
                println("[Incompatible publisher] do some work");
                sleep(2000);
                subscriber.onNext(new StringBuilder("Incompatible result"));
                subscriber.onCompleted();
                println("[Incompatible publisher] end");
            }
    );

    private Observable<String> errorPublisher = Observable.create(subscriber -> {
        println(((String) null).toString()); //this publisher will throw throw RuntimeException
    });

    private Observable<Number> buggyPublisher = Observable.create(subscriber -> {
        sleep(15); //sleep 10ms to just let other thread run so can get predictable output
        println("[Buggy publisher] begin");
        println(((String) null).toString()); //this publisher will throw throw RuntimeException
    });

    private Scheduler schedulerForWorkThread1 = createScheduler("RxWorkThread1");
    private Scheduler schedulerForWorkThread2 = createScheduler("RxWorkThread2");
    private Scheduler schedulerForWorkThread3 = createScheduler("RxWorkThread3");

    @Test
    public void test_publisher_subscriber_in_current_thread() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            slowPublisher
                    .subscribe(result -> {
                        println("---- subscriber got " + result);
                    });

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("17:02:56.571 @CurrentThread Enter test function");
        assertOut("17:02:56.667 @CurrentThread [SLOW publisher] begin");
        assertOut("17:02:56.667 @CurrentThread [SLOW publisher] do some work");
        assertOut("17:02:59.668 @CurrentThread [SLOW publisher] publish");
        assertOut("17:02:59.668 @CurrentThread ---- subscriber got SLOW result");
        assertOut("17:02:59.668 @CurrentThread [SLOW publisher] end");
        assertOut("17:02:59.669 @CurrentThread Leave test function");
    }

    @Test
    public void test_publisher_in_a_thread_and_subscriber_also_in_same() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            slowPublisher
                    .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                    .subscribe(result -> {
                        println("---- subscriber got " + result);
                    });

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("11:49:51.169 @CurrentThread Enter test function");
        assertOut("11:49:51.217 @CurrentThread Leave test function");
        assertOut("11:49:51.218 @RxWorkThread1 [SLOW publisher] begin");
        assertOut("11:49:51.218 @RxWorkThread1 [SLOW publisher] do some work");
        assertOut("11:49:54.221 @RxWorkThread1 [SLOW publisher] publish");
        assertOut("11:49:54.221 @RxWorkThread1 ---- subscriber got SLOW result");
        assertOut("11:49:54.222 @RxWorkThread1 [SLOW publisher] end");
    }

    @Test
    public void test_publisher_in_a_thread_and_subscriber_in_another() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            slowPublisher
                    .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                    .observeOn(schedulerForWorkThread2) //cause subscriber run in another new thread
                    .subscribe(result -> {
                        sleep(1); //sleep 1ms to just let other thread run so can get predictable output
                        println("---- subscriber got " + result);
                    });

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("12:41:10.523 @CurrentThread Enter test function");
        assertOut("12:41:10.676 @CurrentThread Leave test function");
        assertOut("12:41:10.678 @RxWorkThread1 [SLOW publisher] begin");
        assertOut("12:41:10.678 @RxWorkThread1 [SLOW publisher] do some work");
        assertOut("12:41:13.682 @RxWorkThread1 [SLOW publisher] publish");
        assertOut("12:41:13.682 @RxWorkThread1 [SLOW publisher] end");
        assertOut("12:41:13.683 @RxWorkThread2 ---- subscriber got SLOW result");
    }

    @Test
    public void test_rx_toBlocking_publisher_subscriber_all_in_current_thread() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            String result = slowPublisher
                    .toBlocking().first();

            println("---- got " + result);

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("00:31:52.775 @CurrentThread Enter test function");
        assertOut("00:31:52.792 @CurrentThread [SLOW publisher] begin");
        assertOut("00:31:52.793 @CurrentThread [SLOW publisher] do some work");
        assertOut("00:31:55.798 @CurrentThread [SLOW publisher] publish");
        assertOut("00:31:55.799 @CurrentThread [SLOW publisher] end");
        assertOut("00:31:55.800 @CurrentThread ---- got SLOW result");
        assertOut("00:31:55.800 @CurrentThread Leave test function");
    }

    @Test
    public void test_rx_toBlocking_publisher_in_a_thread() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            String result = slowPublisher
                    .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                    .toBlocking().first();

            sleep(1); //sleep 1ms to let other thread run so can get predictable output
            println("---- got " + result);

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("00:33:41.042 @CurrentThread Enter test function");
        assertOut("00:33:41.210 @RxWorkThread1 [SLOW publisher] begin");
        assertOut("00:33:41.210 @RxWorkThread1 [SLOW publisher] do some work");
        assertOut("00:33:44.212 @RxWorkThread1 [SLOW publisher] publish");
        assertOut("00:33:44.213 @RxWorkThread1 [SLOW publisher] end");
        assertOut("00:33:44.214 @CurrentThread ---- got SLOW result");
        assertOut("00:33:44.214 @CurrentThread Leave test function");
    }

    @Test
    public void test_toBlocking_simulation() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            AtomicReference<String> out_result = new AtomicReference<>();

            slowPublisher
                    .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                    .observeOn(schedulerForWorkThread2) //cause subscriber run in another new thread
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

            sleep(1); //sleep 1ms to let other thread run so can get predictable output
            println("---- got " + out_result.get());

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("00:35:22.900 @CurrentThread Enter test function");
        assertOut("00:35:23.019 @RxWorkThread1 [SLOW publisher] begin");
        assertOut("00:35:23.019 @RxWorkThread1 [SLOW publisher] do some work");
        assertOut("00:35:26.023 @RxWorkThread1 [SLOW publisher] publish");
        assertOut("00:35:26.024 @RxWorkThread1 [SLOW publisher] end");
        assertOut("00:35:26.026 @RxWorkThread2 ---- subscriber got SLOW result");
        assertOut("00:35:26.027 @CurrentThread ---- got SLOW result");
        assertOut("00:35:26.028 @CurrentThread Leave test function");
    }

    @Test
    public void test_merge_in_parallel_but_result_order_not_predictable() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            slowPublisher
                    .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                    .mergeWith(
                            fastPublisher
                                    .subscribeOn(schedulerForWorkThread2)
                    )
                    .subscribe(result -> {
                        println("---- subscriber got " + result);

                    });

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("10:50:56.507 @CurrentThread Enter test function");
        assertOut("10:50:56.637 @CurrentThread Leave test function");
        assertOut("10:50:56.638 @RxWorkThread1 [SLOW publisher] begin");
        assertOut("10:50:56.638 @RxWorkThread1 [SLOW publisher] do some work");
        assertOut("10:50:56.690 @RxWorkThread2 [FAST publisher] begin");
        assertOut("10:50:56.690 @RxWorkThread2 ---- subscriber got FAST result");
        assertOut("10:50:56.691 @RxWorkThread2 [FAST publisher] end");
        assertOut("10:50:59.640 @RxWorkThread1 [SLOW publisher] publish");
        assertOut("10:50:59.640 @RxWorkThread1 ---- subscriber got SLOW result");
        assertOut("10:50:59.641 @RxWorkThread1 [SLOW publisher] end");
    }

    @Test
    public void test_merge_in_serial_if_not_specify_schedule_thread() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            slowPublisher
                    .mergeWith(
                            fastPublisher
                    )
                    .subscribe(result -> {
                        println("---- subscriber got " + result);

                    });

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("10:52:59.765 @CurrentThread Enter test function");
        assertOut("10:52:59.846 @CurrentThread [SLOW publisher] begin");
        assertOut("10:52:59.846 @CurrentThread [SLOW publisher] do some work");
        assertOut("10:53:02.849 @CurrentThread [SLOW publisher] publish");
        assertOut("10:53:02.850 @CurrentThread ---- subscriber got SLOW result");
        assertOut("10:53:02.850 @CurrentThread [SLOW publisher] end");
        assertOut("10:53:02.904 @CurrentThread [FAST publisher] begin");
        assertOut("10:53:02.904 @CurrentThread ---- subscriber got FAST result");
        assertOut("10:53:02.904 @CurrentThread [FAST publisher] end");
        assertOut("10:53:02.905 @CurrentThread Leave test function");
    }

    @Test
    public void test_concat_will_always_serially_so_predictable_order() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            slowPublisher
                    .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                    .concatWith(
                            fastPublisher
                                    .subscribeOn(schedulerForWorkThread2)
                    )
                    .subscribe(result -> {
                        println("---- subscriber got " + result);

                    });

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("10:53:55.314 @CurrentThread Enter test function");
        assertOut("10:53:55.454 @CurrentThread Leave test function");
        assertOut("10:53:55.456 @RxWorkThread1 [SLOW publisher] begin");
        assertOut("10:53:55.456 @RxWorkThread1 [SLOW publisher] do some work");
        assertOut("10:53:58.461 @RxWorkThread1 [SLOW publisher] publish");
        assertOut("10:53:58.461 @RxWorkThread1 ---- subscriber got SLOW result");
        assertOut("10:53:58.463 @RxWorkThread1 [SLOW publisher] end");
        assertOut("10:53:58.516 @RxWorkThread2 [FAST publisher] begin");
        assertOut("10:53:58.516 @RxWorkThread2 ---- subscriber got FAST result");
        assertOut("10:53:58.517 @RxWorkThread2 [FAST publisher] end");
    }

    private static class RxResult<T> {
        T result;
        Throwable error;
    }

    private static class RxResultAndSignal<T> extends RxResult<T> {
        CountDownLatch latch = new CountDownLatch(1);
    }

    @Test
    public void test_merge_3_different_type_rx_by_self_idea2() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            RxResultAndSignal<String> r1 = new RxResultAndSignal<>();
            RxResultAndSignal<StringBuilder> r2 = new RxResultAndSignal<>();
            RxResultAndSignal<Number> r3 = new RxResultAndSignal<>();

            slowPublisher
                    .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                    .subscribe(result -> {
                        r1.result = result;
                    }, e -> { //onError
                        r1.error = e;
                        r1.latch.countDown();
                    }, () -> { //onComplete
                        r1.latch.countDown();
                    });

            incompatiblePublisher
                    .subscribeOn(schedulerForWorkThread2)
                    .subscribe(result -> {
                        r2.result = result;
                    }, e -> { //onError
                        r2.error = e;
                        r2.latch.countDown();
                    }, () -> { //onComplete
                        r2.latch.countDown();
                    });

            buggyPublisher
                    .subscribeOn(schedulerForWorkThread3)
                    .subscribe(result -> {
                        r3.result = result;
                    }, e -> { //onError
                        r3.error = e;
                        r3.latch.countDown();
                    }, () -> { //onComplete
                        r3.latch.countDown();
                    });

            try {
                r1.latch.await();
                r2.latch.await();
                r3.latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            sleep(1); //sleep 1ms to let other thread run so can get predictable output
            println("---- got all result: {" + r1.result + "}, {" + r2.result + "}, {" + r3.result + "}");
            println("---- got all error: {" + r1.error + "}, {" + r2.error + "}, {" + r3.error + "}");

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("13:27:55.033 @CurrentThread Enter test function");
        assertOut("13:27:55.065 @RxWorkThread1 [SLOW publisher] begin");
        assertOut("13:27:55.065 @RxWorkThread1 [SLOW publisher] do some work");
        assertOut("13:27:55.082 @RxWorkThread2 [Incompatible publisher] begin");
        assertOut("13:27:55.082 @RxWorkThread2 [Incompatible publisher] do some work");
        assertOut("13:27:55.086 @RxWorkThread3 [Buggy publisher] begin");
        assertOut("13:27:57.084 @RxWorkThread2 [Incompatible publisher] end");
        assertOut("13:27:58.069 @RxWorkThread1 [SLOW publisher] publish");
        assertOut("13:27:58.070 @RxWorkThread1 [SLOW publisher] end");
        assertOut("13:27:58.071 @CurrentThread ---- got all result: {SLOW result}, {Incompatible result}, {null}");
        assertOut("13:27:58.071 @CurrentThread ---- got all error: {null}, {null}, {java.lang.NullPointerException}");
        assertOut("13:27:58.071 @CurrentThread Leave test function");
    }

    @Test
    public void test_merge_3_different_type_rx() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            RxResult<String> r1 = new RxResult<>();
            RxResult<StringBuilder> r2 = new RxResult<>();
            RxResult<Number> r3 = new RxResult<>();

            Completable.merge(
                    slowPublisher
                            .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                            .doOnNext(result -> r1.result = result)
                            .doOnError(e -> r1.error = e)
                            .toCompletable() //so can mergeWith other type rx
                            .onErrorComplete() //auto call complete when error
                    ,
                    incompatiblePublisher
                            .subscribeOn(schedulerForWorkThread2)
                            .doOnNext(result -> r2.result = result)
                            .doOnError(e -> r2.error = e)
                            .toCompletable() //so can mergeWith other type rx
                            .onErrorComplete() //auto call complete when error
                    ,
                    buggyPublisher
                            .subscribeOn(schedulerForWorkThread3)
                            .doOnNext(result -> r3.result = result)
                            .doOnError(e -> r3.error = e)
                            .toCompletable() //so can mergeWith other type rx
                            .onErrorComplete() //auto call complete when error
            )
                    .await(/*can specify total timeout*/);

            sleep(1); //sleep 1ms to let other thread run so can get predictable output
            println("---- got all result: {" + r1.result + "}, {" + r2.result + "}, {" + r3.result + "}");
            println("---- got all error: {" + r1.error + "}, {" + r2.error + "}, {" + r3.error + "}");

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("13:47:50.789 @CurrentThread Enter test function");
        assertOut("13:47:50.852 @RxWorkThread1 [SLOW publisher] begin");
        assertOut("13:47:50.852 @RxWorkThread1 [SLOW publisher] do some work");
        assertOut("13:47:50.862 @RxWorkThread2 [Incompatible publisher] begin");
        assertOut("13:47:50.863 @RxWorkThread2 [Incompatible publisher] do some work");
        assertOut("13:47:50.869 @RxWorkThread3 [Buggy publisher] begin");
        assertOut("13:47:52.868 @RxWorkThread2 [Incompatible publisher] end");
        assertOut("13:47:53.854 @RxWorkThread1 [SLOW publisher] publish");
        assertOut("13:47:53.854 @RxWorkThread1 [SLOW publisher] end");
        assertOut("13:47:53.856 @CurrentThread ---- got all result: {SLOW result}, {Incompatible result}, {null}");
        assertOut("13:47:53.856 @CurrentThread ---- got all error: {null}, {null}, {java.lang.NullPointerException}");
    }

    @Test
    public void test_rx_will_produce_if_subscribe_called_even_without_callback() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            slowPublisher
                    .subscribe();

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("17:08:28.050 @CurrentThread Enter test function");
        assertOut("17:08:28.145 @CurrentThread [SLOW publisher] begin");
        assertOut("17:08:28.145 @CurrentThread [SLOW publisher] do some work");
        assertOut("17:08:31.145 @CurrentThread [SLOW publisher] publish");
        assertOut("17:08:31.146 @CurrentThread [SLOW publisher] end");
        assertOut("17:08:31.146 @CurrentThread Leave test function");
    }

    @Test
    public void test_rx_will_do_nothing_if_not_subscribed() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            Observable.create(subscriber -> println("anything can be ok"));

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("17:08:28.050 @CurrentThread Enter test function");
        assertOut("17:08:28.050 @CurrentThread Leave test function");
    }

    @Test
    public void test_exception_in_blocking_mode_will_always_throw_out_in_current_thread() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            try {
                errorPublisher
                        .toBlocking().first();
            } catch (Exception e) {
                println("---- test1: " + e);
            }

            try {
                errorPublisher
                        .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                        .toBlocking().first();
            } catch (Exception e) {
                println("---- test2: " + e);
            }

            try {
                errorPublisher
                        .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                        .doOnError(e -> println("-------- " + e))
                        .toBlocking().first();
            } catch (Exception e) {
                println("---- test3: " + e);
            }

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("14:38:43.078 @CurrentThread Enter test function");
        assertOut("14:38:43.113 @CurrentThread ---- test1: java.lang.NullPointerException");
        assertOut("14:38:43.126 @CurrentThread ---- test2: java.lang.NullPointerException");
        assertOut("14:38:43.152 @RxWorkThread1 -------- java.lang.NullPointerException");
        assertOut("14:38:43.152 @CurrentThread ---- test3: java.lang.NullPointerException");
        assertOut("14:38:43.152 @CurrentThread Leave test function");
    }

    @Test
    public void test_exception_without_on_error_handler() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            try {
                errorPublisher
                        .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                        .subscribe();
            } catch (Exception e) {
                println("---- should not come here : " + e);
            }

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("21:06:34.342 @CurrentThread Enter test function");
        assertOut("21:06:34.397 @CurrentThread Leave test function");
    }

    @Test
    public void test_exception_with_on_error_handler() throws Exception {
        new Thread(() -> {
            println("Enter test function");

            errorPublisher
                    .subscribeOn(schedulerForWorkThread1) //cause publisher run in new thread
                    .subscribe(result -> {
                        //nothing
                    }, e -> { //onError
                        sleep(1); //sleep 1ms to just let other thread run so can get predictable output
                        println("-------- " + e);
                    }, () -> { //onComplete
                        //nothing
                    });

            println("Leave test function");
        }, "CurrentThread" /*threadName*/).start();

        assertOut("14:26:02.439 @CurrentThread Enter test function");
        assertOut("14:26:02.459 @CurrentThread Leave test function");
        assertOut("14:26:02.462 @RxWorkThread1 -------- java.lang.NullPointerException");
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////


    private static Scheduler createScheduler(String threadName) {
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
