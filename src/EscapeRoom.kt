@file:Suppress("SpellCheckingInspection", "EnumEntryName", "FunctionName", "NonAsciiCharacters", "LocalVariableName", "ClassName", "PrivatePropertyName", "PropertyName", "ObjectPropertyName")

import kotlin.math.absoluteValue

data class Spiel(
    val voriges: Spiel?,
    val spieler: Spieler,
    val alleSpieler: List<Spieler>,
    val haus: Haus,
    val aufgeben: Boolean = false,
    val phaseGesetzt: Boolean = false,
    val schwierigkeit: Schwierigkeit
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Spiel

        if (alleSpieler != other.alleSpieler) return false
        if (haus != other.haus) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alleSpieler.hashCode()
        result = 31 * result + haus.hashCode()
        return result
    }
}

interface Steuerung {
    fun <T> auswahl(spiel: Spiel, nachricht: String, auswahl: Collection<T>, anzeige: (T, Int) -> AuswahlAnzeige<T>, aktion: (T) -> Spiel): Spiel
    fun fertig(spiel: Spiel): Spiel?
    fun ausgabe(nachricht: () -> String)
}

@Suppress("unused")
enum class Phase(val aktion: (Spiel) -> Spiel) {
    wache(::wache),
    gegenstandsAuswahl(::gegenstandAuswahl),
    truhe(::truhe),
    türAuswählen(::türAuswählen)
}

@Suppress("unused")
enum class Schwierigkeit(
    val richtungAnzeigen: (richtung: Richtung, spiel: Spiel) -> AuswahlAnzeige<Richtung>,
    val aktuellerRaum: (Raum, Spieler) -> String,
    val nächsterRaum: (Raum) -> String,
    val truhe: (Truhe) -> String,
    val intro: (Entscheidung) -> String
) {
    leicht(::absoluteRichtungAnzeigen,
        { r, s ->
            "Spieler ${s.name}, du befindest dich bei ${r.punkt}. Gegenstand: ${s.gegenstand}"
        },
        { r -> r.wache?.let { "(${it.name} benötigt ${it.benötigt})" } ?: "" },
        { "Du siehst eine Truhe, die sich durch einen ${it.schlüssel} öffnen lässt" },
        {
            "Schaffe es in ${it.pfad.size} zügen! " +
                "\nDer Ausgang ist bei ${it.spiel!!.haus.ausgang}. " +
                "\nEine Möglichkeit wäre ${it.pfad}" +
                "\nDas Haus: ${it.spiel.haus.räume.values}"
        }
    ),
    mittel(::relativeRichtungAnzeigen,
        { r, s ->
            "Spieler ${s.name}, du befindest dich in einem ${r.name} Raum. Gegenstand: ${s.gegenstand}"
        },
        { r -> "(ein ${r.name} Raum)" },
        { "Du siehst eine Truhe" },
        { "Schaffe es in ${it.pfad.size} zügen!" }
    ),
    schwer(::relativeRichtungAnzeigen,
        { _, s ->
            "Spieler ${s.name}. Gegenstand: ${s.gegenstand}"
        },
        { "" },
        { "Du siehst eine Truhe" },
        { "Viel Erfolg!" }
    ),
}

val startPunkt = Punkt(0, 0)

data class Spieler(
    val name: String,
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
    val name: String,
    val punkt: Punkt,
    val gegenstände: List<Gegenstand> = emptyList(),
    val türen: Map<Richtung, Punkt> = mapOf(),
    val wache: Wache? = null,
    val truhe: Truhe? = null
)

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

    SilberSchlüssel,
    GoldSchlüssel,
    PlatinSchlüssel,
    EisenSchlüssel,
    MessingSchlüssel,

    Keinen
}

val schlüssel = listOf(
    Gegenstand.SilberSchlüssel,
    Gegenstand.GoldSchlüssel,
    Gegenstand.PlatinSchlüssel,
    Gegenstand.EisenSchlüssel,
    Gegenstand.MessingSchlüssel
)

