package org.openfoot.importer

import java.io.EOFException

/** The synthetic field name a list record stores its elements under. */
const val LIST_ITEMS = "__items__"

/** The class whose elements need reading past its declared fields. */
const val LIST_CLASS = "java.util.ArrayList"

/**
 * One record read out of a serialized stream.
 *
 * Fields are keyed by the names the stream itself carries. The class name is
 * kept for diagnostics only: nothing decides what to do with a record by
 * matching a class name, because those names belong to the original and naming
 * them in code is what the clean room rule forbids. Records are recognised by
 * the fields they carry instead.
 *
 * One flat map holds the fields of every level of a class hierarchy, so a name
 * declared twice cannot be represented. The reader refuses such a record rather
 * than letting the inner declaration shadow the outer one, because obfuscated
 * names are single letters and a silent collision would lose a value without
 * anything looking wrong.
 */
class SerializedRecord(
    val className: String,
    val serialVersionUID: Long,
    val fields: Map<String, Any?>,
) {
    fun int(name: String): Int = intOrNull(name)
        ?: throw IllegalArgumentException("record has no integer field '$name', it has ${fields.keys}")

    fun intOrNull(name: String): Int? = fields[name] as? Int

    fun boolean(name: String): Boolean = fields[name] as? Boolean ?: false

    fun text(name: String): String = fields[name] as? String ?: ""

    /** True when the record carries every one of the given field names. */
    fun hasFields(vararg names: String): Boolean = names.all { fields.containsKey(it) }

    /**
     * The records held by a list field, empty when the field is absent, null or
     * holds something that is not a list.
     */
    fun records(name: String): List<SerializedRecord> {
        val held = fields[name] as? SerializedRecord ?: return emptyList()
        if (held.className != LIST_CLASS) return emptyList()
        val items = held.fields[LIST_ITEMS] as? List<*> ?: return emptyList()
        return items.filterIsInstance<SerializedRecord>()
    }

    override fun toString(): String = "$className${fields.keys.sorted()}"
}

/**
 * Reads the subset of the serialization stream format the game data uses.
 *
 * The format is self describing: a stream carries the names, types and order of
 * every field it holds, which is what makes reading it without the original
 * classes possible at all. That is why this is a parser rather than a call to
 * the platform deserializer, which would need classes named after the
 * original's, and which decides what to construct based on what the file says.
 *
 * Only what the game data contains is implemented. Anything else throws rather
 * than guessing, so an unsupported stream is a clear failure and never a wrong
 * answer.
 */
class SerializedStreamReader(private val bytes: ByteArray) {

    private var position = 0
    private val handles = ArrayList<Any?>()

    /** Reads the single object a data file holds. */
    fun readRoot(): SerializedRecord {
        val magic = readUnsignedShort()
        val version = readUnsignedShort()
        require(magic == STREAM_MAGIC && version == STREAM_VERSION) {
            "not a serialized stream: magic 0x${magic.toString(16)} version $version"
        }
        return readValue() as? SerializedRecord
            ?: throw IllegalArgumentException("the stream does not hold a record")
    }

    private fun readValue(): Any? = when (val tag = readByte()) {
        TC_NULL -> null
        TC_REFERENCE -> handles[readInt() - BASE_HANDLE]
        TC_STRING -> newHandle(readUtf())
        TC_OBJECT -> readObject()
        TC_ARRAY -> readArray()
        TC_BLOCKDATA -> skipBlockData()
        TC_ENDBLOCKDATA -> END_OF_BLOCK
        else -> throw IllegalArgumentException(
            "unsupported stream tag 0x${tag.toString(16)} at byte ${position - 1}",
        )
    }

    private fun readObject(): SerializedRecord {
        val descriptor = readClassDescriptor()
            ?: throw IllegalArgumentException("an object at byte $position has no class descriptor")

        val fields = LinkedHashMap<String, Any?>()
        val record = SerializedRecord(descriptor.name, descriptor.serialVersionUID, fields)
        newHandle(record)

        for (level in descriptor.hierarchy()) {
            for (field in level.fields) {
                val value = readFieldValue(field.typeCode)
                require(!fields.containsKey(field.name)) {
                    "${descriptor.name} declares '${field.name}' at more than one level of its " +
                        "hierarchy, and a record keyed by bare field name cannot hold both"
                }
                fields[field.name] = value
            }
            when {
                level.name == LIST_CLASS ->
                    fields[LIST_ITEMS] = readListItems(fields[LIST_SIZE] as? Int ?: 0)

                level.hasWriteObject -> skipAnnotations()
            }
        }
        return record
    }

    /**
     * Reads the elements a list writes after its declared fields.
     *
     * The list writes its capacity as a block of raw bytes, then one value per
     * element, then the end marker. The capacity is not the size and is
     * deliberately discarded: the size field already read is what counts.
     */
    private fun readListItems(size: Int): List<Any?> {
        readValue()
        val items = ArrayList<Any?>(size)
        repeat(size) { items.add(readValue()) }
        require(readValue() === END_OF_BLOCK) {
            "a list of $size elements did not end where the format says it should"
        }
        return items
    }

