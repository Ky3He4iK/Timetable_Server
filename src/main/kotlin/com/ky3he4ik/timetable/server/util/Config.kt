package main.kotlin.com.ky3he4ik.timetable.server.util

import org.json.JSONObject

internal object Config {
    lateinit var userAgent: String
    lateinit var urlTimetable: String
    lateinit var urlLists: String
    lateinit var dayNames: ArrayList<String>
    var isDebug = false
    var logLevel: LOG.LogLevel = LOG.LogLevel.INFO
    lateinit var ttFile: String
    lateinit var changesFile: String
    lateinit var adminLogin: String
    lateinit var adminPassword: String
    var port = 8080

    private fun<T> get(jsonObject: JSONObject, name: String, default: T): T {
        if (jsonObject.has(name))
            return jsonObject.get(name) as T
        return default
    }

    fun fromJson(json: String) {
        val jsonObject = JSONObject(json)
        userAgent = get(jsonObject, "userAgent", "Nobody from nowhere")
        urlTimetable = get(jsonObject, "urlTimetable", "")
        urlLists = get(jsonObject, "urlLists", "")
        if (jsonObject.has("dayNames")) {
            dayNames = arrayListOf()
            jsonObject.getJSONArray("dayNames").forEach { dayNames.add(it as String) }
        } else
            dayNames = arrayListOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
        isDebug = get(jsonObject, "isDebug", false)
        logLevel = LOG.LogLevel.valueOf(get(jsonObject, "logLevel", "INFO"))
        ttFile = get(jsonObject, "ttFile", "")
        changesFile = get(jsonObject, "changesFile", "")
        adminLogin = get(jsonObject, "adminLogin", "admin")
        adminPassword = get(jsonObject, "adminPassword", "12345")
        port = get(jsonObject, "port", 8080)
    }
}
