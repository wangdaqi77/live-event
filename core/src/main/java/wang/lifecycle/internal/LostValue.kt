package wang.lifecycle.internal

/**
 * Linked.
 *      +------+  next +-----+  next +-----+
 * head |      | ----> |     | ----> |     |  tail
 *      +------+       +-----+       +-----+
 */
internal open class LostValue<T>(val value: T) {
    var next: LostValue<T>? = null

    internal open class Head<T> (value: T) : LostValue<T>(value) {

        private var tail: LostValue<T>? = null // tail, latest
            get() = field ?: this

        fun appendTail(value: T) {
            enq(value)
        }

        private fun enq(value: T) {
            val left = LostValue(value)
            this.tail!!.next = left
            this.tail = left
        }

        fun eachToTail(from: LostValue<T>? = null, block: (value: T) -> Unit) {
            var head: LostValue<T>? = from ?: this
            while (head != null) {
                block(head.value)
                head = head.next
            }
        }
    }
}