package me.apomazkin.core_db_impl.room

import me.apomazkin.core_db_impl.room.migrations.MigrationFrom01to02
import me.apomazkin.core_db_impl.room.migrations.MigrationFrom02to03
import me.apomazkin.core_db_impl.room.migrations.MigrationFrom03to04
import me.apomazkin.core_db_impl.room.migrations.MigrationFrom04to05
import me.apomazkin.core_db_impl.room.migrations.MigrationFrom05to06
import me.apomazkin.core_db_impl.room.migrations.MigrationFrom06to07
import me.apomazkin.core_db_impl.room.migrations.MigrationFrom07to08
import me.apomazkin.core_db_impl.room.migrations.MigrationFrom08to09
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    MigrationFrom01to02::class,
    MigrationFrom02to03::class,
    MigrationFrom03to04::class,
    MigrationFrom04to05::class,
    MigrationFrom05to06::class,
    MigrationFrom06to07::class,
    MigrationFrom07to08::class,
    MigrationFrom08to09::class,
)
class AllMigrationTest