val raumEigenschaften = listOf(
    "gewölbten",
    "kahlen",
    "bunten",
    "hellen",
    "dunklen",
    "großen",
    "kleinen",
    "stinkenden",
    "engen",
    "nassen",
    "warmen",
    "roten",
    "gelben",
    "grünen",
    "kalten",
    "monotonen",
    "weißen",
    "schwarzen"
)

data class MöglicheWache(val name: String, val gegenstände: List<Gegenstand>)


val wachen = listOf(
    MöglicheWache("Skatrunde", listOf(Gegenstand.Joker)),
    MöglicheWache("Horde", listOf(Gegenstand.Maske)),
    MöglicheWache("Dimensionswandler", listOf(Gegenstand.Erfindung)),
    MöglicheWache("Flaschengeist", listOf(Gegenstand.Flasche)),
    MöglicheWache("Schlange", listOf(Gegenstand.Flöte)),
    MöglicheWache("Riesenkrake", listOf(Gegenstand.Hypnosescheibe)),
    MöglicheWache("Security Mann", listOf(Gegenstand.Goldsack)),
    MöglicheWache("Roboter", listOf(Gegenstand.Fernbedienung)),
    MöglicheWache("Troll", listOf(Gegenstand.Diamant, Gegenstand.Messer))
)

@Suppress("unused")
enum class HausElement(
    val relativeWahrscheinlichkeit: Int,
    val anwenden: (Map<Punkt, Raum>, Punkt) -> Map<Punkt, Raum>?) {
    wache(10, ::plaziereWache),
    truhe(4, ::plaziereTruhe),
}

data class Punkt(val x: Int, val y: Int) {
    override fun toString(): String {
        return "$x/$y"
    }
}

fun plus(a: Punkt, b: Punkt) = Punkt(a.x + b.x, a.y + b.y)

@Suppress("unused")
enum class Richtung(val punkt: Punkt, val taste: String, val reihenfolge: String) {
    N(Punkt(0, -1), "w", "0"),
    O(Punkt(1, 0), "d", "3"),
    S(Punkt(0, 1), "s", "2"),
    W(Punkt(-1, 0), "a", "1")
}

@Suppress("unused")
enum class RelativeRichtung(val wert: Int, val taste: String, val reihenfolge: String) {
    vorne(0, "w", "0"),
    rechts(1, "d", "3"),
    hinten(2, "s", "2"),
    links(3, "a", "1")
}

fun relativeRichtung(spieler: Richtung, tür: Richtung): RelativeRichtung =
    RelativeRichtung.values().first { (spieler.ordinal + it.wert) % 4 == tür.ordinal }

data class AuswahlAnzeige<T>(
    val wahl: T,
    val name: String,
    val taste: String,
    val reihenfolge: String)

class MenschSteuerung : Steuerung {
    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        anzeige: (T, Int) -> AuswahlAnzeige<T>,
        aktion: (T) -> Spiel
    ): Spiel {
        val eingabe = eingabe(nachricht, auswahl, anzeige)
        return aktion(eingabe)
    }

    override fun fertig(spiel: Spiel): Spiel {
        return spiel
    }

    override fun ausgabe(nachricht: () -> String) {
        println(nachricht())
    }
}

private fun <T> eingabe(nachricht: String, auswahl: Collection<T>, anzeige: (T, Int) -> AuswahlAnzeige<T>): T {
    val anzeigen = auswahl.mapIndexed { index, t -> anzeige(t, index + 1) }.sortedBy { it.reihenfolge }

    println("$nachricht ${anzeigen.joinToString { "${it.taste}=${it.name}" }}")

    while (true) {
        val eingabe = readLine()
        val w = anzeigen.firstOrNull { it.taste == eingabe }
        if (w != null) {
            return w.wahl
        }
        println("Falsche Eingabe. Versuche es noch einmal.")
    }
}

