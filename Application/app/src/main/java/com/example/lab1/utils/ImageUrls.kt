package com.example.lab1.utils

import com.example.lab1.network.RetrofitClient
import java.net.URI

object ImageUrls {

    // A backend képes URL-jei lehetnek relatívak vagy "localhost"-ra mutathatnak,
    // az emulátorból viszont a BASE_URL hosztján érhető el a szerver.
    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val base = RetrofitClient.BASE_URL.trimEnd('/')
        val absolute = if (url.startsWith("/")) base + url else url
        val host = runCatching { URI(RetrofitClient.BASE_URL).host }.getOrNull() ?: return absolute
        return absolute
            .replace("://localhost", "://$host")
            .replace("://127.0.0.1", "://$host")
    }
}
