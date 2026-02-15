package utils

import java.util.UUID

fun String.toUUIDOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
fun String.isValidUUID(): Boolean = runCatching { UUID.fromString(this) }.isSuccess
