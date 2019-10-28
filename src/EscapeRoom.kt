@file:Suppress("SpellCheckingInspection", "EnumEntryName", "FunctionName", "NonAsciiCharacters", "LocalVariableName", "ClassName", "PrivatePropertyName", "PropertyName")

import kotlin.math.absoluteValue

data class Spiel(
    val voriges: Spiel?,
    val spieler: Spieler,
    val haus: Haus,
    val aufgeben: Boolean = false,
    val phaseGesetzt: Boolean = false
)

interface Steuerung {
    fun <T> auswahl(spiel: Spiel, nachricht: String, auswahl: Collection<T>, nameUndTaste: (T) -> Pair<String, String?>): Auswahl<T>
    fun neuerRaum(spiel: Spiel, raum: Raum, richtung: Richtung): Spiel
    fun ausgabe(nachricht: () -> String)
}

@Suppress("unused")
enum class Phase(val aktion: (Spiel) -> Spiel) {
    wache(::wache),
    gegenstandsAuswahl(::gegenstandAuswahl),
    ereignis(::ereignis),
    türAuswählen(::türAuswählen)
}

val startPunkt = Punkt(0, 0)

data class Spieler(
    val steuerung: Steuerung,
    val raum: Punkt = startPunkt,
    val phase: Phase = Phase.values().first(),
    val richtung: Richtung = Richtung.O,
    val gegenstand: Gegenstand? = null
)

data class Haus(
    val räume: Map<Punkt, Raum>,
    val eingang: Punkt,
    val ausgang: Punkt
)

data class Raum(
    val punkt: Punkt,
    val gegenstände: List<Gegenstand> = emptyList(),
    val türen: Map<Richtung, Punkt> = mapOf(),
    val wache: ((Spieler) -> Boolean)? = null,
    val ereignis: ((Spiel) -> Spiel)? = null
) {
    override fun toString(): String {
        return punkt.toString()
    }
}

enum class Gegenstand {
    Diamant,
    Erfindung,
    Flasche,
    Flöte,
    Fernbedienung,
    Goldsack,
    Hypnosescheibe,
    Joker,
    Maske,
    Messer,
    Keinen
}

data class Wache(val name: String, val gegenstände: List<Gegenstand>)


val wachen = listOf(
    Wache("Skatrunde", listOf(Gegenstand.Joker)),
    Wache("Horde", listOf(Gegenstand.Maske)),
    Wache("Dimensionswandler", listOf(Gegenstand.Erfindung)),
    Wache("Flaschengeist", listOf(Gegenstand.Flasche)),
    Wache("Schlange", listOf(Gegenstand.Flöte)),
    Wache("Riesenkrake", listOf(Gegenstand.Hypnosescheibe)),
    Wache("Security Mann", listOf(Gegenstand.Goldsack)),
    Wache("Roboter", listOf(Gegenstand.Fernbedienung)),
    Wache("Troll", listOf(Gegenstand.Diamant, Gegenstand.Messer))
)

data class Punkt(val x: Int, val y: Int)

fun plus(a: Punkt, b: Punkt) = Punkt(a.x + b.x, a.y + b.y)

@Suppress("unused")
enum class Richtung(val punkt: Punkt) {
    N(Punkt(0, -1)),
    O(Punkt(1, 0)),
    S(Punkt(0, 1)),
    W(Punkt(-1, 0))
}

@Suppress("unused")
enum class RelativeRichtung(val wert: Int, val taste: String) {
    vorne(0, "w"),
    rechts(1, "d"),
    hinten(2, "s"),
    links(3, "a")
}

fun relativeRichtung(spieler: Richtung, tür: Richtung): RelativeRichtung =
    RelativeRichtung.values().first { (spieler.ordinal + it.wert) % 4 == tür.ordinal }

