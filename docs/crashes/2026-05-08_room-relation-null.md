# Crash: Room @Relation — NULL в NON-NULL поле

**Дата:** 2026-05-08 04:48:27
**Версия:** 0.1.1 (10001)
**Issue ID:** cb263a021c3b5603864ca92d3bd4e7b6

## Fatal Exception

```
java.lang.IllegalStateException: Relationship item 'lexemeDbWithWordDbRelation' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'lexeme_id' and entityColumn named 'id'.
    at me.apomazkin.core_db_impl.room.WordDao_Impl.getRandomWriteQuizList$lambda$20(WordDao_Impl.kt:1196)
    at me.apomazkin.core_db_impl.room.WordDao_Impl.$r8$lambda$uKKPwpKKdFcZ1meQjql5J5aYoYA()
    at me.apomazkin.core_db_impl.room.WordDao_Impl$$ExternalSyntheticLambda12.invoke(D8$$SyntheticClass)
    at androidx.room.util.DBUtil__DBUtil_androidKt$performSuspending$lambda$1$$inlined$internalPerform$1$1.invokeSuspend(DBUtil.kt:61)
    at androidx.room.util.DBUtil__DBUtil_androidKt$performSuspending$lambda$1$$inlined$internalPerform$1$1.invoke(:8)
    at androidx.room.util.DBUtil__DBUtil_androidKt$performSuspending$lambda$1$$inlined$internalPerform$1$1.invoke(:4)
    at androidx.room.coroutines.PassthroughConnection.transaction(PassthroughConnectionPool.kt:127)
    at androidx.room.coroutines.PassthroughConnection.access$transaction(PassthroughConnectionPool.kt:77)
    at androidx.room.coroutines.PassthroughConnection$withTransaction$2.invokeSuspend(PassthroughConnectionPool.kt:103)
    at androidx.room.coroutines.PassthroughConnection$withTransaction$2.invoke(:8)
    at androidx.room.coroutines.PassthroughConnection$withTransaction$2.invoke(:2)
    at androidx.room.RoomDatabaseKt__RoomDatabase_androidKt.compatTransactionCoroutineExecute(RoomDatabase.android.kt:2187)
    at androidx.room.RoomDatabaseKt.compatTransactionCoroutineExecute(:1)
    at androidx.room.RoomDatabase$createConnectionManager$3.invoke(RoomDatabase.android.kt:338)
    at androidx.room.RoomDatabase$createConnectionManager$3.invoke(RoomDatabase.android.kt:338)
    at androidx.room.coroutines.PassthroughConnection.withTransaction(PassthroughConnectionPool.kt:103)
    at androidx.room.util.DBUtil__DBUtil_androidKt$performSuspending$lambda$1$$inlined$internalPerform$1.invokeSuspend(DBUtil.kt:59)
    at androidx.room.util.DBUtil__DBUtil_androidKt$performSuspending$lambda$1$$inlined$internalPerform$1.invoke(:8)
    at androidx.room.util.DBUtil__DBUtil_androidKt$performSuspending$lambda$1$$inlined$internalPerform$1.invoke(:4)
    at androidx.room.coroutines.PassthroughConnectionPool$useConnection$2.invokeSuspend(PassthroughConnectionPool.kt:59)
    at androidx.room.coroutines.PassthroughConnectionPool$useConnection$2.invoke(:8)
    at androidx.room.coroutines.PassthroughConnectionPool$useConnection$2.invoke(:4)
    at kotlinx.coroutines.intrinsics.UndispatchedKt.startUndispatchedOrReturn(Undispatched.kt:43)
    at kotlinx.coroutines.BuildersKt__Builders_commonKt.withContext(Builders.common.kt:166)
    at kotlinx.coroutines.BuildersKt.withContext(:1)
    at androidx.room.coroutines.PassthroughConnectionPool.useConnection(PassthroughConnectionPool.kt:59)
    at androidx.room.RoomConnectionManager.useConnection(RoomConnectionManager.android.kt:138)
    at androidx.room.RoomDatabase.useConnection(RoomDatabase.android.kt:619)
    at androidx.room.util.DBUtil__DBUtil_androidKt$performSuspending$$inlined$compatCoroutineExecute$DBUtil__DBUtil_androidKt$1.invokeSuspend(DBUtil.android.kt:261)
    at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
    at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:100)
    at androidx.room.TransactionExecutor.execute$lambda$1$lambda$0(TransactionExecutor.android.kt:38)
    at androidx.room.TransactionExecutor.$r8$lambda$FZWr2PGmP3sgXLCiri-DCcePXSs()
    at androidx.room.TransactionExecutor$$ExternalSyntheticLambda0.run(D8$$SyntheticClass)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1154)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:652)
    at java.lang.Thread.run(Thread.java:1564)
```

## Описание

`WordDao_Impl.getRandomWriteQuizList()` — Room ожидает NON-NULL значение для `@Relation` поля `lexemeDbWithWordDbRelation`, но получает NULL. Связь по `lexeme_id` (parent) → `id` (entity) не находит соответствующую запись. Возможно: осиротевшая лексема без привязанного слова, или несогласованность данных после миграции/удаления.
