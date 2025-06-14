package com.github.shortiosdk

data class ShortIOResponseModel(
    val originalURL: String,
    val cloaking: Boolean? = null,
    val password: String? = null,
    val expiresAt: Int? = null,
    val expiredURL: String? = null,
    val title: String? = null,
    val tags: Array<String>? = null,
    val utmSource: String? = null,
    val utmMedium: String? = null,
    val utmCampaign: String? = null,
    val utmTerm: String? = null,
    val utmContent: String? = null,
    val ttl: String? = null,
    val path: String? = null,
    val androidURL: String? = null,
    val iphoneURL: String? = null,
    val createdAt: String? = null,
    val clicksLimit: Int? = null,
    val passwordContact: Boolean? = null,
    val skipQS: Boolean = false,
    val archived: Boolean = false,
    val splitURL: String? = null,
    val splitPercent: Int? = null,
    val integrationAdroll: String? = null,
    val integrationFB: String? = null,
    val integrationGA: String? = null,
    val integrationGTM: String? = null,
    val idString: String? = null,
    val id: String? = null,
    val shortURL: String? = null,
    val secureShortURL: String? = null,
    val redirectType: String? = null,
    val FolderId: String? = null,
    val DomainId: Int? = null,
    val OwnerId: Int? = null,
    val hasPassword: Boolean? = null,
    val User: ShortIOUserModel? = null,
    val source: String? = null,
    val success: Boolean? = null,
    val duplicate: Boolean? = null
)

data class ShortIOUserModel(
    val id: Int,
    val name: String,
    val email: String,
    val photoURL: String
)