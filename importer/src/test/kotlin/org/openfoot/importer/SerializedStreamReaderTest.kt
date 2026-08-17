package org.openfoot.importer

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The reader is exercised against streams this test writes itself, using the
 * platform serializer.
 *
 * That is deliberate. It gives real coverage of the wire format without needing
 * a file from the original game, which no continuous integration machine has
 * and which must never enter the repository. The classes below are this test's
 * own and have nothing to do with the original's.
 */
class SerializedStreamReaderTest {

    private class Simple(
        val count: Int,
        val flag: Boolean,
        val label: String,
        val missing: String?,
    ) : Serializable

    private class Element(val value: Int, val name: String) : Serializable

    private class Holder(val title: String, val elements: ArrayList<Element>) : Serializable

    private class Wide(
        val byteField: Byte,
        val shortField: Short,
        val longField: Long,
        val doubleField: Double,
        val floatField: Float,
        val charField: Char,
    ) : Serializable

    private fun serialize(value: Any): ByteArray {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(value) }
        return bytes.toByteArray()
    }

    private fun read(value: Any): SerializedRecord =
        SerializedStreamReader(serialize(value)).readRoot()

    @Test
    fun `a stream begins with the magic the format specifies`() {
        val bytes = serialize(Simple(1, true, "x", null))
        assertEquals(0xAC.toByte(), bytes[0])
        assertEquals(0xED.toByte(), bytes[1])
        assertEquals(0x00.toByte(), bytes[2])
        assertEquals(0x05.toByte(), bytes[3])
    }

    @Test
    fun `primitive and string fields come back by name`() {
        val record = read(Simple(count = 42, flag = true, label = "titulo", missing = null))
        assertEquals(42, record.int("count"))
        assertTrue(record.boolean("flag"))
        assertEquals("titulo", record.text("label"))
    }

    @Test
    fun `a null string field reads as empty rather than failing`() {
        val record = read(Simple(1, false, "x", missing = null))
        assertEquals("", record.text("missing"))
        assertTrue(record.fields.containsKey("missing"))
    }

    @Test
    fun `the whole width of the primitive types is understood`() {
        val record = read(
            Wide(
                byteField = 7,
                shortField = -300,
                longField = 1L shl 40,
                doubleField = 0.5,
                floatField = 0.25f,
                charField = 'q',
            ),
        )
        assertEquals(7, record.fields["byteField"])
        assertEquals(-300, record.fields["shortField"])
        assertEquals(1L shl 40, record.fields["longField"])
        assertEquals(0.5, record.fields["doubleField"])
        assertEquals(0.25f, record.fields["floatField"])
        assertEquals('q', record.fields["charField"])
    }

    @Test
    fun `a list field yields its elements in order`() {
        val holder = Holder(
            title = "elenco",
            elements = arrayListOf(Element(1, "um"), Element(2, "dois"), Element(3, "tres")),
        )
        val record = read(holder)

        assertEquals("elenco", record.text("title"))
        val elements = record.records("elements")
        assertEquals(3, elements.size)
        assertEquals(listOf(1, 2, 3), elements.map { it.int("value") })
        assertEquals(listOf("um", "dois", "tres"), elements.map { it.text("name") })
    }

    @Test
    fun `an empty list yields nothing without failing`() {
        assertEquals(emptyList(), read(Holder("vazio", arrayListOf())).records("elements"))
    }

    @Test
    fun `a repeated string is read through its back reference`() {
        val repeated = "mesmo"
        val holder = Holder(
            title = repeated,
            elements = arrayListOf(Element(1, repeated), Element(2, repeated)),
        )
        val record = read(holder)
        assertEquals(repeated, record.text("title"))
        assertEquals(listOf(repeated, repeated), record.records("elements").map { it.text("name") })
    }

    @Test
    fun `a record reports the fields it carries so callers need no class name`() {
        val record = read(Simple(1, true, "x", null))
        assertTrue(record.hasFields("count", "flag", "label"))
        assertTrue(!record.hasFields("count", "absent"))
    }

    @Test
    fun `the class name and version are carried for diagnostics`() {
        val record = read(Element(1, "um"))
        assertTrue(record.className.endsWith("Element"), record.className)
        assertTrue(record.toString().contains("name"), record.toString())
    }

    @Test
    fun `bytes that are not a stream are refused`() {
        assertFailsWith<IllegalArgumentException> {
            SerializedStreamReader(byteArrayOf(1, 2, 3, 4, 5, 6)).readRoot()
        }
    }

    @Test
    fun `a truncated stream fails rather than inventing a record`() {
        val bytes = serialize(Holder("x", arrayListOf(Element(1, "um"))))
        assertFailsWith<Exception> {
            SerializedStreamReader(bytes.copyOf(bytes.size / 2)).readRoot()
        }
    }

    @Test
    fun `asking for a field that is not there says which fields are`() {
        val record = read(Element(1, "um"))
        val failure = assertFailsWith<IllegalArgumentException> { record.int("ausente") }
        assertTrue(failure.message.orEmpty().contains("value"), failure.message.orEmpty())
    }
}
