package org.openfoot.importer

import org.openfoot.dataset.ClubEntry
import org.openfoot.dataset.CountryEntry
import org.openfoot.dataset.WorldDataset
import org.openfoot.model.SpecRef
import java.io.File

/** A dataset built from an installation, and everything worth saying about it. */
data class ImportResult(
    val dataset: WorldDataset,
    val notes: List<String>,
)

/**
 * Builds a dataset from a player's own installation of the original game.
 *
 * The files stay where they are. Nothing is copied into the project, and the
 * denylist in the ignore file plus the check in continuous integration exist to
 * keep it that way: this reads a machine the player already owns and writes a
 * dataset of numbers, not of anyone's artwork.
 *
 * Two things the installation cannot supply are filled with placeholders and
 * reported rather than guessed. Country names live in a table that ships inside
 * the game rather than beside it, and country strength lives in the game's code,
 * which the clean room rule puts out of reach entirely. Both are dataset fields
 * precisely so that filling them in later costs no code.
 */
object InstallationImporter {

    fun importFrom(root: File): ImportResult {
        val notes = ImportNotes()

        val teamFiles = File(root, TEAMS_DIRECTORY)
            .listFiles { file -> file.isFile && file.name.endsWith(TEAM_SUFFIX) }
            ?.sortedBy { it.name }
            ?: throw IllegalArgumentException(
                "no $TEAMS_DIRECTORY directory under $root, so this is not an installation",
            )
        require(teamFiles.isNotEmpty()) { "$TEAMS_DIRECTORY under $root holds no team files" }

        val clubs = ArrayList<ClubEntry>(teamFiles.size)
        val seen = HashSet<String>()
        for (file in teamFiles) {
            try {
                val club = TeamFileReader.read(file.readBytes(), file.nameWithoutExtension, notes)
                if (!seen.add(club.ref)) {
                    notes.note("${file.name} repeats the reference '${club.ref}' and was skipped")
                } else {
                    clubs.add(club)
                }
            } catch (failure: Exception) {
                notes.note("${file.name} could not be read: ${failure.message}")
            }
        }
        require(clubs.isNotEmpty()) { "not one team file under $root could be read" }

        val withDivisions = assignDivisions(clubs, readPyramids(root, notes))
        reportDivisionCoverage(withDivisions, notes)
        val countries = placeholderCountries(withDivisions, notes)

        return ImportResult(
            dataset = WorldDataset(countries = countries, clubs = withDivisions),
            notes = notes.notes,
        )
    }

    /**
     * Says how many clubs came out with no division at all.
     *
     * This is the single most consequential thing about an imported world. A
     * club with no division is generated on the weakest band of section 4.4, a
     * strength base of one against twenty for a first division side, so it
     * fields a visibly worse squad than an equally rated club in a country that
     * happens to ship a league configuration. The distributed data configures
     * very few countries, so this is the normal case rather than the exception,
     * and it must not be discovered by wondering why a good club is bad.
     */
    @SpecRef("4.4")
    private fun reportDivisionCoverage(clubs: List<ClubEntry>, notes: ImportNotes) {
        val without = clubs.count { it.division == null && !it.nationalTeam }
        if (without > 0) {
            notes.note(
                "$without of ${clubs.size} clubs have no division, because the installation " +
                    "configures a league for only some countries. Those clubs generate on the " +
                    "weakest band of section 4.4 and will field weaker squads than their level " +
                    "suggests",
            )
        }
    }

    private fun readPyramids(root: File, notes: ImportNotes): List<DivisionShape> {
        val directory = File(root, LEAGUES_DIRECTORY)
        val files = directory.listFiles { file -> file.isFile && file.name.endsWith(LEAGUE_SUFFIX) }
            ?.sortedBy { it.name }
        if (files.isNullOrEmpty()) {
            notes.note(
                "no league configuration under $LEAGUES_DIRECTORY, so no club has a division and " +
                    "every squad will be generated on the weakest band",
            )
            return emptyList()
        }
        return files.flatMap { file ->
            try {
                LeagueConfigReader.read(file.readBytes())
            } catch (failure: Exception) {
                notes.note("${file.name} could not be read: ${failure.message}")
                emptyList()
            }
        }
    }

    /**
     * Every country the clubs and their players refer to, as a placeholder.
     *
     * The name is the index because the real names are not in the installation
     * beside the data. The level sits one above the threshold at which section
     * 4.4 starts penalising a country's clubs, so an unfilled table leaves
     * strength unscaled rather than quietly weakening the whole world. The
     * continent is deliberately not Europe, so an unfilled table does not
     * quietly exempt everyone from the handicaps that ask about it.
     */
    @SpecRef("4.4")
    private fun placeholderCountries(clubs: List<ClubEntry>, notes: ImportNotes): List<CountryEntry> {
        val indices = sortedSetOf<Int>()
        for (club in clubs) {
            indices.add(club.country)
            indices.add(club.coachCountry)
            club.squad.forEach { indices.add(it.country) }
        }
        notes.note(
            "${indices.size} countries were referenced and none can be named from the data: " +
                "levels default to $UNKNOWN_COUNTRY_LEVEL and want filling in before the world " +
                "is taken seriously",
        )
        return indices.map { index ->
            CountryEntry(
                index = index,
                name = "$UNKNOWN_COUNTRY_PREFIX$index",
                level = UNKNOWN_COUNTRY_LEVEL,
                continent = UNKNOWN_CONTINENT,
            )
        }
    }

    private const val TEAMS_DIRECTORY = "teams"
    private const val TEAM_SUFFIX = ".ban"
    private const val LEAGUES_DIRECTORY = "conf_ligas_nacionais"
    private const val LEAGUE_SUFFIX = ".cfg"

    private const val UNKNOWN_COUNTRY_PREFIX = "pais "

    /** One above the level at which section 4.4 begins scaling a country down. */
    @SpecRef("4.4")
    const val UNKNOWN_COUNTRY_LEVEL = 14

    /** Anything but Europe, so an unfilled table grants no European exemptions. */
    @SpecRef("3.3")
    const val UNKNOWN_CONTINENT = 1
}
