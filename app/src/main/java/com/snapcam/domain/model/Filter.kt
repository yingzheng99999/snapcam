package com.snapcam.domain.model

data class Filter(
    val name: String,
    val agslFile: String?,
    val icon: Int? = null
) {
    companion object {
        val PRESETS = listOf(
            Filter("Original", null),
            Filter("Grayscale", "filters/grayscale.agsl"),
            Filter("Vintage", "filters/vintage.agsl"),
            Filter("B&W", "filters/bw.agsl"),
            Filter("Warm", "filters/warm.agsl"),
            Filter("Cool", "filters/cool.agsl"),
            Filter("Film", "filters/film.agsl")
        )
    }
}
