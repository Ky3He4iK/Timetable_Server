package main.kotlin.com.ky3he4ik.timetable.server.util

import org.json.JSONObject

internal object ChangesUpdater {
    private var fast = false
    private var token = ""

    private fun getToken() {
        var page = IO.get(Config.urlLists, fast, true)
        page = page.substring(page.indexOf("<script>var tmToken=\"") + "<script>var tmToken=\"".length)
        token = page.substring(0, page.indexOf("\"</script>"))
    }

    private fun getRawChanges(fast: Boolean): String {
        val page = IO.get(Config.urlTimetable, parameters = mapOf(
                "tmToc" to token,
                "tmrType" to "1",
                "tmrClass" to "0",
                "tmrTeach" to "0",
                "tmrRoom" to "0",
                "tmrDay" to "0"
        ), fast = fast, ignore501 = true)
        if (page == "Err\n") {
            LOG.w("TTBuilder/getRChanges", "Can't get raw changes")
            return ""
        }
        return page.substring(page.indexOf("</summary>") + "</summary>".length)
    }

    private fun getDayByChanges(day: String): Int {
        if (day.isEmpty())
            return -1
        for (it in 0 until Config.dayNames.size)
            if (day.equals(Config.dayNames[it], ignoreCase = true))
                return it
        return 7
    }

    private fun setChangesSub(changes: Changes, lesCh: String, classInd: Int) {
        if (classInd != -1) {
            changes.hasChanges[classInd] = true
            val tmpArr =  ArrayList<String>()
            for (it in lesCh.split("<p>"))
                if (it.length >= 2)
                    tmpArr.add(it.substring(0, it.indexOf("</p>")))
            changes.changeIndexes[classInd] = changes.changes.size
            changes.changes.add(Changes.ChangesClass(classInd, tmpArr))
        }
    }

    /* Loading form site */
    fun createChanges(timetable: TimetableStoring, fast: Boolean = Config.isDebug): Changes {
        getToken()
        var rawChanges = getRawChanges(fast)
        val changes = Changes(timetable.classCount)
        if (rawChanges.isEmpty() || rawChanges == "<h3>ИЗМЕНЕНИЯ В РАСПИСАНИИ НА</h3></details>")
            return changes
        changes.dayInd = getDayByChanges(rawChanges
                .substring(rawChanges.indexOf("НА ") + "НА ".length, rawChanges.indexOf(", ")))
        if (changes.dayInd == -1) {
            LOG.w("TTBuilder/getChanges", "Changes' day ind is -1!")
            return changes
        }
        rawChanges = rawChanges.substring(rawChanges.indexOf("</h3>") + "<h3>".length)
                .replace("&nbsp;&mdash;", "-")
        for (lesCh in rawChanges.split("<h6>")) {
            if (lesCh.contains("</h6>"))
                setChangesSub(changes, lesCh.substring(lesCh.indexOf("<p>")),
                        timetable.findClass(lesCh.substring(0, lesCh.indexOf("</h6>"))))
        }
        LOG.i("CB/fetch", "finished")
        return changes
    }

    /* Loading from file */
    fun loadChanges(filename: String, classCount: Int): Changes? {
        val jsonObject = JSONObject(IO.read(filename))
        val changes = Changes(classCount, jsonObject.getInt("dayInd"))
        val hasChanges = jsonObject.getJSONArray("hasChanges")
        changes.hasChanges = Array(classCount) { hasChanges.getBoolean(it) }
        val changesArr = jsonObject.getJSONArray("changes")
        for (it in 0 until changesArr.length()) {
            val cc = changesArr.getJSONObject(it)
            val changeData = ArrayList<String>()
            cc.getJSONArray("changeData").forEach { changeData.add(it.toString()) }
            changes.changes.add(Changes.ChangesClass(classInd = cc.getInt("classInd"),
                    changeData = changeData))
        }
        changes.changes.forEachIndexed { index, changesClass -> changes.changeIndexes[changesClass.classInd] = index }
        return changes
    }
}