    private fun readFieldValue(typeCode: Char): Any? = when (typeCode) {
        'I' -> readInt()
        'Z' -> readByte() != 0
        'B' -> readByte().toByte().toInt()
        'S' -> readSignedShort()
        'J' -> readLong()
        'D' -> Double.fromBits(readLong())
        'F' -> Float.fromBits(readInt())
        'C' -> readUnsignedShort().toChar()
        'L', '[' -> readValue()
        else -> throw IllegalArgumentException("unsupported field type '$typeCode'")
    }

    private fun readClassDescriptor(): ClassDescriptor? = when (val tag = readByte()) {
        TC_NULL -> null
        TC_REFERENCE -> handles[readInt() - BASE_HANDLE] as ClassDescriptor
        TC_CLASSDESC -> readNewClassDescriptor()
        else -> throw IllegalArgumentException(
            "expected a class descriptor, found 0x${tag.toString(16)} at byte ${position - 1}",
        )
    }

    private fun readNewClassDescriptor(): ClassDescriptor {
        val name = readUtf()
        val serialVersionUID = readLong()
        val flags = readByte()
        val fieldCount = readUnsignedShort()

        val descriptor = ClassDescriptor(name, serialVersionUID, (flags and SC_WRITE_METHOD) != 0)
        newHandle(descriptor)

        repeat(fieldCount) {
            val typeCode = readByte().toChar()
            val fieldName = readUtf()
            if (typeCode == 'L' || typeCode == '[') {
                readValue()
            }
            descriptor.fields.add(FieldDescriptor(fieldName, typeCode))
        }
        skipAnnotations()
        descriptor.parent = readClassDescriptor()
        return descriptor
    }

    private fun readArray(): List<Any?> {
        val descriptor = readClassDescriptor()
            ?: throw IllegalArgumentException("an array at byte $position has no class descriptor")
        val length = readInt()
        val items = ArrayList<Any?>(length)
        newHandle(items)
        val typeCode = descriptor.name.getOrElse(1) { 'L' }
        repeat(length) { items.add(readFieldValue(typeCode)) }
        return items
    }

    private fun skipAnnotations() {
        while (readByte() != TC_ENDBLOCKDATA) {
            position -= 1
            readValue()
        }
    }

    /**
     * Steps over a block of raw bytes.
     *
     * The length must be read into a local first. Reading it inside a compound
     * assignment would take the old position before the read advanced it, and
     * land one byte short of the block.
     */
    private fun skipBlockData(): Any {
        val length = readByte()
        position += length
        return RAW_BLOCK
    }

    private fun <T> newHandle(value: T): T {
        handles.add(value)
        return value
    }

    private fun readByte(): Int {
        if (position >= bytes.size) {
            throw EOFException("the stream ended at byte $position")
        }
        return bytes[position++].toInt() and 0xFF
    }

    private fun readSignedShort(): Int = (readUnsignedShort() shl 16) shr 16

    private fun readUnsignedShort(): Int = (readByte() shl 8) or readByte()

    private fun readInt(): Int = (readUnsignedShort() shl 16) or readUnsignedShort()

    private fun readLong(): Long {
        val high = readInt().toLong() and 0xFFFFFFFFL
        val low = readInt().toLong() and 0xFFFFFFFFL
        return (high shl 32) or low
    }

    private fun readUtf(): String {
        val length = readUnsignedShort()
        val text = String(bytes, position, length, Charsets.UTF_8)
        position += length
        return text
    }

    private class FieldDescriptor(val name: String, val typeCode: Char)

    private class ClassDescriptor(
        val name: String,
        val serialVersionUID: Long,
        val hasWriteObject: Boolean,
    ) {
        val fields = ArrayList<FieldDescriptor>()
        var parent: ClassDescriptor? = null

        /** Parents first, because that is the order their values are written in. */
        fun hierarchy(): List<ClassDescriptor> {
            val chain = ArrayList<ClassDescriptor>()
            var current: ClassDescriptor? = this
            while (current != null) {
                chain.add(0, current)
                current = current.parent
            }
            return chain
        }
    }

    private companion object {
        const val STREAM_MAGIC = 0xACED
        const val STREAM_VERSION = 5

        const val TC_NULL = 0x70
        const val TC_REFERENCE = 0x71
        const val TC_CLASSDESC = 0x72
        const val TC_OBJECT = 0x73
        const val TC_STRING = 0x74
        const val TC_ARRAY = 0x75
        const val TC_BLOCKDATA = 0x77
        const val TC_ENDBLOCKDATA = 0x78

        const val SC_WRITE_METHOD = 0x01
        const val BASE_HANDLE = 0x7E0000
        const val LIST_SIZE = "size"

        val END_OF_BLOCK = Any()
        val RAW_BLOCK = Any()
    }
}
