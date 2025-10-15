package com.xomena.cmpfutboltfe.model

import android.text.Spanned

data class RouteStep(
    val stepText: Spanned
) {
    companion object {
        @JvmStatic
        fun createRouteStepsList(data: Array<Spanned>): List<RouteStep> {
            return data.map { RouteStep(it) }
        }
    }
}
