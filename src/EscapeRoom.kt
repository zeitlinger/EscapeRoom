@file:Suppress("SpellCheckingInspection", "EnumEntryName", "FunctionName", "NonAsciiCharacters", "LocalVariableName", "ClassName", "PrivatePropertyName", "PropertyName", "ObjectPropertyName")

import kotlin.math.absoluteValue

data class Spiel(
    val voriges: Spiel?,
    val spieler: Spieler,
    val alleSpieler: List<Spieler>,
    val haus: Haus,
    val aufgeben: Boolean = false,
    val phaseGesetzt: Boolean = false,
    val schwierigkeit: Schwierigkeit,
    val phase: Phase
) {
    override fun toString(): String {
        return "Spiel(spieler=$spieler, alleSpieler=$alleSpieler, haus=$haus, aufgeben=$aufgeben, " +
            "phaseGesetzt=$phaseGesetzt, schwierigkeit=$schwierigkeit, phase=$phase)"
    }
}

interface Steuerung {
    fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        anzeige: (T, Int) -> AuswahlAnzeige<T>,
        aktion: (T) -> Spiel): Spiel

    fun fertig(spiel: Spiel): Spiel?
    fun ausgabe(nachricht: () -> String)
    fun drehen(spiel: Spiel, richtung: Richtung): Spiel
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
    val intro: (Entscheidung, Haus) -> String
) {
    leicht(::absoluteRichtungAnzeigen,
        { r, s ->
            "Spieler ${s.name}, du befindest dich bei ${r.punkt}. Gegenstand: ${s.gegenstand}"
        },
        { r -> r.wache?.let { "(${it.name} benötigt ${it.benötigt})" } ?: "" },
        { "Du siehst eine Truhe, die sich durch einen ${it.schlüssel} öffnen lässt" },
        { e, h ->
            "Schaffe es in ${e.pfadLänge} zügen! " +
                "\nDer Ausgang ist bei ${h.ausgang}. " +
                "\nEine Möglichkeit wäre ${e.pfad}" +
                "\nElemente ${elemente(h)}" +
                "\nDas Haus: \n${h.räume.values.joinToString("\n")}"
        }
    ),
    mittel(::relativeRichtungAnzeigen,
        { r, s ->
            "Spieler ${s.name}, du befindest dich in einem ${r.name} Raum. Gegenstand: ${s.gegenstand}"
        },
        { r -> "(ein ${r.name} Raum)" },
        { "Du siehst eine Truhe" },
        { e, _ -> "Schaffe es in ${e.pfadLänge} zügen!" }
    ),
    schwer(::relativeRichtungAnzeigen,
        { _, s ->
            "Spieler ${s.name}. Gegenstand: ${s.gegenstand}"
        },
        { "" },
        { "Du siehst eine Truhe" },
        { _, _ -> "Viel Erfolg!" }
    ),
}

val nullPunkt = Punkt(0, 0)

data class Spieler(
    val name: String,
    val steuerung: Steuerung,
    val raum: Punkt,
    val richtung: Richtung = Richtung.O,
    val gegenstand: Gegenstand? = null
)

data class Haus(
    val räume: Map<Punkt, Raum>,
    val ausgang: Punkt
)