@Suppress("unused")
val mensch = Spieler(steuerung = object : Steuerung {
    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        nameUndTaste: (T) -> Pair<String, String?>
    ): Auswahl<T> {
        val namenUndTasten = auswahl.map { it to nameUndTaste(it) }
        val kurz = namenUndTasten.mapIndexed { index, s ->
            (s.second.second ?: (index + 1).toString()) to s.second.first
        }.toMap()
        val namen = namenUndTasten.map { it.second.first to it.first }.toMap()

        ausgabe { "$nachricht ${kurz.entries.joinToString { "${it.key}=${it.value}" }}" }

        while (true) {
            val eingabe = readLine()
            if (eingabe in kurz) {
                return Auswahl.Weiter(namen.getValue(kurz.getValue(eingabe!!)))
            }
            ausgabe { "Falsche Eingabe. Versuche es noch einmal." }
        }
    }

    override fun neuerRaum(spiel: Spiel, raum: Raum, richtung: Richtung): Spiel {
        return kopiere(spiel, spieler = spiel.spieler.copy(
            raum = raum.punkt,
            richtung = richtung))
    }

    override fun ausgabe(nachricht: () -> String) {
        println(nachricht())
    }
})

data class Entscheidung(
    val pfad: List<Any>,
    var fertig: Boolean = false,
    var ausprobiert: Map<Any, Entscheidung>? = null,
    val vorige: Entscheidung?,
    var spiel: Spiel? = null
) {
    override fun toString(): String {
        return pfad.toString()
    }
}

sealed class Auswahl<T> {
    class Weiter<T>(val wahl: T) : Auswahl<T>()
    class ZurückAusRaum<T>(val spiel: Spiel) : Auswahl<T>()
}

fun zurück(computerSteuerung: ComputerSteuerung, entscheidung: Entscheidung): Spiel {
    if (!entscheidung.fertig && entscheidung.ausprobiert?.any { !it.value.fertig } == true) {
        val spiel = entscheidung.spiel!!
        spiel.spieler.steuerung.ausgabe { "zurück zu $entscheidung / ${spiel.spieler.raum} / ${spiel.spieler.gegenstand}" }
        computerSteuerung.aktuelleEntscheidung = entscheidung
        return kopiere(spiel, phaseGesetzt = true)
    } else {
        entscheidung.fertig = true
    }

    return entscheidung.vorige?.let { zurück(computerSteuerung, it) }
        ?: beste(computerSteuerung)?.let { beste ->
            val l = computerSteuerung.lösungen
            computerSteuerung.ausgabe {
                "Lösung: $beste. Länge: ${beste.pfad.size} " +
                    "Lösungen: ${l.joinToString { it.toString() + "/" + it.pfad.size }}"
            }
            beste.spiel!!
        }
        ?: entscheidung.spiel!!.copy(aufgeben = true)
}

private fun beste(computerSteuerung: ComputerSteuerung): Entscheidung? {
    return computerSteuerung.lösungen.minBy { it.pfad.size }
}

fun schonMal(entscheidung: Entscheidung?, spiel: Spiel): Boolean {
    if (entscheidung == null) {
        return false
    }

    return entscheidung.spiel == spiel || schonMal(entscheidung.vorige, spiel)
}

