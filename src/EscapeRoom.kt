@file:Suppress("SpellCheckingInspection", "EnumEntryName", "FunctionName", "NonAsciiCharacters", "LocalVariableName", "ClassName", "PrivatePropertyName", "PropertyName")

import kotlin.math.absoluteValue

data class Spiel(
    val spieler: Spieler,
    val haus: Haus,
    val aufgeben: Boolean = false
)

interface Steuerung {
    fun <T> auswahl(spiel: Spiel, nachricht: String, auswahl: Collection<T>, name: (T) -> String): Auswahl<T>
    fun neuerRaum(spiel: Spiel, raum: Raum, richtung: Richtung): Spiel
    fun ausgabe(nachricht: () -> String)
}

@Suppress("unused")
enum class Phase(val aktion: (Spiel) -> Spiel) {
    gegenstandsAuswahl(::gegenstandAuswahl),
    türAuswählen(::türAuswählen)
}

val punkt0 = Punkt(0, 0)

data class Spieler(
    val steuerung: Steuerung,
    val raum: Punkt = punkt0,
    val phase: Phase,
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
    var gegenstände: List<Gegenstand> = emptyList(),
    var türen: MutableMap<Richtung, Punkt> = mutableMapOf(),
    var wache: ((Spieler) -> Boolean)? = null
) {
    override fun toString(): String {
        return punkt.toString()
    }
}

@Suppress("unused")
enum class Gegenstand {
    Armband,
    Brief,
    Cello,
    Dudelsack,
    Ei,
    Fahne,
    Goldsack,
    Heft,
    Imbus,
    Joker,
    Keinen,
    Lampe,
    Messer,
    Kompass,
    Ring,
    Teller,
    Waffe
}

