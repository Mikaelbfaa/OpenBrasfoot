package org.openfoot.cli

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The output path is checked before the import runs, so the test is about which
 * paths are refused rather than about what gets written.
 */
class OutputPathTest {

    @Test
    fun `a path in an existing directory is accepted`() {
        val directory = File(System.getProperty("java.io.tmpdir"))
        assertNull(outputPathProblem(File(directory, "base.json").path))
    }

    @Test
    fun `a bare file name is accepted because it lands in the working directory`() {
        assertNull(outputPathProblem("base.json"))
    }

    @Test
    fun `a path through a directory that does not exist is refused`() {
        val missing = File(System.getProperty("java.io.tmpdir"), "nao/existe/base.json")
        val problem = outputPathProblem(missing.path)
        assertNotNull(problem)
        assertTrue(problem.contains("nao"), problem)
    }

    @Test
    fun `a path that is itself a directory is refused`() {
        val problem = outputPathProblem(System.getProperty("java.io.tmpdir"))
        assertNotNull(problem)
        assertTrue(problem.contains("directory"), problem)
    }
}