data class Entscheidung(
    val spiel: Spiel?,
    val vorige: Entscheidung?,
    val raum: Punkt,
    val wert: Any?,
    var fertig: Boolean = false,
    var ausprobiert: Map<Any, Entscheidung>? = null
) {
    override fun toString(): String {
        return pfad.toString()
    }

    val pfad: List<Pair<Punkt, Any>>
        get() = (vorige?.pfad ?: emptyList()) + listOfNotNull(wert?.let { Pair(raum, it) })
}

private fun beste(computerSteuerung: ComputerSteuerung): Entscheidung? {
    return computerSteuerung.lösungen.minBy { it.pfad.size }
}

private fun allesGebraucht(räaume: Collection<Raum>): Boolean {
    return räaume.all { r -> (r.truhe?.inhalt?.size ?: 0) == 0 } &&
        räaume.all { it.wache == null }
}

fun schonMal(entscheidung: Entscheidung?, spiel: Spiel): Boolean {
    if (entscheidung == null) {
        return false
    }

    return entscheidung.spiel == spiel || schonMal(entscheidung.vorige, spiel)
}

class ComputerSteuerung(private val detail: Boolean, private val schnell: Boolean) : Steuerung {
    private var aktuelleEntscheidung: Entscheidung = Entscheidung(null, null, startPunkt, null)
    private var züge = 0
    val lösungen: MutableList<Entscheidung> = mutableListOf()

    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        anzeige: (T, Int) -> AuswahlAnzeige<T>,
        aktion: (T) -> Spiel
    ): Spiel {
        if (züge >= 500) {
            return spiel.copy(aufgeben = true)
        }
        züge++

        val a = aktuelleEntscheidung

        if (a.ausprobiert == null) {
            a.ausprobiert = auswahl.mapNotNull { w ->
                aktion(w).takeUnless { schonMal(a, it) }
                    ?.let { w as Any to Entscheidung(it, a, spiel.spieler.raum, w) }
            }.toMap()
        }

        return baumSuche(a)
    }

    private fun baumSuche(a: Entscheidung): Spiel {
        check(a.ausprobiert != null) { "Programmierfehler $a" }

        val wahl = a.ausprobiert!!.entries.firstOrNull { !it.value.fertig }
        if (wahl == null) {
            a.fertig = true
        }

        return wahl
            ?.let { entry ->
                aktuelleEntscheidung = entry.value
                entry.value.spiel!!.also {
                    ausgabe { "computer hat ${entry.key} genommen. $aktuelleEntscheidung. Raum:${it.spieler.raum}" }
                }
            }
            ?: a.vorige?.let {
                it.spiel?.let { spiel ->
                    spiel.spieler.steuerung.ausgabe { "zurück zu $it / ${spiel.spieler.raum} / ${spiel.spieler.gegenstand}" }

                    baumSuche(it)
                }
            }
            ?: beste(this)?.let { beste ->
                val l = lösungen
                ausgabe {
                    "Lösung: $beste. Länge: ${beste.pfad.size} " +
                        "Lösungen: ${l.joinToString { it.toString() + "/" + it.pfad.size }}"
                }
                beste.spiel!!
            }
            ?: a.spiel!!.copy(aufgeben = true)
    }

    override fun fertig(spiel: Spiel): Spiel? {
        if (!allesGebraucht(spiel.haus.räume.values)) {
            return spiel.copy(aufgeben = true)
        }

        if (schnell) {
            return spiel
        }
        val a = aktuelleEntscheidung
        lösungen.add(a)
        a.fertig = true
        return baumSuche(a.vorige!!)
    }

    override fun ausgabe(nachricht: () -> String) {
        if (detail) {
            println(nachricht())
        }
    }
}

