package com.hrm.breeze.ui.navigation

/**
 * Shared navigation contract used by feature code and the app-level NavHost.
 *
 * Convention:
 * - destinations without arguments use `data object`
 * - destinations with arguments use `data class` plus a `ROUTE_PATTERN`
 * - route builders stay on the destination type to avoid stringly-typed callers
 */
sealed interface Destination {
    val routePattern: String
}

sealed interface TopLevelDestination : Destination {
    val title: String
}

data object Chat : TopLevelDestination {
    override val routePattern: String = "chat"
    override val title: String = "Chat"
}

data object History : TopLevelDestination {
    override val routePattern: String = "history"
    override val title: String = "History"
}

data object ApiConfig : TopLevelDestination {
    override val routePattern: String = "api-config"
    override val title: String = "API"
}

data object ModelSettings : TopLevelDestination {
    override val routePattern: String = "model-settings"
    override val title: String = "Models"
}

data class ChatThread(
    val conversationId: String,
) : Destination {
    override val routePattern: String = createRoute(conversationId)

    companion object {
        const val ARGUMENT_CONVERSATION_ID: String = "conversationId"
        const val ROUTE_PATTERN: String = "chat/thread/{$ARGUMENT_CONVERSATION_ID}"

        fun createRoute(conversationId: String): String = "chat/thread/$conversationId"
    }
}

object BreezeDestinations {
    val topLevel: List<TopLevelDestination> =
        listOf(
            Chat,
            History,
            ApiConfig,
            ModelSettings,
        )
}
