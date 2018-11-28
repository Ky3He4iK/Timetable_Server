package main.kotlin.com.ky3he4ik.timetable.server

import com.google.gson.GsonBuilder
import main.kotlin.com.ky3he4ik.timetable.server.util.*
import java.net.UnknownHostException
import kotlin.concurrent.thread


class Updater {
    private var timetable: TimetableStoring? = null

    init {
        LOG.i("Updater/Init", "Starting init")
        LOG.i("Updater/Init", "Finished init")
        thread(isDaemon = true, name = "updater") { updater() }
    }

    private fun updater() {
        var counter = 0
        while (true) {
            try {
                Thread.sleep(30 * 60 * 1000L) // every 30 min
                update(false, counter % 8 == 0) // Following statistics, only 12.5% of this updates are full
                LOG.i("UpdatingThr", "Updated. Full = ${counter % 8 == 0} (${counter % 8} in a row)")
                counter++
            } catch (e: UnknownHostException) {
                LOG.e("UpdatingThr/fail", "Can't resolve host. ${e.message}")
            } catch (e: Exception) {
                LOG.e("UpdatingThr/fail", e.message, e)
            }
        }
    }

    fun update(fast: Boolean, full: Boolean = true) {
        LOG.d("Update", "Starting")
        var tt = timetable
        if (full || tt == null) {
            tt = TimetableUpdater.fetchTimetable(fast)
            timetable = tt
            val newTTJson = GsonBuilder().setPrettyPrinting().create().toJson(timetable) + '\n'
            if (timetableJson != newTTJson) {
                timetableJson = newTTJson
                timetableTimestamp = System.currentTimeMillis()
                IO.write(Config.ttFile, timetableJson)
                IO.writeBkp("timetable.json", timetableJson)
                LOG.d("Update/tt", "updated")
            }
        }
/*        val changes = ChangesUpdater.createChanges(tt, fast)
        val newCJson = GsonBuilder().setPrettyPrinting().create().toJson(changes) + '\n'
        if (changesJson != newCJson) {
            changesJson = newCJson
            changesTimestamp = System.currentTimeMillis()
            IO.write(Config.changesFile, changesJson)
            IO.writeBkp("changes.json", changesJson)
            LOG.d("Update/changes", "updated")
        }*/
        //TODO: do something with changes
    }
}
