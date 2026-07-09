package com.example.rollerapp.utils

import com.example.rollerapp.data.Inspection
import com.example.rollerapp.data.Replacement
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonHelper {
    private val gson = Gson()

    fun toJsonInspections(list: List<Inspection>): String {
        return gson.toJson(list)
    }

    fun toJsonReplacements(list: List<Replacement>): String {
        return gson.toJson(list)
    }

    fun fromJsonInspections(json: String): List<Inspection> {
        val type = object : TypeToken<List<Inspection>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun fromJsonReplacements(json: String): List<Replacement> {
        val type = object : TypeToken<List<Replacement>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}