@Suppress("unused")
val zuffalsComputer = Spieler(name = "Zufalls KI", steuerung = object : Steuerung {
    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        anzeige: (T, Int) -> AuswahlAnzeige<T>,
        aktion: (T) -> Spiel
    ): Spiel {
        val wahl = auswahl.random()
        ausgabe { "computer hat $wahl genommen" }
        return aktion(wahl)
    }

    override fun fertig(spiel: Spiel): Spiel {
        return spiel
    }

    override fun ausgabe(nachricht: () -> String) {
        println(nachricht())
    }

})

data class Wache(val benötigt: Gegenstand, val name: String)

data class Truhe(val schlüssel: Gegenstand, val inhalt: List<Gegenstand>)

fun main() {
    val (räume, lösung) = plaziereElemente(generiereRäume())

    val steuerung = MenschSteuerung()

    val spieler = (1..eingabe("Wie viele Spieler", (1..4).toList()) { i, _ ->
        AuswahlAnzeige(i, i.toString(), i.toString(), i.toString())
    }).map { Spieler(it.toString(), steuerung) }

    val sch = eingabe("Wähle eine Schwierigkeit", Schwierigkeit.values().toList()) { s, i ->
        AuswahlAnzeige(s, s.name, i.toString(), i.toString())
    }

    println(sch.intro(lösung))

    spiele(Spiel(null, spieler.first(), spieler, Haus(räume, startPunkt, räume.values.last().punkt),
        schwierigkeit = sch))
}

private fun generiereRäume(): Map<Punkt, Raum> {
    val limit = 2
    val zahl = 4
    var räume = mapOf(startPunkt to Raum(raumEigenschaften.first(), startPunkt))

    while (räume.size < zahl) {
        val raum = räume.values.random()
        val richtung = Richtung.values().random()
        val p = plus(raum.punkt, richtung.punkt)
        if (p.x.absoluteValue > limit || p.y.absoluteValue > limit) {
            continue
        }
        if (raum.türen[richtung] != null) {
            continue
        }

        val ziel = räume[p]
            ?: Raum(raumEigenschaften.filterNot { e -> räume.values.any { e == it.name } }.random(), p).also {
                räume = räume.plus(p to it)
            }

        val andersrum = Richtung.values().first { (richtung.ordinal + 2) % 4 == it.ordinal }
        räume = kopiereRäume(räume, listOf(
            raum.copy(türen = raum.türen.plus(richtung to ziel.punkt)),
            ziel.copy(türen = ziel.türen.plus(andersrum to raum.punkt))
        ))
    }
    return räume
}

private fun plaziereElemente(räume: Map<Punkt, Raum>): Pair<Map<Punkt, Raum>, Entscheidung> {
    val probieren = 100
    var lösbar = räume
    var aktuell = räume
    var best: Entscheidung? = null
    val ausgang = räume.keys.maxBy { it.x.absoluteValue + it.y.absoluteValue }!!
    var probiert = 0

    while (probiert < probieren) {
        probiert++

        val st = ComputerSteuerung(detail = true, schnell = false)
        val ki = Spieler("KI", steuerung = st)
        spiele(Spiel(null, ki, listOf(ki), Haus(aktuell, startPunkt, ausgang), schwierigkeit = Schwierigkeit.leicht))

        val b = beste(st)
        if (b != null) {
            best = b
            lösbar = aktuell
            aktuell = plaziereElement(aktuell, ausgang) ?: return Pair(lösbar, best)
        } else {
            aktuell = lösbar
        }
    }
    return Pair(lösbar, best!!)
}

private fun plaziereElement(aktuell: Map<Punkt, Raum>, ausgang: Punkt): Map<Punkt, Raum>? {
    val hausElemente = HausElement.values().map { e -> List(e.relativeWahrscheinlichkeit) { e } }.flatten()

    val probieren = 100
    var probiert = 0
    while (probiert < probieren) {
        probiert++
        hausElemente.random().anwenden(aktuell, ausgang)?.let {
            return it
        }
    }
    return null
}