val wachen = listOf(
    "Troll",
    "Elfe",
    "Fee",
    "Zwerg",
    "Dinosaurier",
    "Killerhai",
    "Monster",
    "Einhorn",
    "Biest",
    "Hexe",
    "Kröte",
    "Drache",
    "Maikäfer",
    "König",
    "Königin",
    "Prinz",
    "Prinzessin"
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
enum class RelativeRichtung(val wert: Int) {
    vorne(0),
    rechts(1),
    hinten(2),
    links(3)
}

fun relativeRichtung(spieler: Richtung, tür: Richtung): RelativeRichtung =
    RelativeRichtung.values().first { (spieler.ordinal + it.wert) % 4 == tür.ordinal }

@Suppress("unused")
val mensch = Spieler(phase = Phase.values().first(), steuerung = object : Steuerung {
    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        name: (T) -> String
    ): Auswahl<T> {
        val namen = auswahl.map { name(it) to it }.toMap()
        val kurz = namen.keys.map { it.substring(0, 1).toLowerCase() to it }.toMap()

        ausgabe { "$nachricht ${namen.keys.joinToString(", ")} (${kurz.keys.joinToString("/")})" }

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
            richtung = richtung,
            phase = Phase.values().first()))
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
        return spiel
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
        name: (T) -> String
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
            raum = raum.punkt,
            phase = Phase.values().first()))

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
val zuffalsComputer = Spieler(phase = Phase.values().first(), steuerung = object : Steuerung {
    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        name: (T) -> String
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

private fun wache(benötigt: Gegenstand, name: String): (Spieler) -> Boolean = { spieler: Spieler ->
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
    val r = 10
    val probieren = 100
    val räume = mutableMapOf(punkt0 to Raum(punkt0))

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
        val ziel = räume[p] ?: Raum(p).also { räume[p] = it }

        raum.türen[richtung] = ziel.punkt
        ziel.türen[Richtung.values().first { (richtung.ordinal + 2) % 4 == it.ordinal }] = raum.punkt
    }

    var (lösbar, best) = plaziereWachen(räume, probieren)

    println("Schaffe es in $best zügen!")

    spiele(Spiel(mensch, Haus(lösbar, punkt0, lösbar.values.last().punkt)))
}

private fun plaziereWachen(räume: MutableMap<Punkt, Raum>, probieren: Int): Pair<Map<Punkt, Raum>, Int> {
    val plaziert: MutableMap<String, Gegenstand> = mutableMapOf()

    var lösbar = räume.toMap()
    var best = 0
    val ausgang = räume.values.last().punkt
    var probiert = 0
    while (true) {
        val wache = wachen.filterNot { it in plaziert.keys }.toList().random()

        val g = Gegenstand.values()
            .filterNot { it == Gegenstand.Keinen || it in plaziert.values }
            .takeIf { it.isNotEmpty() }
            ?.toList()?.random()
            ?: return Pair(lösbar, best)

        val wachenRaum = räume.values
            .filter { it.wache == null  }
            .takeIf { it.isNotEmpty() }
            ?.toList()?.random()
            ?: return Pair(lösbar, best)
        
        val lager = räume.values
            .filter { it != wachenRaum }
            .takeIf { it.isNotEmpty() }
            ?.toList()?.random()
            ?: return Pair(lösbar, best)

        plaziert[wache] = g
        wachenRaum.wache = wache(g, wache)
        lager.gegenstände = lager.gegenstände + listOf(g)

        val st = ComputerSteuerung()
        val spiel = Spiel(Spieler(steuerung = st, phase = Phase.values().first()), Haus(räume, punkt0, ausgang))

        val b = System.currentTimeMillis()
        val geschafft = spiele(spiel)
        val d = (System.currentTimeMillis() - b) / 1000
        println("Wachen: ${plaziert.size} Dauer: $d")

        if (geschafft) {
            lösbar = kopiereRäume(räume)
            best = beste(st)!!.pfad.size
            probiert = 0
        } else {
            probiert++
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
    aufgeben: Boolean = false
): Spiel {
    return Spiel(
        spieler,
        if (räume.isNotEmpty()) spiel.haus.copy(räume = kopiereRäume(spiel.haus.räume, räume)) else spiel.haus,
        aufgeben = aufgeben
    )
}

private fun kopiereRäume(räume: Map<Punkt, Raum>, neue: List<Raum> = emptyList()): Map<Punkt, Raum> {
    return räume.map { r ->
        neue.firstOrNull { it.punkt == r.value.punkt }
            ?.let { it.punkt to it }
            ?: r.key to r.value.copy(gegenstände = r.value.gegenstände.toMutableList())
    }.toMap()
}

fun spiele(spiel: Spiel): Boolean {
    var züge = 0
    var s = spiel.spieler.steuerung.neuerRaum(spiel, spiel.haus.räume.getValue(spiel.haus.eingang), Richtung.O)
    while (s.spieler.raum != s.haus.ausgang && !s.aufgeben) {
        s = s.spieler.phase.aktion(s)
        züge++
    }
    spiel.spieler.steuerung.ausgabe { if (s.aufgeben) "Aufgegeben! $züge Züge gebraucht." else "Endlich wieder frei! $züge Züge gebraucht." }
    return !s.aufgeben
}

private fun raum(spiel: Spiel) = spiel.haus.räume.values.first { it.punkt == spiel.spieler.raum }

private fun türAuswählen(spiel: Spiel): Spiel {
    val spieler = spiel.spieler

    val türWahl = spieler.steuerung.auswahl(spiel,
        "Durch welche Tür möchtest du gehen?", raum(spiel).türen.keys) {
        relativeRichtung(spieler.richtung, it).name
    }

    val tür = when (türWahl) {
        is Auswahl.Weiter -> türWahl.wahl
        is Auswahl.ZurückAusRaum -> return türWahl.spiel
    }

    return neuerRaum(tür, spiel)
}

private fun gegenstandAuswahl(spiel: Spiel): Spiel {
    val raum = raum(spiel)
    val spieler = spiel.spieler
    spiel.spieler.steuerung.ausgabe { "Gegenstand: ${spieler.gegenstand}" }

    if (raum.gegenstände.isNotEmpty()) {
        val auswahl = spieler.steuerung.auswahl(
            spiel,
            "Was willst du mitnehmen?",
            raum.gegenstände + listOf(Gegenstand.Keinen),
            Gegenstand::name
        )
        when (auswahl) {
            is Auswahl.Weiter -> if (auswahl.wahl != Gegenstand.Keinen) {
                return nimm(spieler, auswahl.wahl, raum, spiel)
            }
            is Auswahl.ZurückAusRaum -> return auswahl.spiel
        }
    }
    return kopiere(spiel, spieler.copy(phase = nächste(spiel)))
}

private fun nächste(spiel: Spiel) = Phase.values()[spiel.spieler.phase.ordinal + 1]

private fun neuerRaum(tür: Richtung, spiel: Spiel): Spiel {
    val nächster = spiel.haus.räume.getValue(raum(spiel).türen.getValue(tür))
    val spieler = spiel.spieler

    val wache = nächster.wache
    if (wache != null) {
        return if (wache(spieler)) {
            spieler.steuerung.neuerRaum(kopiere(spiel, räume = listOf(nächster.copy(wache = null))), nächster, tür)
        } else {
            spiel
        }
    }

    return spieler.steuerung.neuerRaum(spiel, nächster, tür)
}

fun nimm(spieler: Spieler, gegenstand: Gegenstand, raum: Raum, spiel: Spiel): Spiel {
    return kopiere(spiel,
        spieler = spieler.copy(gegenstand = gegenstand, phase = nächste(spiel)),
        räume = listOf(raum.copy(gegenstände = raum.gegenstände + listOfNotNull(spieler.gegenstand) - listOf(gegenstand)))
    )
}






