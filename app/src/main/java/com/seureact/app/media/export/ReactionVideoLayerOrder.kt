package com.seureact.app.media.export

internal enum class ReactionVideoLayer { CAMERA, SOURCE }

internal object ReactionVideoLayerOrder {
    val compositionOrder = listOf(ReactionVideoLayer.CAMERA, ReactionVideoLayer.SOURCE)

    fun layerForInputId(inputId: Int): ReactionVideoLayer? = compositionOrder.getOrNull(inputId)
}
