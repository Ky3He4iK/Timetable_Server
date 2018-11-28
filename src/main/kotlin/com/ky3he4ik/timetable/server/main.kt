package main.kotlin.com.ky3he4ik.timetable.server

import com.sun.net.httpserver.BasicAuthenticator
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import main.kotlin.com.ky3he4ik.timetable.server.util.Config
import main.kotlin.com.ky3he4ik.timetable.server.util.IO
import main.kotlin.com.ky3he4ik.timetable.server.util.LOG
import java.io.File
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.security.MessageDigest
import kotlin.concurrent.thread

var timetableJson: String? = null
var changesJson: String? = null
var timetableTimestamp: Long = 0L
var changesTimestamp: Long = 0L
val tokens = HashSet<String>()
const val json = "application/json"
const val html = "text/html"
const val splitCharacter = "g"

// Removes outdated tokens
fun checkerThread() {
    while (true) {
        try {
            for (token in tokens)
                if (token.split(splitCharacter).last().toLongOrNull(16) ?: -1 < System.currentTimeMillis())
                    tokens.remove(token)
            Thread.sleep(60 * 1000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun main(args: Array<String>) {
    LOG.extremelyImportant("Main/start", "loading...")
    //todo: make config
    //todo: docs
    //todo: main page
    //todo: tls
    thread(isDaemon = true, name = "checkerThread") { checkerThread() }
    Config.fromJson(IO.read("data${File.separator}config.json"))
    val updater = Updater()
    try {
        val mainFile = File("data${File.separator}timetable.json")
        timetableJson = mainFile.readText()
        timetableTimestamp = mainFile.lastModified()
    } catch (e: Exception) {
        LOG.e("Main/init", e.message, e)
    }
    try {
        val changesFile = File("data${File.separator}changes.json")
        changesJson = changesFile.readText()
        changesTimestamp = changesFile.lastModified()
    } catch (e: Exception) {
        LOG.e("Main/init", e.message, e)
    }
    LOG.extremelyImportant("Main/start", "loaded")
    HttpServer.create(InetSocketAddress(Config.port), 0).apply {
        createContext("/api") { http ->
            output(http, File("data${File.separator}apiPage.html").readText(), "text/html")
        }

        createContext("/api/dates.json") { http ->
            output(http, "{\n  \"timetable\": $timetableTimestamp,\n" +
                    "  \"changes\": $changesTimestamp\n}", json)
        }

        createContext("/api/timetable.json") { http -> output(http, timetableJson, json) }

        createContext("/api/changes.json") { http -> output(http, changesJson, json) }

        createContext("/login") { http ->
            //todo: add salt
            LOG.i("Server/login", "Successful attempt from ${http.localAddress} ${http.requestHeaders["User-agent"]}")
            val userAgent = http.requestHeaders["User-agent"]!![0]
            val timestamp = System.currentTimeMillis() + 10 * 60 * 1000
            val cookie = "token=${sha256(userAgent)}$splitCharacter${timestamp.toString(16)}"
            http.responseHeaders["Set-Cookie"] = cookie
            tokens.add(cookie)
            redirect(http, "/admin", "<a href=\"/admin\">Go there<a>")
        }.authenticator = object : BasicAuthenticator("Are you Misha?") {
            override fun checkCredentials(username: String?, password: String?): Boolean =
                    Config.adminLogin == username && Config.adminPassword == password
        }

        createContext("/admin") { http ->
            val response = checkToken(http)
            if (response != null) {
                output(http, "$response<br><a href=\"/login\">Try again</a>", html)
                return@createContext
            }
            output(http, File("data${File.separator}adminPage.html").readText(), html)
        }

        createContext("/admin/update") { http ->
            val response = checkToken(http)
            if (response != null) {
                output(http, "$response<br><a href=\"/login\">Try again</a>", html)
                return@createContext
            }
            LOG.i("Main/server", "Updating changes")
            output(http, "Ok", "text/plain")
            updater.update(fast = true, full = false)
        }

        createContext("/admin/updateFull") { http ->
            val response = checkToken(http)
            if (response != null) {
                output(http, "$response<br><a href=\"/login\">Try again</a>", html)
                return@createContext
            }
            LOG.i("Main/server", "Updating timetable")
            output(http, "Ok", "text/plain")
            updater.update(fast = true, full = true)
        }

        createContext("/admin/turnOff") { http ->
            val response = checkToken(http)
            if (response != null) {
                output(http, "$response<br><a href=\"/login\">Try again</a>", html)
                return@createContext
            }
            LOG.extremelyImportant("Main/server", "/admin/turnOff called!")
            output(http, "Ok", "text/plain")
            stop(5)
            LOG.extremelyImportant("Main/server", "stop")
        }

        createContext("/") { http ->
            redirect(http, "/api", "goto <a href=\"/api\">/api</a>")
        }

        start()
    }
}

fun checkToken(http: HttpExchange): String? {
    val cookies = http.requestHeaders["Cookie"] ?: return "You must login first"
    var token: String? = null
    out@ for (cookie in cookies)
        for (c in cookie.split("; "))
            if (c.startsWith("token=")) {
                token = c
                break@out
            }
    if (token == null)
        return "You must login first"

    //cookie: String, userAgent: String
    val timestamp = token.split(splitCharacter).last().toLongOrNull(16)
    if (timestamp == null || timestamp < System.currentTimeMillis())
        return "Your session was expired"
    val userAgent = http.requestHeaders["User-agent"]
    val expectedHash = token.split(splitCharacter).first().substring("token=".length)
    if (userAgent == null || userAgent.size != 1 || expectedHash != sha256(userAgent[0]))
        return "Stop using foreign cookies!"
    return null
}

fun redirect(http: HttpExchange, location: String, text: String = "") {
    http.responseHeaders.add("Content-type", "text/plain")
    http.responseHeaders.add("Location", location)
    http.sendResponseHeaders(303, 0)
    PrintWriter(http.responseBody).use { out -> out.println(text) }
}

fun output(http: HttpExchange, text: String?, contentType: String = "text/plain") {
    if (text == null)
        return error500(http)
    http.responseHeaders.add("Content-type", contentType)
    http.sendResponseHeaders(200, 0)
    PrintWriter(http.responseBody).use { out ->
        out.println(text)
    }
}

fun error500(http: HttpExchange) {
    http.responseHeaders.add("Content-type", "text/plain")
    http.sendResponseHeaders(500, 0)
    PrintWriter(http.responseBody).use { out ->
        out.println("I have some error there :(")
    }
}

fun sha256(string: String): String {
    val bytes = string.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}


