package com.example.dependency_graph

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform