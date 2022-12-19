package me.apomazkin.polytrainer.navigation

data class NavLogEntity(
    val current: NavEntity,
    val stack: List<NavEntity>,
)

fun NavLogEntity.processLog(): String {
    val result = StringBuilder()
    this.stack.forEach { entity ->
        val offset = "--".repeat(entity.index)
        result.append(" \n-$offset-> ${entity.route}")
        entity.deepLink?.let {
            val needLogDeepLink = !Constants.NON_LOG_DEEP_LINK.any { pattern ->
                it.contains(pattern)
            }
            if (needLogDeepLink) {
                result.append(" \n-$offset-> deepLink: $it")
            }
        }
    }
    return result.toString()
}