data class Raum(
    val name: String,
    val punkt: Punkt,
    val gegenstände: List<Gegenstand> = emptyList(),
    val türen: Map<Richtung, Punkt> = mapOf(),
    val wache: Wache? = null,
    val truhe: Truhe? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Raum

        if (gegenstände != other.gegenstände) return false
        if (wache != other.wache) return false
        if (truhe != other.truhe) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gegenstände.hashCode()
        result = 31 * result + (wache?.hashCode() ?: 0)
        result = 31 * result + (truhe?.hashCode() ?: 0)
        return result
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
    "beigen",
    "blassen",
    "bunten",
    "dunklen",
    "engen",
    "gelben",
    "gewölbten",
    "großen",
    "grünen",
    "hellen",
    "kahlen",
    "kalten",
    "kleinen",
    "monotonen",
    "nassen",
    "pinken",
    "roten",
    "schwarzen",
    "stinkenden",
    "warmen",
    "weißen",
    "zinborrten"
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
    val anwenden: (räume: Map<Punkt, Raum>, nicht: List<Punkt>) -> Map<Punkt, Raum>?) {
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

    override fun drehen(spiel: Spiel, richtung: Richtung): Spiel {
        return kopiere(spiel, spieler = spiel.spieler.copy(richtung = richtung))
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
    val wert: Any?,
    var fertig: Boolean = false,
    var ausprobiert: Map<Any, Entscheidung>? = null
) {
    override fun toString(): String {
        return pfad.toString()
    }

    val pfadLänge: Int = vorige?.let { it.pfadLänge + 1 } ?: 0

    val pfad: List<String>
        get() = (vorige?.pfad ?: emptyList()) + listOfNotNull(
            if (wert != null && spiel != null) "${spiel.spieler.name},${spiel.spieler.raum},$wert" else null)
}

private fun beste(computerSteuerung: ComputerSteuerung): Entscheidung? {
    return computerSteuerung.lösungen.minBy { it.pfadLänge }
}

private fun allesGebraucht(räaume: Collection<Raum>): Boolean {
    return räaume.all { r -> (r.truhe?.inhalt?.size ?: 0) == 0 } &&
        räaume.all { it.wache == null }
}

private fun elemente(haus: Haus): Int = haus.räume.values.map {
    listOfNotNull(it.wache, it.truhe).size
}.sum()

tailrec fun schonMal(entscheidung: Entscheidung?, erwartet: Spiel): Boolean {
    val spiel = entscheidung?.spiel ?: return false

    if (spiel.haus.räume.values == erwartet.haus.räume.values && spiel.spieler == erwartet.spieler) {
        return true
    }

    return schonMal(entscheidung.vorige, erwartet)
}

class ComputerSteuerung(private val detail: Boolean, private val schnell: Boolean) : Steuerung {
    private var aktuelleEntscheidung: Entscheidung = Entscheidung(null, null, null)
    private var züge = 0
    val lösungen: MutableList<Entscheidung> = mutableListOf()
    var zuLeicht = false

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
                aktion(w).takeUnless { schonMal(a, it) }?.let { w as Any to Entscheidung(it, a, w) }
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
                    val pfad = beste.pfad
                    "Lösung: $beste. Länge: ${pfad.size} " +
                        "Lösungen: ${l.joinToString { it.toString() + "/" + pfad.size }}"
                }
                beste.spiel!!
            }
            ?: a.spiel!!.copy(aufgeben = true)
    }

    override fun fertig(spiel: Spiel): Spiel? {
        if (!allesGebraucht(spiel.haus.räume.values)) {
            zuLeicht = true
            return spiel.copy(aufgeben = true)
        }

        val a = aktuelleEntscheidung
        lösungen.add(a)
        if (schnell) {
            return spiel
        }
        a.fertig = true
        return baumSuche(a.vorige!!)
    }

    override fun ausgabe(nachricht: () -> String) {
        if (detail) {
            println(nachricht())
        }
    }

    override fun drehen(spiel: Spiel, richtung: Richtung): Spiel {
        return spiel
    }
}

data class Wache(val benötigt: Gegenstand, val name: String)

data class Truhe(val schlüssel: Gegenstand, val inhalt: List<Gegenstand>)

fun main() {
    val w = eingabe("Wie viele Spieler", (1..4).toList()) { i, _ ->
        AuswahlAnzeige(i, i.toString(), i.toString(), i.toString())
    }

    val räume = generiereRäume()

    val punkte = maximalEntfernt(w + 1, räume.keys)
    val (haus, lösung) = plaziereElemente(räume, punkte)

    val steuerung = MenschSteuerung()
    val spieler = neueSpieler(steuerung, punkte)

    val sch = eingabe("Wähle eine Schwierigkeit", Schwierigkeit.values().toList()) { s, i ->
        AuswahlAnzeige(s, s.name, i.toString(), i.toString())
    }

    println(sch.intro(lösung, haus))

    spiele(Spiel(null, spieler.first(), spieler, haus, schwierigkeit = sch, phase = Phase.values().first()))
}

private fun neueSpieler(steuerung: Steuerung, punkte: List<Punkt>): List<Spieler> {
    return (1 until punkte.size).map { Spieler(it.toString(), steuerung, punkte[it]) }
}

private fun maximalEntfernt(w: Int, alle: Set<Punkt>): List<Punkt> {
    var best = Pair(1, listOf<Punkt>())

    repeat(10000) {
        val list = (1..w).map { alle.random() }
        var p = entfernung(list.first(), list.last())
        list.forEachIndexed { i, p1 ->
            list.drop(i + 1).forEach {
                p *= entfernung(p1, it)
            }
        }
        if (p > best.first) {
            best = Pair(p, list)
        }
    }

    return best.second
}

private fun entfernung(p1: Punkt, p2: Punkt) = (p1.x - p2.x).absoluteValue + (p1.y - p2.y).absoluteValue

