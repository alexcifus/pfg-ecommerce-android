package com.example.myapplication.network

object ImageUrlNormalizer {
    fun normalize(url: String): String {
        return url
            .replace(
                "http://127.0.0.1:8000",
                "http://10.0.2.2:8000"
            )
            .replace(
                "http://localhost:8000",
                "http://10.0.2.2:8000"
            )
    }
}
