package org.openfoot.importer

import org.openfoot.dataset.ClubEntry
import org.openfoot.model.SpecRef

/** One tier of a national league, as its configuration file describes it. */
data class DivisionShape(
    val country: Int,
    val division: Int,
    val teamCount: Int,
)

/**
 * Reads a national league configuration file.
 *
 * These files say how a country's pyramid is shaped, not who is in it. There is
 * no team list anywhere in them, which is the whole reason division membership
 * has to be worked out rather than read.
 */
object LeagueConfigReader {

    fun read(bytes: ByteArray): List<DivisionShape> {
        val root = SerializedStreamReader(bytes).readRoot()
        val tiers = root.records(TIERS)
        require(tiers.isNotEmpty()) {
            "this does not look like a league configuration, it carries ${root.fields.keys}"
        }
        return tiers.mapNotNull { tier ->
            val country = tier.intOrNull(COUNTRY) ?: return@mapNotNull null
            val division = tier.intOrNull(DIVISION) ?: return@mapNotNull null
            val teamCount = tier.intOrNull(TEAM_COUNT) ?: return@mapNotNull null
            if (teamCount <= 0) null else DivisionShape(country, division, teamCount)
        }
    }

    @SpecRef("FORMAT-SPEC, configuracoes")
    private const val TIERS = "a"

    @SpecRef("FORMAT-SPEC, configuracoes")
    private const val COUNTRY = "pais"

    @SpecRef("FORMAT-SPEC, configuracoes")
    private const val DIVISION = "divisao"

    @SpecRef("FORMAT-SPEC, configuracoes")
    private const val TEAM_COUNT = "nTimes"
}

/**
 * Works out which division each club plays in.
 *
 * The data files do not say. Team files carry no division and configuration
 * files carry no team list, so membership has to be derived, and the only
 * ordering the data offers is the club level.
 *
 * Sorting a country's clubs by level and filling each tier in turn reproduces
 * the real world exactly where it can be checked: the twenty strongest
 * Brazilian clubs in the distributed data are precisely the twenty that played
 * the 2022 first division, and the next twenty are precisely the second. See
 * OPEN-QUESTIONS item 24, which also records that this is observable in game
 * and therefore settleable by observation rather than argument.
 *
 * Ties are broken by reference. Levels repeat heavily in the lower tiers, so
 * without a tie break the same dataset would produce different pyramids on
 * different runs, and every seed would stop reproducing.
 */
@SpecRef("4.4")
fun assignDivisions(clubs: List<ClubEntry>, shapes: List<DivisionShape>): List<ClubEntry> {
    val tiersByCountry = shapes.groupBy { it.country }
        .mapValues { (_, tiers) -> tiers.sortedBy { it.division } }

    val assigned = LinkedHashMap<String, Int?>()

    for ((country, tiers) in tiersByCountry) {
        val ranked = clubs.asSequence()
            .filter { it.country == country && !it.nationalTeam }
            .sortedWith(compareByDescending<ClubEntry> { it.level }.thenBy { it.ref })
            .toList()

        var index = 0
        for (tier in tiers) {
            repeat(tier.teamCount) {
                if (index < ranked.size) {
                    assigned[ranked[index].ref] = tier.division
                    index += 1
                }
            }
        }
    }

    return clubs.map { club ->
        val division = assigned[club.ref]
        if (division == null) club else club.copy(division = division)
    }
}
