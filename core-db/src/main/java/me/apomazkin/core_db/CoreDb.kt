package me.apomazkin.core_db

import me.apomazkin.core_db_api.CoreDBApi
import me.apomazkin.core_db_impl.CoreDbImpl

class CoreDb {
    val api = CoreDBApi()
    val impl = CoreDbImpl()
}