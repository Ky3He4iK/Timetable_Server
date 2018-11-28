package main.kotlin.com.ky3he4ik.timetable.server.util

import main.kotlin.com.ky3he4ik.timetable.server.util.TimetableStoring.TT.TimetableDay.TimetableLesson.TimetableClass.TimetableCell
import org.json.JSONObject
import java.util.*

internal object TimetableUpdater {
    /* Loading from site */
    private const val ClassesListInd = 0
    private const val TeachersListInd = 1
    private const val RoomsListInd = 2
    private const val DaysListInd = 3
    private const val quote = '"'

    private var fast = false
    private var lists2 = HashMap<Int, ArrayList<ListObject2>>()
    private var defaults2 = ArrayList<Int>(2)

    private data class ListObject2(val index: Int, val name: String, val sortByInd: Boolean) {
        operator fun compareTo(other: ListObject2): Int {
            if (sortByInd)
                return index - other.index
            return name.compareTo(other.name)
        }

        override fun hashCode(): Int {
            return name.hashCode() * index
        }

        override fun equals(other: Any?): Boolean {
            if (other !is ListObject2)
                return super.equals(other)
            return index == other.index && name == other.name
        }
    }

    private fun sortGroups(timetable: TimetableStoring) {
        timetable.timetable.days.forEach { d -> d.lessons.forEach { l -> l.classes.forEach { c ->
            c.groups.sortBy { it.groupInd }
        } } }
    }

    private fun getListsSub2(string: String, sortByInd: Boolean = false): ArrayList<ListObject2> {
        val answer = ArrayList<ListObject2>()
        val arr = string.split("</option>").dropLast(1).drop(1)
        for (it in arr) {
            val quote = if ('\'' in it) '\'' else '"'
            val tmp = it.substring(it.indexOf("value=$quote") + "value=$quote".length)
            val tmpI = tmp.substring(0, tmp.indexOf(quote)).toInt()
            val value = it.substring(it.lastIndexOf(">") + 1)
            if (value == "Нет")
                continue
            answer.add(ListObject2(tmpI, value, sortByInd))
        }
        if (sortByInd)
            answer.sortBy(ListObject2::index)
        else
            answer.sortBy(ListObject2::name)
        return answer
    }

    private fun getLists2(): HashMap<Int, ArrayList<ListObject2>> {
        val answer = HashMap<Int, ArrayList<ListObject2>>()
        //tx_suncschedule_schedule[auditory]
        val page = IO.get("https://lyceum.urfu.ru/ucheba/raspisanie-zanjatii", fast)
        val array = page.substring(page.indexOf("<form data-name=\"form\" method=\"post\" action=\"" +
                "/ucheba/raspisanie-"),
                page.indexOf("<div class=\"schedule-block \">")).split("</select>")
        answer[ClassesListInd] = getListsSub2(array[1], true)

        val teachersWithFB = array[3].substring(0, array[3].lastIndexOf("</option>") + "</option>".length) +
                "<option value=${quote}666$quote>Учитель не задан</option>"

        answer[TeachersListInd] = getListsSub2(teachersWithFB)

        val roomsWithFB = array[2].substring(0, array[2].lastIndexOf("</option>") + "</option>".length) +
                "<option value=${quote}616$quote>ACCESS DENIED</option>"

        answer[RoomsListInd] = getListsSub2(roomsWithFB)
        answer[DaysListInd] = getListsSub2(array[4], true)
        return answer
    }

