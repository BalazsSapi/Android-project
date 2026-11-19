package com.example.lab1

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.lab1.network.models.ProfileDto
import com.example.lab1.utils.ImageUrls

// Kör alakú profilkép: URL-ből, base64-ből, vagy a név kezdőbetűje, ha nincs kép
@Composable
fun ProfileAvatar(
    profile: ProfileDto?,
    fallbackName: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val imageUrl = ImageUrls.resolve(profile?.profileImageUrl)
    val base64Image = remember(profile?.profileImageBase64) {
        decodeBase64Image(profile?.profileImageBase64)
    }
    val initial = fallbackName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        // A kezdőbetű alul marad, a kép (ha betöltődik) eltakarja
        Text(initial, style = MaterialTheme.typography.titleLarge)
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (base64Image != null) {
            Image(
                bitmap = base64Image,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun decodeBase64Image(value: String?): ImageBitmap? {
    if (value.isNullOrBlank()) return null
    return runCatching {
        // "data:image/png;base64,...." előtag esetén csak a tényleges adat kell
        val raw = value.substringAfter("base64,", value)
        val bytes = Base64.decode(raw, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}
