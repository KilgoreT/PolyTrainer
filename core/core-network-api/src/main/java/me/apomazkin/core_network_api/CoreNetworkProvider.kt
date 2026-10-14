package me.apomazkin.core_network_api

/** Поставщик адресов сети для Dagger-зависимостей, как `CoreDbProvider`. */
interface CoreNetworkProvider {
    fun getWordsApi(): CoreNetworkApi.WordsApi
}
