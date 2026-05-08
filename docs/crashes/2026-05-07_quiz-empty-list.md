# Crash: QuizGameImpl — IndexOutOfBoundsException

**Дата:** 2026-05-07 21:07:53
**Версия:** 0.1.1 (10001)
**Issue ID:** 966f00b8309630f9a9a8e7da3670a38e

## Fatal Exception

```
java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
    at java.util.ArrayList.get(ArrayList.java:437)
    at me.apomazkin.quiz.chat.quiz.QuizGameImpl.getQuiz(QuizGameImpl.kt:284)
    at me.apomazkin.quiz.chat.quiz.QuizGameImpl.getNextQuestion(QuizGameImpl.kt:210)
    at me.apomazkin.quiz.chat.quiz.QuizGameImpl.nextQuestion(QuizGameImpl.kt:57)
    at me.apomazkin.quiz.chat.logic.DatasourceEffectHandler$runEffect$10.invokeSuspend(DatasourceEffectHandler.kt:111)
    at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
    at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:100)
    at kotlinx.coroutines.internal.LimitedDispatcher$Worker.run(LimitedDispatcher.kt:113)
    at kotlinx.coroutines.scheduling.TaskImpl.run(Tasks.kt:89)
    at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:586)
    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:820)
    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:717)
    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:704)
```

## Все потоки на момент краша

```
DefaultDispatcher-worker-1:
    at sun.misc.Unsafe.park(Unsafe.java)
    at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:353)
    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.park(CoroutineScheduler.kt:855)
    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.tryPark(CoroutineScheduler.kt:803)
    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:751)
    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:704)

Firebase Blocking Thread #6:
    at java.io.FileDescriptor.sync(FileDescriptor.java)
    at androidx.datastore.core.FileWriteScope.writeData(FileStorage.kt:202)
    at androidx.datastore.core.DataStoreImpl$writeData$2.invokeSuspend(DataStoreImpl.kt:353)
    at androidx.datastore.core.DataStoreImpl$writeData$2.invoke(:8)
    at androidx.datastore.core.DataStoreImpl$writeData$2.invoke(:4)
    at androidx.datastore.core.FileStorageConnection.writeScope(FileStorage.kt:118)
    at androidx.datastore.core.DataStoreImpl.writeData$datastore_core_release(DataStoreImpl.kt:348)
    at androidx.datastore.core.DataStoreImpl$transformAndWrite$2.invokeSuspend(DataStoreImpl.kt:337)
    at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
    at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:100)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
    at com.google.firebase.concurrent.CustomThreadFactory.lambda$newThread$0$com-google-firebase-concurrent-CustomThreadFactory(CustomThreadFactory.java:47)
    at com.google.firebase.concurrent.CustomThreadFactory$$ExternalSyntheticLambda0.run(D8$$SyntheticClass)
    at java.lang.Thread.run(Thread.java:923)

queued-work-looper:
    at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
    at android.os.MessageQueue.next(MessageQueue.java:335)
    at android.os.Looper.loop(Looper.java:183)
    at android.os.HandlerThread.run(HandlerThread.java:67)

GoogleApiHandler:
    at android.os.MessageQueue.nativePollOnce(MessageQueue.java)
    at android.os.MessageQueue.next(MessageQueue.java:335)
    at android.os.Looper.loop(Looper.java:183)
    at android.os.HandlerThread.run(HandlerThread.java:67)

arch_disk_io_1:
    at sun.misc.Unsafe.park(Unsafe.java)
    at java.util.concurrent.locks.LockSupport.park(LockSupport.java:190)
    at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2067)
    at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
    at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1092)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
    at java.lang.Thread.run(Thread.java:923)

ScionFrontendApi:
    at sun.misc.Unsafe.park(Unsafe.java)
    at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:230)
    at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2109)
    at java.util.concurrent.LinkedBlockingQueue.poll(LinkedBlockingQueue.java:467)
    at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1091)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1152)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
    at java.lang.Thread.run(Thread.java:923)

FileObserver:
    at android.os.FileObserver$ObserverThread.observe(FileObserver.java)
    at android.os.FileObserver$ObserverThread.run(FileObserver.java:113)

OkHttp ConnectionPool:
    at java.lang.Object.wait(Object.java)
    at com.android.okhttp.ConnectionPool$1.run(ConnectionPool.java:106)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
    at java.lang.Thread.run(Thread.java:923)
```

## Описание

`QuizGameImpl.getQuiz()` (строка 284) обращается к `ArrayList.get(0)` при пустом списке. Вызывается из цепочки `nextQuestion()` → `getNextQuestion()` → `getQuiz()`. Эффект запускается из `DatasourceEffectHandler.runEffect()` (строка 111).
