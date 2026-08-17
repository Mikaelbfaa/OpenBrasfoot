package org.openfoot.importer

import org.openfoot.dataset.DatasetOptions
import org.openfoot.model.SpecRef

/**
 * Reads the options file of an installation.
 *
 * The file holds dozens of settings, almost all of them about how the game looks
 * or how fast it plays. Only two change what a world is, and only those two are
 * read: whether players carry seven individual abilities or a single strength,
 * and whether wages are shown by the week or by the month.
 *
 * Anything absent keeps the default the original ships with, so an installation
 * that has never had its options touched still imports.
 */
object OptionsFileReader {

    fun read(bytes: ByteArray): DatasetOptions {
        val record = SerializedStreamReader(bytes).readRoot()
        val defaults = DatasetOptions()
        return DatasetOptions(
            individualAbilities = record.fields[INDIVIDUAL_ABILITIES] as? Boolean
                ?: defaults.individualAbilities,
            monthlyWages = record.fields[MONTHLY_WAGES] as? Boolean ?: defaults.monthlyWages,
        )
    }

    /**
     * Whether a player has seven abilities or one strength.
     *
     * World generation produces the abilities either way, so this does not
     * change what is generated. It decides whether the match engine reads them.
     */
    @SpecRef("FORMAT-SPEC, habilidadeIndividual")
    private const val INDIVIDUAL_ABILITIES = "habilidadeIndividual"

    @SpecRef("4.8")
    private const val MONTHLY_WAGES = "salarioMensal"
}