    private fun setClass2(timetable: TimetableStoring, classInd: Int, dayInd: Int) {
        val page = IO.get("https://lyceum.urfu.ru/", parameters = mapOf(
                "type" to 11,
                "scheduleType" to "group",
                "weekday" to dayInd + 1,
                "group" to classInd + 1
        ), fast = fast, ignore501 = true)

        //mapOf("uid" to 652,"subject" to "Литература","auditory" to "307","group" to "9Б",
        //                        "teacher" to "Овчинников А. Г.", "subgroup" to 0, "number" to 1,"weekday" to 4),
        val json = JSONObject(page)
        if (json.has("diffs") && !json.getJSONArray("diffs").isEmpty)
            LOG.w("TB/fetch/setClass", "We are found diffs at $classInd " +
                    "(${lists2[ClassesListInd]?.get(classInd)})$dayInd (${lists2[DaysListInd]?.get(dayInd)})\n" +
                    json.getJSONArray("diffs").toString(1))
        if (!json.has("lessons"))
            LOG.e("TB/fetch/setClass", "There are no timetable: $classInd " +
                    "(${lists2[ClassesListInd]?.get(classInd)})$dayInd (${lists2[DaysListInd]?.get(dayInd)})")
        else {
            val arr = json.getJSONArray("lessons")
            for (group in 0 until arr.length())
                setClassLesson2(timetable, classInd, dayInd, arr.getJSONObject(group))
        }
    }

    private fun setClassLesson2(timetable: TimetableStoring, classInd: Int, dayInd: Int, group: JSONObject) {
        val groupInd = group.getInt("subgroup")
        val subject = group.getString("subject")
        val room = group.getString("auditory")
        val roomInd = findName2(lists2[RoomsListInd]!!, room)

        val lesNum = group.getInt("number") - 1
        val teacher = group.getString("teacher")
        val teacherInd =
                if (teacher.isEmpty())
                    findInd2(lists2[TeachersListInd]!!, defaults2[1])
                else
                    findName2(lists2[TeachersListInd]!!, teacher)
        //LOG.v("TB/fetch/scl2", "$dayInd $lesNum $classInd; $roomInd: $room $teacherInd: $teacher; $subject $groupInd")

        timetable.timetable[dayInd, lesNum, classInd].groups.add(TimetableCell(
                classInd, roomInd, teacherInd, subject, groupInd
        ))
    }

    private fun setClasses2(timetable: TimetableStoring) {
        lists2[ClassesListInd]?.forEach { it ->
            for (day in 0 until 6)
                setClass2(timetable, it.index, day)
        }
    }


    private fun findInd2(list: ArrayList<ListObject2>, ind: Int): Int {
        if (list.first().sortByInd)
            list.binarySearch { it.compareTo(ListObject2(ind, "", true)) }
        else
            for (it in 0 until list.size)
                if (list[it].index == ind)
                    return it
        return -1
    }

    private fun findName2(list: ArrayList<ListObject2>, name: String): Int {
        if (list.first().sortByInd) {
            for (it in 0 until list.size)
                if (list[it].name == name)
                    return it
        } else
            list.binarySearch { it.compareTo(ListObject2(-1, name, false)) }
        return -1
    }

    private fun copyList2(from: ArrayList<ListObject2>, to: ArrayList<String>) {
        to.clear()
        for (obj in from)
            to.add(obj.name)
    }

    fun fetchTimetable(fast: Boolean = Config.isDebug): TimetableStoring {
        LOG.i("TTBuilder/createTT2", "Fetching timetable from new site. Smaller IO delay: $fast")
        this.fast = fast
        lists2 = getLists2()

        defaults2 = arrayListOf(findInd2(lists2[RoomsListInd]!!, 616),
                findInd2(lists2[TeachersListInd]!!, 666))

        val timetable = TimetableStoring(6, 7, classCount = lists2[ClassesListInd]!!.size,
                roomsCount = lists2[RoomsListInd]!!.size, trap = defaults2[0])

        copyList2(lists2[ClassesListInd]!!, timetable.classNames)
        copyList2(lists2[TeachersListInd]!!, timetable.teacherNames)
        copyList2(lists2[DaysListInd]!!, timetable.dayNames)
        copyList2(lists2[RoomsListInd]!!, timetable.roomNames)
        for (obj in lists2[RoomsListInd]!!)
            timetable.roomInd.add(obj.name)

        setClasses2(timetable)
        sortGroups(timetable)
        LOG.v("TTBuilder/createTT2", "classes loaded")
        timetable.freeRooms.setAll(timetable)
        LOG.i("TTBuilder/createTT2", "done")
        return timetable
    }
}
