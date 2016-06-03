package com.company;

import java.time.LocalTime;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Util {
    public static class System {
        public static System out = new System(java.lang.System.out);
        public static System err = new System(java.lang.System.err);

        private java.io.PrintStream ps;
        private Queue<String> textQueue = new LinkedBlockingQueue<>();
        private Queue<String> textQueueBackup = new LinkedBlockingQueue<>();

        public System(java.io.PrintStream ps) {
            this.ps = ps;
        }

        public void println(String text) {
            synchronized (System.class) {
                text = " [" + Thread.currentThread().getName() + "] " + text;
                textQueue.offer(text);
                textQueueBackup.offer(text);
                ps.println(LocalTime.now().toString() + text);
            }
        }

        public String popLine() {
            synchronized (System.class) {
                return textQueue.poll();
            }
        }

        public void clear() {
            synchronized (System.class) {
                textQueue.clear();
                textQueueBackup.clear();
            }
        }

        public void rewind() {
            synchronized (System.class) {
                textQueue.clear();
                textQueue.addAll(textQueueBackup);
            }
        }
    }

    public static void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void delay(long millis, Runnable callback) {
        new Thread(() -> {
            sleep(millis);
            callback.run();
        }).start();
    }
}