class ComputerSteuerung(private val detail: Boolean = false, private val schnell: Boolean = true) : Steuerung {
    var aktuelleEntscheidung: Entscheidung? = null
    private var züge = 0
    val lösungen: MutableList<Entscheidung> = mutableListOf()

    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        nameUndTaste: (T) -> Pair<String, String?>
    ): Auswahl<T> {
        if (züge >= 500) {
            return Auswahl.ZurückAusRaum(spiel.copy(aufgeben = true))
        }
        züge++

        val a = aktuelleEntscheidung!!
        if (schonMal(a.vorige, spiel)) {
            a.fertig = true
            return Auswahl.ZurückAusRaum(zurück(this, a))
        }

        if (a.ausprobiert == null) {
            a.spiel = spiel
            a.ausprobiert = auswahl.map { it as Any to Entscheidung(a.pfad + listOf(it as Any), vorige = a) }.toMap()
        }

        return a.ausprobiert?.entries
            ?.firstOrNull { !it.value.fertig }
            ?.let {
                @Suppress("UNCHECKED_CAST") val wahl = it.key as T
                aktuelleEntscheidung = it.value
                ausgabe { "computer hat $wahl genommen. $aktuelleEntscheidung. Raum:${spiel.spieler.raum}" }
                Auswahl.Weiter(wahl)
            }
            ?: Auswahl.ZurückAusRaum(zurück(this, a))
    }

    override fun neuerRaum(spiel: Spiel, raum: Raum, richtung: Richtung): Spiel {
        if (aktuelleEntscheidung == null) {
            aktuelleEntscheidung = Entscheidung(emptyList(), vorige = null)
        }

        val s = kopiere(spiel, spieler = spiel.spieler.copy(
            raum = raum.punkt))

        if (raum.punkt == spiel.haus.ausgang) {
            val a = aktuelleEntscheidung!!
            a.spiel = s
            lösungen.add(a)
            if (!schnell) {
                return zurück(this, a)
            }
        }
        return s
    }

    override fun ausgabe(nachricht: () -> String) {
        if (detail) {
            println(nachricht())
        }
    }
}

@Suppress("unused")
val zuffalsComputer = Spieler(steuerung = object : Steuerung {
    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        nameUndTaste: (T) -> Pair<String, String?>
    ): Auswahl<T> {
        val wahl = auswahl.random()
        ausgabe { "computer hat $wahl genommen" }
        return Auswahl.Weiter(wahl)
    }

    override fun neuerRaum(spiel: Spiel, raum: Raum, richtung: Richtung): Spiel {
        return kopiere(spiel, spieler = spiel.spieler.copy(
            raum = raum.punkt,
            phase = Phase.values().first()))
    }

    override fun ausgabe(nachricht: () -> String) {
        println(nachricht())
    }

})

private fun neueWache(benötigt: Gegenstand, name: String): (Spieler) -> Boolean = { spieler: Spieler ->
    if (spieler.gegenstand == benötigt) {
        spieler.steuerung.ausgabe { "Du besiegst den $name!" }
        true
    } else {
        spieler.steuerung.ausgabe { "Du siehst einen $name und gehst rückwärts schnell wieder zurück!" }
        false
    }
}

fun main() {
    val limit = 5
    val r = 3
    val probieren = 100
    var räume = mapOf(startPunkt to Raum(startPunkt))

    while (räume.size < r) {
        val raum = räume.values.random()
        val richtung = Richtung.values().random()
        val p = plus(raum.punkt, richtung.punkt)
        if (p.x.absoluteValue > limit || p.y.absoluteValue > limit) {
            continue
        }
        if (raum.türen[richtung] != null) {
            continue
        }
        val ziel = räume[p] ?: Raum(p).also {
            räume = räume.plus(p to it)
        }

        val andersrum = Richtung.values().first { (richtung.ordinal + 2) % 4 == it.ordinal }
        räume = kopiereRäume(räume, listOf(
            raum.copy(türen = raum.türen.plus(richtung to ziel.punkt)),
            ziel.copy(türen = ziel.türen.plus(andersrum to raum.punkt))
        ))
    }

    val (lösbar, best) = plaziereWachen(räume, probieren)

    println("Schaffe es in $best zügen!")

    spiele(Spiel(null, mensch, Haus(lösbar, startPunkt, lösbar.values.last().punkt)))
}

