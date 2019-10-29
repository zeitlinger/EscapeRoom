@file:Suppress("SpellCheckingInspection", "EnumEntryName", "FunctionName", "NonAsciiCharacters", "LocalVariableName", "ClassName", "PrivatePropertyName", "PropertyName")

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
    fun <T> auswahl(spiel: Spiel, nachricht: String, auswahl: Collection<T>, anzeige: (T, Int) -> AuswahlAnzeige<T>): Auswahl<T>
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

enum class Schwierigkeit(
    val richtungAnzeigen: (richtung: Richtung, spiel: Spiel) -> AuswahlAnzeige<Richtung>,
    val aktuellerRaum: (Raum, Spieler) -> String,
    val nächsterRaum: (Raum) -> String
) {
    leicht(::absoluteRichtungAnzeigen,
        { r, s ->
            "Spieler ${s.name}, du befindest dich in einem ${r.name} Raum bei ${r.punkt.x}/${r.punkt.y}. Gegenstand: ${s.gegenstand}"
        },
        { r -> "(ein ${r.name} Raum) ${r.wache?.let { "(${it.name} benötigt ${it.benötigt})" } ?: ""}" }
    ),
    mittel(::relativeRichtungAnzeigen,
        { r, s ->
            "Spieler ${s.name}, du befindest dich in einem ${r.name} Raum. Gegenstand: ${s.gegenstand}"
        },
        { r -> "(ein ${r.name} Raum)" }
    ),
    schwer(::relativeRichtungAnzeigen,
        { _, s ->
            "Spieler ${s.name}. Gegenstand: ${s.gegenstand}"
        },
        { "" }
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
    val ereignis: ((Spiel) -> Spiel)? = null
) {
    override fun toString(): String {
        return "$name:$punkt"
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

data class Punkt(val x: Int, val y: Int)

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
        anzeige: (T, Int) -> AuswahlAnzeige<T>
    ): Auswahl<T> {
        return Auswahl.Weiter(eingabe(nachricht, auswahl, anzeige))
    }

    override fun neuerRaum(spiel: Spiel, raum: Raum, richtung: Richtung): Spiel {
        return kopiere(spiel, spieler = spiel.spieler.copy(raum = raum.punkt, richtung = richtung))
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
        anzeige: (T, Int) -> AuswahlAnzeige<T>
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
val zuffalsComputer = Spieler(name = "Zufalls KI", steuerung = object : Steuerung {
    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        anzeige: (T, Int) -> AuswahlAnzeige<T>
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

data class Wache(val benötigt: Gegenstand, val name: String)

fun main() {
    val probieren = 100
    var räume = generiereRäume(5, 7)

    val (lösbar, best) = plaziereWachen(räume, probieren)

    println("Schaffe es in $best zügen!")

    val steuerung = MenschSteuerung()
    val mensch1 = Spieler("1", steuerung)
    val mensch2 = Spieler("2", steuerung)

    val sch = eingabe("Wähle eine Schwierigkeit", Schwierigkeit.values().toList()) { s, i ->
        AuswahlAnzeige(s, s.name, i.toString(), i.toString())
    }

    spiele(Spiel(null, mensch1, listOf(mensch1), Haus(lösbar, startPunkt, lösbar.values.last().punkt),
        schwierigkeit = sch))
}

private fun generiereRäume(limit: Int, zahl: Int): Map<Punkt, Raum> {
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
            wachenRaum.copy(wache = Wache(g, wache.name)),
            lager.copy(gegenstände = lager.gegenstände + listOf(g))
        ))

        val st = ComputerSteuerung(detail = false)
        val ki = Spieler("KI", steuerung = st)
        val spiel = Spiel(null, ki, listOf(ki), Haus(aktuell, startPunkt, ausgang), schwierigkeit = Schwierigkeit.leicht)

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
        s = sp.steuerung.neuerRaum(spiel, spiel.haus.räume.getValue(spiel.haus.eingang), Richtung.O)
    }

    while (s.spieler.raum != s.haus.ausgang && !s.aufgeben) {
        s = s.spieler.phase.aktion(s)
        if (!s.phaseGesetzt) {
            s = nächstePhase(s)
            if (s.spieler.phase.ordinal == 0) {
                val alle = s.alleSpieler
                val spieler = alle[(alle.indexOf(s.spieler) + 1) % alle.size]

                s = s.copy(spieler = spieler)
            }
        }
        züge++
    }
    spiel.spieler.steuerung.ausgabe {
        if (s.aufgeben) "Aufgegeben! $züge Züge gebraucht."
        else "Endlich wieder frei! $züge Züge gebraucht."
    }
    return !s.aufgeben
}

private fun raum(spiel: Spiel) = spiel.haus.räume.values.first { it.punkt == spiel.spieler.raum }

private fun türAuswählen(spiel: Spiel): Spiel {
    val spieler = spiel.spieler

    val türWahl = spieler.steuerung.auswahl(spiel,
        "Durch welche Tür möchtest du gehen?", raum(spiel).türen.keys) { richtung, _ ->
        spiel.schwierigkeit.richtungAnzeigen(richtung, spiel)
    }

    val tür = when (türWahl) {
        is Auswahl.Weiter -> türWahl.wahl
        is Auswahl.ZurückAusRaum -> return türWahl.spiel
    }

    return neuerRaum(tür, spiel)
}

private fun absoluteRichtungAnzeigen(richtung: Richtung, spiel: Spiel): AuswahlAnzeige<Richtung> =
    AuswahlAnzeige(richtung, richtung.name + raumVorschau(spiel, richtung), richtung.taste, richtung.reihenfolge)

private fun relativeRichtungAnzeigen(richtung: Richtung, spiel: Spiel): AuswahlAnzeige<Richtung> =
    relativeRichtung(spiel.spieler.richtung, richtung).let {
        AuswahlAnzeige(richtung, it.name + raumVorschau(spiel, richtung), it.taste, it.reihenfolge)
    }

private fun raumVorschau(spiel: Spiel, richtung: Richtung) =
    spiel.schwierigkeit.nächsterRaum(raumHinterTür(spiel, richtung))

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

    if (gegenstände.isNotEmpty()) {
        val auswahl = spieler.steuerung.auswahl(
            spiel,
            "Was willst du mitnehmen?",
            gegenstände + listOf(Gegenstand.Keinen)
        ) { g, i -> AuswahlAnzeige(g, g.name, i.toString(), i.toString()) }

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
    return spiel.spieler.steuerung.neuerRaum(spiel, raumHinterTür(spiel, tür), tür)
}

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
//2) ein/eine
//3) Truhe
//4) Tauchen
//5) Keller
//6) bessere Eingabe (kein Enter) also nur w
//7) Enter = nix nehme
//8) Freitexteingabe bei Forscher, Hexe

