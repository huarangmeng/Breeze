package com.hrm.breeze.domain.model

import kotlinx.datetime.Instant

/**
 * 一条聊天消息的领域模型。保持纯数据，不引入任何 UI / 网络类型。
 */
data class Message(
    val id: String,
    val conversationId: String,
    val role: Role,
    val content: String,
    val createdAt: Instant,
) {
    enum class Role { User, Assistant, System }
}