private fun plaziereWachen(räume: Map<Punkt, Raum>, probieren: Int): Pair<Map<Punkt, Raum>, Int> {
    var plaziert: MutableMap<String, Gegenstand> = mutableMapOf()
    var lösbarPlaziert: Map<String, Gegenstand> = mapOf()

    var lösbar = räume
    var aktuell = räume
    var best = 0
    val ausgang = räume.keys.maxBy { it.x.absoluteValue + it.y.absoluteValue }!!
    var probiert = 0
    while (true) {
        val wache = wachen.filterNot { it.name in plaziert.keys }.toList().random()

        val g = wache.gegenstände
            .filterNot { it == Gegenstand.Keinen || it in plaziert.values }
            .takeIf { it.isNotEmpty() }
            ?.random()
            ?: return Pair(lösbar, best)

        val wachenRaum = aktuell.values
            .filter { it.wache == null && it.punkt != startPunkt }
            .takeIf { it.isNotEmpty() }
            ?.random()
            ?: return Pair(lösbar, best)

        val lager = aktuell.values
            .filter { it != wachenRaum }
            .takeIf { it.isNotEmpty() }
            ?.random()
            ?: return Pair(lösbar, best)

        plaziert[wache.name] = g
        aktuell = kopiereRäume(aktuell, listOf(
            wachenRaum.copy(wache = neueWache(g, wache.name)),
            lager.copy(gegenstände = lager.gegenstände + listOf(g))
        ))

        val st = ComputerSteuerung()
        val spiel = Spiel(null, Spieler(steuerung = st), Haus(aktuell, startPunkt, ausgang))

        val b = System.currentTimeMillis()
        val geschafft = spiele(spiel)
        val d = (System.currentTimeMillis() - b) / 1000
        println("Wachen: ${plaziert.size} Dauer: $d")

        if (geschafft) {
            lösbar = aktuell
            lösbarPlaziert = plaziert.toMap()
            best = beste(st)!!.pfad.size
            probiert = 0
        } else {
            probiert++
            aktuell = lösbar
            plaziert = lösbarPlaziert.toMutableMap()
            if (probiert >= probieren) {
                break
            }
        }
    }
    return Pair(lösbar, best)
}

fun kopiere(
    spiel: Spiel,
    spieler: Spieler = spiel.spieler,
    räume: List<Raum> = emptyList(),
    aufgeben: Boolean = spiel.aufgeben,
    phaseGesetzt: Boolean = spiel.phaseGesetzt
): Spiel {
    return Spiel(
        spiel,
        spieler,
        if (räume.isNotEmpty()) spiel.haus.copy(räume = kopiereRäume(spiel.haus.räume, räume)) else spiel.haus,
        aufgeben = aufgeben,
        phaseGesetzt = phaseGesetzt
    )
}

private fun kopiereRäume(räume: Map<Punkt, Raum>, neue: List<Raum>): Map<Punkt, Raum> {
    return räume.map { r ->
        neue.firstOrNull { it.punkt == r.value.punkt }?.let { it.punkt to it }
            ?: r.key to r.value
    }.toMap()
}

fun spiele(spiel: Spiel): Boolean {
    var züge = 0
    var s = spiel.spieler.steuerung.neuerRaum(spiel, spiel.haus.räume.getValue(spiel.haus.eingang), Richtung.O)
    while (s.spieler.raum != s.haus.ausgang && !s.aufgeben) {
        s = s.spieler.phase.aktion(s)
        if (!s.phaseGesetzt) {
            s = nächstePhase(s)
        }
        züge++
    }
    spiel.spieler.steuerung.ausgabe { if (s.aufgeben) "Aufgegeben! $züge Züge gebraucht." else "Endlich wieder frei! $züge Züge gebraucht." }
    return !s.aufgeben
}

private fun raum(spiel: Spiel) = spiel.haus.räume.values.first { it.punkt == spiel.spieler.raum }

private fun türAuswählen(spiel: Spiel): Spiel {
    val spieler = spiel.spieler

    val türWahl = spieler.steuerung.auswahl(spiel,
        "Durch welche Tür möchtest du gehen?", raum(spiel).türen.keys) { richtung ->
        relativeRichtung(spieler.richtung, richtung).let { it.name to it.taste }
    }

    val tür = when (türWahl) {
        is Auswahl.Weiter -> türWahl.wahl
        is Auswahl.ZurückAusRaum -> return türWahl.spiel
    }

    return neuerRaum(tür, spiel)
}

