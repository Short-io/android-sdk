package com.github.shortiosdk.Helpers

import com.github.shortiosdk.StringOrInt
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class StringOrIntSerializer : JsonSerializer<StringOrInt> {
    override fun serialize(
        src: StringOrInt?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return when (src) {
            is StringOrInt.Str -> JsonPrimitive(src.value)
            is StringOrInt.IntVal -> JsonPrimitive(src.value)
            null -> JsonNull.INSTANCE
        }
    }
}