private fun generiereRäume(): Map<Punkt, Raum> {
    val limit = 3
    val zahl = 20
    var räume = mapOf(nullPunkt to Raum(raumEigenschaften.first(), nullPunkt))

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

private fun plaziereElemente(räume: Map<Punkt, Raum>, punkte: List<Punkt>): Pair<Haus, Entscheidung> {
    val probieren = 10000
    var lösbar = räume
    var aktuell = räume
    var best: Entscheidung? = null

    val ausgang = punkte.first()
    var probiert = 0
    var plaziert = 0

    while (probiert < probieren) {
        probiert++

        val st = ComputerSteuerung(detail = false, schnell = false)
        val ki = neueSpieler(st, punkte)
        spiele(Spiel(null, ki.first(), ki, Haus(aktuell, ausgang),
            schwierigkeit = Schwierigkeit.leicht,
            phase = Phase.values().first()))

        val b = beste(st)
        if (b != null) {
            best = b
            lösbar = aktuell
        }

        if (b != null || st.zuLeicht) {
            aktuell = plaziereElement(aktuell, punkte) ?: return Pair(Haus(lösbar, ausgang), best!!)
            plaziert++
        } else {
            aktuell = lösbar
        }
    }
    return Pair(Haus(lösbar, ausgang), best!!)
}

private fun plaziereElement(aktuell: Map<Punkt, Raum>, nicht: List<Punkt>): Map<Punkt, Raum>? {
    val hausElemente = HausElement.values().map { e -> List(e.relativeWahrscheinlichkeit) { e } }.flatten()

    var probiert = 0
    while (probiert < 10000) {
        probiert++
        hausElemente.random().anwenden(aktuell, nicht)?.let {
            return it
        }
    }
    return null
}

fun plaziereWache(räume: Map<Punkt, Raum>, nicht: List<Punkt>): Map<Punkt, Raum>? {
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
        .filter { it.wache == null && it.punkt !in nicht }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val lager = räume.values
        .filter { it.punkt != wachenRaum.punkt && it.punkt !in nicht }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    return kopiereRäume(räume, listOf(
        wachenRaum.copy(wache = Wache(g, wache.name)),
        lager.copy(gegenstände = lager.gegenstände + listOf(g))
    ))
}

fun plaziereTruhe(räume: Map<Punkt, Raum>, nicht: List<Punkt>): Map<Punkt, Raum>? {
    val plaziert = räume.values.mapNotNull { it.truhe?.schlüssel }

    val schlüssel = schlüssel
        .filterNot { it in plaziert }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val raum = räume.values
        .filter { it.truhe == null && it.punkt !in nicht }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val alterRaum = räume.values
        .filter { it.punkt != raum.punkt && it.gegenstände.isNotEmpty() }
        .takeIf { it.isNotEmpty() }
        ?.random()
        ?: return null

    val schlüsselRaum = räume.values
        .filter { it.punkt != raum.punkt && it.punkt != alterRaum.punkt && it.punkt !in nicht }
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
    phase: Phase? = null,
    phaseGesetzt: Boolean = false
): Spiel {
    return Spiel(
        spiel,
        spieler ?: spiel.spieler,
        if (spieler != null) spiel.alleSpieler.map { if (it.name == spieler.name) spieler else it } else spiel.alleSpieler,
        if (räume.isNotEmpty()) spiel.haus.copy(räume = kopiereRäume(spiel.haus.räume, räume)) else spiel.haus,
        aufgeben = aufgeben,
        phaseGesetzt = phaseGesetzt,
        schwierigkeit = spiel.schwierigkeit,
        phase = phase ?: spiel.phase
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

    while (!s.aufgeben && züge < 50000) {
        s = s.phase.aktion(s)

        if (fertig(s)) {
            s = s.spieler.steuerung.fertig(s) ?: return false
            if (fertig(s)) {
                break
            }
        }

        s = if (!s.phaseGesetzt) {
            kopiere(s, phase = nächste(s), phaseGesetzt = false)
        } else {
            s.copy(phaseGesetzt = false)
        }
        if (s.phase.ordinal == 0) {
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
    }, { spiel.spieler.steuerung.drehen(neuerRaum(spiel, raumHinterTür(spiel, it)), it) })
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

private fun nächste(spiel: Spiel) = Phase.values()[(spiel.phase.ordinal + 1) % Phase.values().size]

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
            kopiere(spiel,
                spieler = spieler.copy(raum = letzterRaum(spiel, spieler)),
                phase = Phase.values().first(), phaseGesetzt = true)
        }
    } else {
        spiel
    }
}

fun letzterRaum(spiel: Spiel, aktuell: Spieler): Punkt {
    return if (spiel.spieler.raum != aktuell.raum && spiel.spieler.name == aktuell.name) {
        spiel.spieler.raum
    } else {
        letzterRaum(spiel.voriges!!, aktuell)
    }
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

