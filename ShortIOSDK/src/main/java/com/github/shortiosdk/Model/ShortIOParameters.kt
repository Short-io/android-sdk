package com.github.shortiosdk


data class ShortIOParameters(
    val originalURL: String,
    val cloaking: Boolean? = null,
    val password: String? = null,
    val redirectType: Int? = null,
    var expiresAt: StringOrInt? = null,
    val expiredURL: String? = null,
    val title: String? = null,
    val tags: Array<String>? = null,
    val utmSource: String? = null,
    var utmMedium: String? = null,
    val utmCampaign: String? = null,
    val utmTerm: String? = null,
    val utmContent: String? = null,
    var ttl: StringOrInt? = null,
    val path: String? = null,
    val androidURL: String? = null,
    val iphoneURL: String? = null,
    var createdAt: StringOrInt? = null,
    val clicksLimit: Int? = null,
    val passwordContact: Boolean? = null,
    val skipQS: Boolean = false,
    var archived: Boolean = false,
    var splitURL: String? = null,
    val splitPercent: Int? = null,
    val integrationAdroll: String? = null,
    val integrationFB: String? = null,
    var integrationGA: String? = null,
    val integrationGTM: String? = null,
    val domain: String,
    val folderId: String? = null
) {
    init {
        require(clicksLimit == null || clicksLimit >= 1) {
            "clicksLimit must be null or >= 1"
        }
        require(splitPercent == null || splitPercent in 1..100) {
            "splitPercent must be null or between 1 and 100"
        }
    }
}