private fun ereignis(spiel: Spiel): Spiel {
    return raum(spiel).ereignis?.let { it(spiel) } ?: spiel
}

private fun truhe(schlüssel: Gegenstand, inhalt: List<Gegenstand>): (Spiel) -> Spiel {
    return geschützesEreignis("Du siehst eine Truhe", schlüssel) { spiel ->
        wähleGegenstand(spiel, inhalt)
    }
}

private fun geschützesEreignis(nachricht: String, gegenstand: Gegenstand, ereignis: (Spiel) -> Spiel): (Spiel) -> Spiel {
    return { spiel ->
        spiel.spieler.steuerung.ausgabe { nachricht }
        if (spiel.spieler.gegenstand == gegenstand) {
            ereignis(spiel)
        } else {
            spiel
        }
    }
}

private fun gegenstandAuswahl(spiel: Spiel): Spiel {
    return wähleGegenstand(spiel, raum(spiel).gegenstände)
}

private fun wähleGegenstand(spiel: Spiel, gegenstände: List<Gegenstand>): Spiel {
    val spieler = spiel.spieler
    spieler.steuerung.ausgabe { "Gegenstand: ${spieler.gegenstand}" }
    if (gegenstände.isNotEmpty()) {
        val auswahl = spieler.steuerung.auswahl(
            spiel,
            "Was willst du mitnehmen?",
            gegenstände + listOf(Gegenstand.Keinen)
        ) { it.name to null }

        when (auswahl) {
            is Auswahl.Weiter -> if (auswahl.wahl != Gegenstand.Keinen) {
                return nimm(spiel, auswahl.wahl)
            }
            is Auswahl.ZurückAusRaum -> return auswahl.spiel
        }
    }
    return spiel
}

private fun nächstePhase(spiel: Spiel) = kopiere(spiel, spiel.spieler.copy(phase = nächste(spiel)))

private fun nächste(spiel: Spiel) = Phase.values()[(spiel.spieler.phase.ordinal + 1) % Phase.values().size]

private fun neuerRaum(tür: Richtung, spiel: Spiel): Spiel {
    val nächster = spiel.haus.räume.getValue(raum(spiel).türen.getValue(tür))
    return spiel.spieler.steuerung.neuerRaum(spiel, nächster, tür)
}

fun wache(spiel: Spiel): Spiel {
    val raum = raum(spiel)
    val spieler = spiel.spieler

    val wache = raum.wache
    return if (wache != null) {
        if (wache(spieler)) {
            kopiere(spiel, räume = listOf(raum.copy(wache = null)))
        } else {
            kopiere(spiel, spieler.copy(raum = letzterRaum(spiel, spieler.raum)))
        }
    } else {
        spiel
    }
}

fun letzterRaum(spiel: Spiel, aktuell: Punkt): Punkt {
    return if (spiel.spieler.raum == aktuell) letzterRaum(spiel.voriges!!, aktuell) else spiel.spieler.raum
}

fun nimm(spiel: Spiel, gegenstand: Gegenstand): Spiel {
    val spieler = spiel.spieler
    val raum = raum(spiel)
    return kopiere(spiel,
        spieler = spieler.copy(gegenstand = gegenstand),
        räume = listOf(raum.copy(gegenstände = raum.gegenstände + listOfNotNull(spieler.gegenstand) - listOf(gegenstand)))
    )
}

//Ideen
//1) 2 Spieler
//2) ein/eine
//3) Truhe
//4) Tauchen
//5) Keller
//6) bessere Eingabe (kein Enter) also nur w
//7) Enter = nix nehme
//8) Freitexteingabe bei Forscher, Hexe






