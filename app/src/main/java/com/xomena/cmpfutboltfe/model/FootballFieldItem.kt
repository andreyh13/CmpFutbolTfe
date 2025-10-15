package com.xomena.cmpfutboltfe.model

data class FootballFieldItem(
    val name: String,
    val address: String,
    val phone: String
) {
    companion object {
        @JvmStatic
        fun createPitchesList(ffData: List<FootballField>): List<FootballFieldItem> {
            return if (ffData.isNotEmpty()) {
                ffData.map { f ->
                    FootballFieldItem(f.name, f.address, f.phone)
                }
            } else {
                emptyList()
            }
        }
    }
}
