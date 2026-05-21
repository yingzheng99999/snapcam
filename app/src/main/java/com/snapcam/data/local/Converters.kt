package com.snapcam.data.local

import androidx.room.TypeConverter
import android.net.Uri

class Converters {
    @TypeConverter
    fun fromUri(value: String): Uri = Uri.parse(value)

    @TypeConverter
    fun uriToString(uri: Uri): String = uri.toString()
}