fun plaziereWache(räume: Map<Punkt, Raum>, ziel: Punkt): Map<Punkt, Raum>? {
    val plaziert = räume.values.mapNotNull { r ->
        r.wache?.let { it.name to it.benötigt }
    }.toMap()
    val wache = wachen.filterNot { it.name in plaziert.keys }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val g = wache.gegenstände
        .filterNot { it == Gegenstand.Keinen || it in plaziert.values || it in schlüssel }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val wachenRaum = räume.values
        .filter { it.wache == null && it.punkt != startPunkt && it.punkt != ziel }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val lager = räume.values
        .filter { it != wachenRaum && it.punkt != ziel }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    return kopiereRäume(räume, listOf(
        wachenRaum.copy(wache = Wache(g, wache.name)),
        lager.copy(gegenstände = lager.gegenstände + listOf(g))
    ))
}

fun plaziereTruhe(räume: Map<Punkt, Raum>, ziel: Punkt): Map<Punkt, Raum>? {
    val plaziert = räume.values.mapNotNull { it.truhe?.schlüssel }

    val schlüssel = schlüssel
        .filterNot { it in plaziert }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val raum = räume.values
        .filter { it.truhe == null && it.punkt != ziel }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val alterRaum = räume.values
        .filter { it != raum && it.gegenstände.isNotEmpty() }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val schlüsselRaum = räume.values
        .filter { it != raum && it != alterRaum && it.punkt != ziel }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val g = alterRaum.gegenstände.random()

    return kopiereRäume(räume, listOf(
        raum.copy(truhe = Truhe(schlüssel, listOf(g))),
        alterRaum.copy(gegenstände = alterRaum.gegenstände - listOf(g)),
        schlüsselRaum.copy(gegenstände = schlüsselRaum.gegenstände + listOf(schlüssel))
    ))
}

