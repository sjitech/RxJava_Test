# RxJava_Test
Test RxJava producer/consumer threading

For detail description, see http://my.oschina.net/osexp2003/blog/688428

Conclusion:
   Same as http://stackoverflow.com/a/29364009/2293666
   
   To enable producer(or says publisher) run in other thread, you need call `subscribeOn(...threading model...)` 
   
   To enable consumer(or says subscriber) run in other thread, you need call `observeOn(...threading model...)` 

   merge maybe get unordered result.
   concat will always get ordered result.


   To run test:
   ```
   ./gradlew test
   ```

