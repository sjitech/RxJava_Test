package com.company;

import org.junit.*;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import com.company.Util;
import com.company.Util.System;

public class RxJava_Test {

    @Before
    public void before_each_test() {
        System.out.clear();
    }

    private static void assertOut(String s) {
        Assert.assertEquals(System.out.popLine(), s.substring(12));
    }

    public static Observable<String> createSlowProducer() {
        return Observable.create((Subscriber<? super String> consumer) -> {
                    Util.sleep(1); //just let main thread run so can get fixed output
                    System.out.println("begin produce");
                    System.out.println("producer sleep a while");
                    Util.sleep(3000);
                    System.out.println("notify consumer");
                    consumer.onNext("sample producedResult");
                    consumer.onCompleted();
                    System.out.println("end produce");
                }
        );
    }

    @Test
    public void test_threading_default_in_main() {
        System.out.println("enter test function");

        createSlowProducer()
                .subscribe(producedResult -> {
                    System.out.println("consumer got produced result");
                });

        System.out.println("leave test function");

        assertOut("17:02:56.571 [main] enter test function");
        assertOut("17:02:56.667 [main] begin produce");
        assertOut("17:02:56.667 [main] producer sleep a while");
        assertOut("17:02:59.668 [main] notify consumer");
        assertOut("17:02:59.668 [main] consumer got produced result");
        assertOut("17:02:59.668 [main] end produce");
        assertOut("17:02:59.669 [main] leave test function");
    }

    @Test
    public void test_rx_will_produce_and_consume_in_an_RxScheduler_thread() {
        System.out.println("enter test function");

        createSlowProducer()
                .subscribeOn(Schedulers.newThread()) //cause producer run in new thread
                .subscribe(producedResult -> {
                    System.out.println("consumer got produced result");
                });

        System.out.println("leave test function");

        Util.delay(3500, () -> {
            assertOut("17:38:05.527 [main] enter test function");
            assertOut("17:38:05.641 [main] leave test function");
            assertOut("17:38:05.650 [RxNewThreadScheduler-1] begin produce");
            assertOut("17:38:05.650 [RxNewThreadScheduler-1] producer sleep a while");
            assertOut("17:38:08.650 [RxNewThreadScheduler-1] notify consumer");
            assertOut("17:38:08.650 [RxNewThreadScheduler-1] consumer got produced result");
        });
    }

    @Test
    public void test_rx_will_produce_in_an_RxScheduler_thread_and_consume_in_another_RxScheduler_thread() {
        System.out.println("enter test function");

        createSlowProducer()
                .subscribeOn(Schedulers.newThread()) //cause producer run in new thread
                .observeOn(Schedulers.newThread()) //cause consumer run in another new thread
                .subscribe(producedResult -> {
                    System.out.println("consumer got produced result");
                    System.out.println("consumer sleep a while");
                    Util.sleep(1000);
                    System.out.println("consumer exit");
                });

        System.out.println("leave test function");

        Util.delay(5000, () -> {
            try {
                assertOut("17:49:00.296 [main] enter test function");
                assertOut("17:49:00.481 [main] leave test function");
                assertOut("17:49:00.483 [RxNewThreadScheduler-2] begin produce");
                assertOut("17:49:00.483 [RxNewThreadScheduler-2] producer sleep a while");
                assertOut("17:49:03.485 [RxNewThreadScheduler-2] notify consumer");
                assertOut("17:49:03.486 [RxNewThreadScheduler-2] end produce");
                assertOut("17:49:03.486 [RxNewThreadScheduler-1] consumer got produced result");
                assertOut("17:49:03.486 [RxNewThreadScheduler-1] consumer sleep a while");
            } catch(ComparisonFailure e) {
                System.out.rewind();
                assertOut("17:49:00.296 [main] enter test function");
                assertOut("17:49:00.481 [main] leave test function");
                assertOut("17:49:00.483 [RxNewThreadScheduler-3] begin produce");
                assertOut("17:49:00.483 [RxNewThreadScheduler-3] producer sleep a while");
                assertOut("17:49:03.485 [RxNewThreadScheduler-3] notify consumer");
                assertOut("17:49:03.486 [RxNewThreadScheduler-3] end produce");
                assertOut("17:49:03.486 [RxNewThreadScheduler-2] consumer got produced result");
                assertOut("17:49:03.486 [RxNewThreadScheduler-2] consumer sleep a while");
            }
        });
    }

    @Test
    public void test_rx_will_produce_if_subscribe_called_even_without_callback() {
        System.out.println("enter test function");

        createSlowProducer()
                .subscribe();

        System.out.println("leave test function");

        assertOut("17:08:28.050 [main] enter test function");
        assertOut("17:08:28.145 [main] begin produce");
        assertOut("17:08:28.145 [main] producer sleep a while");
        assertOut("17:08:31.145 [main] notify consumer");
        assertOut("17:08:31.146 [main] end produce");
        assertOut("17:08:31.146 [main] leave test function");

    }

    @Test
    public void test_rx_will_not_produce_if_not_subscribed() {
        System.out.println("enter test function");

        createSlowProducer();

        System.out.println("leave test function");

        assertOut("17:06:31.746 [main] enter test function");
        assertOut("17:06:31.834 [main] leave test function");
    }
}
