package com.hrm.breeze.domain.model

import kotlinx.datetime.Instant

/**
 * 一个会话 / 历史记录。Message 列表本身不在这里持有，由 Repository 分流查询。
 */
data class Conversation(
    val id: String,
    val title: String,
    val modelId: String,
    val updatedAt: Instant,
)