fun kopiere(
    spiel: Spiel,
    spieler: Spieler? = null,
    räume: List<Raum> = emptyList(),
    aufgeben: Boolean = spiel.aufgeben,
    phaseGesetzt: Boolean = spiel.phaseGesetzt
): Spiel {
    return Spiel(
        spiel,
        spieler ?: spiel.spieler,
        if (spieler != null) spiel.alleSpieler.map { if (it.name == spieler.name) spieler else it } else spiel.alleSpieler,
        if (räume.isNotEmpty()) spiel.haus.copy(räume = kopiereRäume(spiel.haus.räume, räume)) else spiel.haus,
        aufgeben = aufgeben,
        phaseGesetzt = phaseGesetzt,
        schwierigkeit = spiel.schwierigkeit
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

    var s = spiel
    for (sp in spiel.alleSpieler) {
        s = neuerRaum(s, spiel.haus.räume.getValue(spiel.haus.eingang))
    }

    while (!s.aufgeben) {
        if (fertig(s)) {
            s = s.spieler.steuerung.fertig(s) ?: return false
            if (fertig(s)) {
                break
            }
        }

        s = s.spieler.phase.aktion(s)
        s = if (!s.phaseGesetzt) {
            kopiere(s, s.spieler.copy(phase = nächste(s)))
        } else {
            kopiere(s, phaseGesetzt = false)
        }
        if (s.spieler.phase.ordinal == 0) {
            val alle = s.alleSpieler
            val spieler = alle[(alle.indexOf(s.spieler) + 1) % alle.size]

            s = s.copy(spieler = spieler)
        }
        züge++
    }
    spiel.spieler.steuerung.ausgabe {
        if (s.aufgeben) "Aufgegeben! $züge Züge gebraucht."
        else "Endlich wieder frei! $züge Züge gebraucht."
    }
    return !s.aufgeben
}

private fun fertig(s: Spiel) = s.spieler.raum == s.haus.ausgang

private fun raum(spiel: Spiel) = spiel.haus.räume.values.first { it.punkt == spiel.spieler.raum }

private fun türAuswählen(spiel: Spiel): Spiel {
    return spiel.spieler.steuerung.auswahl(spiel,
        "Durch welche Tür möchtest du gehen?", raum(spiel).türen.keys, { richtung, _ ->
        spiel.schwierigkeit.richtungAnzeigen(richtung, spiel)
    }, { neuerRaum(spiel, raumHinterTür(spiel, it)) })
}

private fun absoluteRichtungAnzeigen(richtung: Richtung, spiel: Spiel): AuswahlAnzeige<Richtung> =
    AuswahlAnzeige(richtung, richtung.name + raumVorschau(spiel, richtung), richtung.taste, richtung.reihenfolge)

private fun relativeRichtungAnzeigen(richtung: Richtung, spiel: Spiel): AuswahlAnzeige<Richtung> =
    relativeRichtung(spiel.spieler.richtung, richtung).let {
        AuswahlAnzeige(richtung, it.name + raumVorschau(spiel, richtung), it.taste, it.reihenfolge)
    }

private fun raumVorschau(spiel: Spiel, richtung: Richtung) =
    spiel.schwierigkeit.nächsterRaum(raumHinterTür(spiel, richtung))

private fun truhe(spiel: Spiel): Spiel {
    return raum(spiel).truhe?.let {
        geschützesEreignis({ spiel.schwierigkeit.truhe(it) }, it.schlüssel) { spiel ->
            spiel.spieler.steuerung.ausgabe { "Du hast die Truhe geöffnet!" }
            wähleGegenstand(spiel, it.inhalt)
        }(spiel)
    } ?: spiel
}

private fun geschützesEreignis(nachricht: () -> String, gegenstand: Gegenstand, ereignis: (Spiel) -> Spiel): (Spiel) -> Spiel {
    return { spiel ->
        spiel.spieler.steuerung.ausgabe(nachricht)
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

    if (gegenstände.isNotEmpty()) {
        return spieler.steuerung.auswahl(
            spiel,
            "Was willst du mitnehmen?",
            gegenstände + listOf(Gegenstand.Keinen),
            { g, i -> AuswahlAnzeige(g, g.name, if (g == Gegenstand.Keinen) "" else i.toString(), i.toString()) },
            {
                if (it != Gegenstand.Keinen) {
                    nimm(spiel, it)
                } else {
                    spiel
                }
            }
        )
    } else {
        return spiel
    }
}

private fun nächste(spiel: Spiel) = Phase.values()[(spiel.spieler.phase.ordinal + 1) % Phase.values().size]

private fun neuerRaum(spiel: Spiel, raum: Raum) =
    kopiere(spiel, spieler = spiel.spieler.copy(raum = raum.punkt))

private fun raumHinterTür(spiel: Spiel, tür: Richtung) = spiel.haus.räume.getValue(raum(spiel).türen.getValue(tür))

fun wache(spiel: Spiel): Spiel {
    val raum = raum(spiel)
    val spieler = spiel.spieler
    spieler.steuerung.ausgabe { spiel.schwierigkeit.aktuellerRaum(raum, spieler) }

    val wache = raum.wache
    return if (wache != null) {
        if (spieler.gegenstand == wache.benötigt) {
            spieler.steuerung.ausgabe { "Du besiegst den ${wache.name}!" }
            kopiere(spiel, räume = listOf(raum.copy(wache = null)))
        } else {
            spieler.steuerung.ausgabe { "Du siehst einen ${wache.name} und gehst rückwärts schnell wieder zurück!" }
            kopiere(spiel, spieler.copy(raum = letzterRaum(spiel, spieler.raum), phase = Phase.values().first()), phaseGesetzt = true)
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
//2) ein/eine
//4) Tauchen
//5) Keller
//6) bessere Eingabe (kein Enter) also nur w
//8) Freitexteingabe bei Forscher, Hexe

