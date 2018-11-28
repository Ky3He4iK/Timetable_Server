package main.kotlin.com.ky3he4ik.timetable.server.util

class Changes(classCount: Int, var dayInd: Int = -1) {
    var hasChanges = Array(classCount) { false }
    val changes = ArrayList<ChangesClass>()
    val changeIndexes = HashMap<Int, Int>()

    data class ChangesClass(val classInd: Int, val changeData: ArrayList<String>) {
        override operator fun equals(other: Any?) : Boolean {
            if (other == null || other !is ChangesClass)
                return false
            if (classInd == other.classInd && changeData == other.changeData)
                return true
            return false
        }

        override fun hashCode(): Int = changeData.hashCode() * classInd
    }

    override operator fun equals(other: Any?): Boolean {
        if (other == null)
            return false
        if (other !is Changes)
            return false
        return difference(other).isEmpty()
    }

    override fun hashCode(): Int = hasChanges.hashCode().xor(changes.hashCode()).xor(changeIndexes.hashCode())

    /**
     * Returns sub-array with different changes from this class
     */
    private fun difference(oldChanges: Changes): ArrayList<ChangesClass> {
        if (oldChanges.dayInd != dayInd || oldChanges.dayInd !in 0..5)
            return changes
        val difference =  ArrayList<ChangesClass>()
        for (change in changes) {
            val changeInd = oldChanges.changeIndexes[change.classInd] ?: 0
            if (!oldChanges.hasChanges[change.classInd]
                    || oldChanges.changes[changeInd].changeData != change.changeData)
                difference.add(change)
        }
        return difference
    }
}
