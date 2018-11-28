package main.kotlin.com.ky3he4ik.timetable.server.util

import main.kotlin.com.ky3he4ik.timetable.server.util.TimetableStoring.TT.TimetableDay.TimetableLesson.TimetableClass.TimetableCell

//TODO: use unified getXXXToday/tomorrow/near/etc.
data class TimetableStoring(val daysCount: Int, val lessonsCount: Int, val classCount: Int, val roomsCount: Int, val trap: Int = -1) {
    override operator fun equals(other: Any?): Boolean {
        if (other == null)
            return false
        if (other !is TimetableStoring)
            return false
        if (classNames != other.classNames
                || teacherNames != other.teacherNames
                || roomNames != other.roomNames
                || roomInd != other.roomInd
                || dayNames != other.dayNames
                || timetable != other.timetable
                || freeRooms != other.freeRooms)
            return false
        return true
    }

    override fun hashCode(): Int {
        return classNames.hashCode().xor(teacherNames.hashCode())
                .xor(roomNames.hashCode()).xor(roomInd.hashCode())
                .xor(timetable.hashCode()).xor(freeRooms.hashCode())
    }

    class TT(daysCount: Int, lessonsCount: Int, classCount: Int) {
        val days = Array(daysCount) { TimetableDay(lessonsCount, classCount) }

        operator fun get(dayInd: Int, lessonNum: Int, classInd: Int, groupInd: Int): TimetableCell =
                days[dayInd].lessons[lessonNum].classes[classInd].groups[groupInd]

        operator fun set(dayInd: Int, lessonNum: Int, classInd: Int, groupInd: Int, value: TimetableCell) {
            days[dayInd].lessons[lessonNum].classes[classInd].groups[groupInd] = value
        }

        operator fun get(dayInd: Int, lessonNum: Int, classInd: Int): TimetableDay.TimetableLesson.TimetableClass =
                days[dayInd].lessons[lessonNum].classes[classInd]

        operator fun get(dayInd: Int, lessonNum: Int): TimetableDay.TimetableLesson = days[dayInd].lessons[lessonNum]

        operator fun get(dayInd: Int): TimetableDay = days[dayInd]

        override operator fun equals(other: Any?): Boolean {
            if (other == null)
                return false
            if (other !is TT)
                return false
            for (day in 0 until days.size)
                for (lesson in 0 until days[day].lessons.size)
                    for (classInd in 0 until get(day, lesson).classes.size)
                        for (group in 0 until get(day, lesson, classInd).groups.size)
                            if (get(day, lesson, classInd, group) != other[day, lesson, classInd, group])
                                return false

            return true
        }

        override fun hashCode(): Int = days.hashCode()

        class TimetableDay(lessonsCount: Int, classCount: Int) {
            val lessons = Array(lessonsCount) { TimetableLesson(classCount) }

            class TimetableLesson(classCount: Int) {
                val classes = Array(classCount) { TimetableClass() }

                class TimetableClass {
                    val groups: ArrayList<TimetableCell> = ArrayList()
                    data class TimetableCell(val classInd: Int, val roomInd: Int, var teacherInd: Int,
                                             var subject: String, val groupInd: Int) {
                        override operator fun equals(other: Any?): Boolean {
                            if (other == null)
                                return false
                            if (other !is TimetableCell)
                                return false
                            return classInd == other.classInd
                                    && roomInd == other.roomInd
                                    && teacherInd == other.teacherInd
                                    && subject == other.subject
                                    && groupInd == other.groupInd
                        }

                        override fun hashCode(): Int
                                = (classInd * roomInd * teacherInd * groupInd).xor(subject.hashCode())
                    }
                }
            }
        }
    }

    class FreeRooms(daysCount: Int, lessonsCount: Int) {
        override operator fun equals(other: Any?): Boolean {
            if (other == null)
                return false
            if (other !is FreeRooms)
                return false
            return days.contentEquals(other.days)
        }

        override fun hashCode(): Int = days.hashCode()

        private val days = Array(daysCount) { FreeRoomsDay(lessonsCount) }

        class FreeRoomsDay(lessonsCount: Int) {
            override operator fun equals(other: Any?): Boolean {
                if (other == null)
                    return false
                if (other !is FreeRoomsDay)
                    return false
                return lessons.contentEquals(other.lessons)
            }

            override fun hashCode(): Int = lessons.hashCode()

            val lessons = Array(lessonsCount) { FreeRoomsLesson() }

            class FreeRoomsLesson {
                override operator fun equals(other: Any?): Boolean {
                    if (other == null)
                        return false
                    if (other !is FreeRoomsLesson)
                        return false
                    return rooms == other.rooms
                }

                override fun hashCode(): Int = rooms.hashCode()

                var rooms = ArrayList<Int>()
            }
        }

        fun setAll(timetable: TimetableStoring) {
            for (dayInd in 0 until timetable.daysCount)
                for (lessonNum in 0 until timetable.lessonsCount) {
                    val isBusy = Array(timetable.roomsCount) { false }
                    for (classCells in timetable.timetable.days[dayInd].lessons[lessonNum].classes)
                        for (group in classCells.groups)
                            isBusy[group.roomInd] = true
                    val freeRooms = ArrayList<Int>()
                    isBusy.forEachIndexed { index, b -> if (!b) freeRooms.add(index) }
                    days[dayInd].lessons[lessonNum].rooms = freeRooms
                }
        }
    }

    var classNames = ArrayList<String>()
    var teacherNames = ArrayList<String>()
    var roomNames = ArrayList<String>()
    var roomInd = ArrayList<String>()
    var dayNames = ArrayList<String>()
    val timetable = TT(daysCount, lessonsCount, classCount)
    val freeRooms = FreeRooms(daysCount, lessonsCount)

    fun findDay(dayName: String): Int = find(dayNames, dayName)

    fun findClass(className: String): Int = find(classNames, className)

    private fun find(array: ArrayList<String>, value: String): Int {
        for (it in 0 until array.size)
            if (array[it].equals(value, ignoreCase = true))
                return it
        return -1
    }
}
