package com.company;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;

public class Util {
    public static class Output {

        private LinkedBlockingQueue<String> textQueue = new LinkedBlockingQueue<>();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

        public void println(String text) {
            text = LocalTime.now().format(timeFormatter) + " [" + Thread.currentThread().getName() + "] " + text;
            textQueue.offer(text);
            System.out.println(text);
        }

        public String popLine() {
            try {
                return textQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
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
}
