package main.kotlin.com.ky3he4ik.timetable.server.util

import java.io.*
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException


internal object IO {
    private fun checkFile(file: File, create: Boolean = false) {
        if (!file.exists()) {
            if (create)
                file.createNewFile()
            else
                throw AccessDeniedException(file)
        }
        if (!file.canWrite())
            throw AccessDeniedException(file)
    }

    fun read(filename: String) : String {
        val file = File(filename)
        checkFile(file)
        return file.readText()
    }

    fun write(filename: String, text: String?) {
        val directories = filename.split(File.separator)
        directories.dropLast(1).forEach {
            val file = File(it)
            if (!file.exists())
                file.mkdir()
        }

        val file = File(filename)
        checkFile(file, create = true)
        if (text == null)
            return
        val writer = FileOutputStream(file).bufferedWriter()
        writer.write(text)
        writer.flush()
        writer.close()

    }

    private fun getUrl(url: String, fast: Boolean = false, ignore501: Boolean = false): String {
        try {
            with(URL(url).openConnection() as HttpURLConnection) {
                setRequestProperty("User-agent", Config.userAgent)
                requestMethod = "GET"
                if (responseCode != 200) {
                    if (responseCode == 501 && ignore501) {
                        LOG.e("IO/get", "GET $url: $responseCode\n$responseMessage")
                        return getUrl(url, fast, ignore501)
                    }
                    throw IOException("Response code is $responseCode ($responseMessage), not 200 on URL $url")
                }
                BufferedReader(InputStreamReader(inputStream)).use {//, "windows-1251"
                    // Big pause to reduce server's load (sesc's servers are very slow)
                    Thread.sleep(if (fast) 50 else 1000)
                    return it.readText()
                }
            }
        } catch (e: UnknownHostException) {
            LOG.e("IO/get", "No internet or server is down", e)
            return getUrl(url, fast, ignore501)
        }
    }

    fun get(url: String, fast: Boolean = false, ignore501: Boolean = false, tryHarder: Boolean = true) : String {
        if (!tryHarder)
            return getUrl(url, fast, ignore501)
        try {
            return getUrl(url, fast, ignore501)
        } catch (e: UnknownHostException) {
            LOG.e("IO/get", "No internet or server is down", e)
        } catch (e: ConnectException) {
            LOG.e("IO/get", "No internet or server is down", e)
        } catch (e: StackOverflowError) {
            LOG.extremelyImportant("IO/get", "Too deep recursion")
        }
        Thread.sleep(1000)
        return get(url, fast, ignore501)
    }

    fun get(url: String, parameters: Map <Any, Any>, fast: Boolean = false, ignore501: Boolean = false) : String {
        val parametersString = StringBuilder()
        for (it in parameters)
            parametersString.append("${it.key}=${it.value}&")
        return get(url + '?' + parametersString.dropLast(1).toString(), fast, ignore501)
    }

    fun writeBkp(filename: String, text: String?) {
        val extensionPos = filename.indexOf('.')
        val filenameBase = filename.substring(0, if (extensionPos == -1) filename.length else extensionPos)
        val extension = filename.substring(if (extensionPos == -1) filename.length else extensionPos + 1)
        val filenameFinal = "bkp${File.separator}$filenameBase${LOG.now}_bkp.$extension"
        write(filenameFinal, text)
    }
}
