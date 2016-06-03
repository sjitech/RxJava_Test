# RxJava_Test
Test RxJava producer/consumer threading

Conclusion:
   Same as http://stackoverflow.com/questions/29355741/rxjava-observing-on-calling-subscribing-thread
   
   To enable producer(or says publisher) run in other thread, you need call `subscribeOn(...threading model...)` 
   
   To enable consumer(or says subscriber) run in other thread, you need call `observeOn(...threading model...)` 
