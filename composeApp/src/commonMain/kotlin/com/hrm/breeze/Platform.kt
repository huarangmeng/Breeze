package com.hrm.breeze

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform