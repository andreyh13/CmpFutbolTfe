package com.xomena.cmpfutboltfe.model

data class County(val name: String) {

    companion object {

        @JvmStatic
        fun createCountiesList(data: Array<String>): List<County> {

            return data.asSequence().map { County(it) }.toList()
        }
    }
}
