package org.openfoot.importer

import org.openfoot.dataset.ClubEntry
import org.openfoot.dataset.PlayerEntry
import org.openfoot.model.Country
import org.openfoot.model.Position
import org.openfoot.model.Side
import org.openfoot.model.SpecRef
import org.openfoot.model.Trait

/**
 * What had to be corrected while reading a file.
 *
 * The distributed data is not clean, and silently repairing it would hide that.
 * Every correction is recorded with the club it came from so an import can be
 * read afterwards and argued with.
 */
class ImportNotes {
    private val entries = ArrayList<String>()

    val notes: List<String> get() = entries

    fun note(message: String) {
        entries.add(message)
    }
}

/**
 * Turns a team file into the dataset entry world generation reads.
 *
 * The field names below are the ones the file itself carries. They are single
 * letters because the original's classes were obfuscated, and they mean
 * different things in a team record than in a player record, which is why the
 * two sets are kept apart rather than shared.
 *
 * Nothing here matches on a class name. A team record is recognised by carrying
 * the format version marker and a squad list, which is both what the clean room
 * rule requires and more robust than a name that could change.
 */
object TeamFileReader {

    /**
     * Reads one team file.
     *
     * The reference is the file's own base name, which the format also stores
     * internally. The stored copy is preferred when the two disagree, because
     * it is what the original uses to find artwork, but a disagreement is
     * recorded because it means a file has been renamed.
     */
    fun read(bytes: ByteArray, fileRef: String, notes: ImportNotes): ClubEntry {
        val record = SerializedStreamReader(bytes).readRoot()

        require(record.hasFields(FORMAT_MARKER, SQUAD, TEAM_NAME)) {
            "$fileRef does not look like a team file, it carries ${record.fields.keys}"
        }
        val version = record.int(FORMAT_MARKER)
        require(version == SUPPORTED_FORMAT) {
            "$fileRef declares format version $version, and the game itself only loads " +
                "$SUPPORTED_FORMAT"
        }

        val storedRef = record.text(TEAM_REF).ifBlank { fileRef }
        if (storedRef != fileRef) {
            notes.note("$fileRef stores the reference '$storedRef', so it has been renamed")
        }

        val country = record.int(TEAM_COUNTRY)
        val level = record.int(TEAM_LEVEL).coerceIn(ClubEntry.LEVEL_RANGE)
        if (level != record.int(TEAM_LEVEL)) {
            notes.note("$storedRef level ${record.int(TEAM_LEVEL)} clamped to $level")
        }

        return ClubEntry(
            ref = storedRef,
            name = record.text(TEAM_NAME).ifBlank { storedRef },
            country = country,
            level = level,
            reputation = record.int(TEAM_REPUTATION).coerceIn(ClubEntry.REPUTATION_RANGE),
            state = brazilianState(record, country, storedRef, notes),
            stadium = record.text(TEAM_STADIUM),
            capacity = record.int(TEAM_CAPACITY).coerceAtLeast(0),
            coach = record.text(TEAM_COACH),
            coachCountry = record.intOrNull(TEAM_COACH_COUNTRY) ?: country,
            squad = record.records(SQUAD).mapNotNull { player(it, storedRef, notes) },
        )
    }

    /**
     * The state championship a club plays in, which only means anything in
     * Brazil.
     *
     * Elsewhere the original leaves whatever was in the field before, so a
     * Spanish club can claim to play a state championship of Sao Paulo. The
     * value is dropped rather than passed on, and dropping it is not worth a
     * note because it is the normal case for most of the world.
     */
    @SpecRef("FORMAT-SPEC, estados")
    private fun brazilianState(
        record: SerializedRecord,
        country: Int,
        ref: String,
        notes: ImportNotes,
    ): Int? {
        if (country != Country.BRAZIL) {
            return null
        }
        val state = record.intOrNull(TEAM_STATE) ?: return null
        if (state !in ClubEntry.STATE_RANGE) {
            notes.note("$ref is Brazilian but carries state $state, which is not a championship")
            return null
        }
        return state
    }

    /**
     * Reads one player, or nothing when the record is too damaged to use.
     *
     * A single unusable player must not cost a whole club, so this reports and
     * returns nothing rather than throwing. Ages outside what the schema admits
     * are clamped instead, because an implausible age is still a player and the
     * value only ever feeds arithmetic that already tolerates the extremes.
     */
    private fun player(record: SerializedRecord, club: String, notes: ImportNotes): PlayerEntry? {
        val name = record.text(PLAYER_NAME).ifBlank { "?" }
        return try {
            val age = record.int(PLAYER_AGE)
            val corrected = age.coerceIn(PlayerEntry.AGE_RANGE)
            if (corrected != age) {
                notes.note("$club: $name is recorded as $age years old, read as $corrected")
            }
            PlayerEntry(
                name = name,
                age = corrected,
                country = record.int(PLAYER_COUNTRY),
                position = Position.ofOrdinal(record.int(PLAYER_POSITION)),
                side = Side.ofOrdinal(record.int(PLAYER_SIDE)),
                firstTrait = Trait.ofOrdinal(record.int(PLAYER_FIRST_TRAIT)),
                secondTrait = Trait.ofOrdinal(record.int(PLAYER_SECOND_TRAIT)),
                starter = record.int(PLAYER_STATUS) == STARTER,
                star = record.boolean(PLAYER_STAR),
                topWorld = record.boolean(PLAYER_TOP_WORLD),
                talent = record.int(PLAYER_TALENT).coerceIn(PlayerEntry.TALENT_RANGE),
            )
        } catch (failure: IllegalArgumentException) {
            notes.note("$club: dropped $name, ${failure.message}")
            null
        }
    }

    /** The version marker the game itself refuses to load a file without. */
    @SpecRef("FORMAT-SPEC, conteiner")
    private const val SUPPORTED_FORMAT = 185

    @SpecRef("FORMAT-SPEC, time")
    private const val FORMAT_MARKER = "vid"

    @SpecRef("FORMAT-SPEC, time")
    private const val TEAM_NAME = "e"

    @SpecRef("FORMAT-SPEC, time")
    private const val TEAM_REF = "d"

    @SpecRef("FORMAT-SPEC, time")
    private const val TEAM_COUNTRY = "a"

    @SpecRef("FORMAT-SPEC, time")
    private const val TEAM_STATE = "b"

    @SpecRef("FORMAT-SPEC, time")
    private const val TEAM_LEVEL = "c"

    @SpecRef("FORMAT-SPEC, time")
    private const val TEAM_REPUTATION = "n"

    @SpecRef("FORMAT-SPEC, time")
    private const val TEAM_STADIUM = "f"

    @SpecRef("FORMAT-SPEC, time")
    private const val TEAM_CAPACITY = "g"

    @SpecRef("FORMAT-SPEC, time")
    private const val TEAM_COACH = "h"

    @SpecRef("FORMAT-SPEC, time")
    private const val TEAM_COACH_COUNTRY = "i"

    @SpecRef("FORMAT-SPEC, time")
    private const val SQUAD = "l"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_NAME = "a"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_AGE = "d"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_COUNTRY = "c"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_POSITION = "e"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_SIDE = "i"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_FIRST_TRAIT = "g"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_SECOND_TRAIT = "h"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_STATUS = "f"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_STAR = "b"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_TOP_WORLD = "j"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val PLAYER_TALENT = "hash"

    @SpecRef("FORMAT-SPEC, jogador")
    private const val STARTER = 1
}
