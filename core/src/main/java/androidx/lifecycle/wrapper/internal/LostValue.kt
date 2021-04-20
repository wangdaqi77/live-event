package androidx.lifecycle.wrapper.internal

/**
 * Linked.
 */
internal open class LostValue<T>(val value: T) {
    var left: LostValue<T>? = null

    internal class Head<T> (value: T) :
        LostValue<T>(value){

        private var latest: LostValue<T>? = null // leftmost, tail
            get() = field ?: this

        fun appendLatest(value: T) {
            val left = LostValue(value)
            this.latest!!.left = left
            this.latest = left
        }

        fun eachLeft(block: (value: T) -> Unit) {
            var current: LostValue<T>? = this
            while (current != null) {
                block(current.value)
                current = current.left
            }
        }
    }
}