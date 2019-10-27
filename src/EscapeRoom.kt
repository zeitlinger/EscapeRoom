@file:Suppress("SpellCheckingInspection", "EnumEntryName", "FunctionName", "NonAsciiCharacters", "LocalVariableName", "ClassName", "PrivatePropertyName", "PropertyName")

data class Spiel(
    val spieler: Spieler,
    val haus: Haus,
    val aufgabe: Boolean = false
)

interface Steuerung {
    fun <T> auswahl(spiel: Spiel, nachricht: String, auswahl: Collection<T>, name: (T) -> String): Auswahl<T>
    fun neuerRaum(spiel: Spiel, raum: Raum, richtung: Richtung): Spiel
}

enum class Phase(val aktion: (Spiel) -> Spiel) {
    gegenstandsAuswahl(::gegenstandAuswahl),
    türAuswählen(::türAuswählen)
}

data class Spieler(
    val steuerung: Steuerung,
    val raum: Int = 0,
    val phase: Phase,
    val richtung: Richtung = Richtung.O,
    val gegenstand: Gegenstand? = null
)

data class Haus(
    val räume: List<Raum>,
    val eingang: Int,
    val ausgang: Int
)

data class Raum(
    val gegenstände: List<Gegenstand> = listOf(),
    val türen: MutableMap<Richtung, Int> = mutableMapOf(),
    val wache: ((Spieler) -> Boolean)? = null,
    var nummer: Int = 0
) {
    override fun toString(): String {
        return nummer.toString()
    }
}

@Suppress("unused")
enum class Gegenstand {
    Waffe,
    Goldsack,
    Keinen
}

enum class Richtung {
    N,
    O,
    S,
    W
}

@Suppress("unused")
enum class RelativeRichtung(val wert: Int) {
    vorne(0),
    rechts(1),
    hinten(2),
    links(-1)
}

fun relativeRichtung(spieler: Richtung, tür: Richtung): RelativeRichtung =
    RelativeRichtung.values().first { (spieler.ordinal + it.wert) % 4 == tür.ordinal }

fun horizontal(räume: List<Raum>, westen: Int, osten: Int) {
    räume[westen].türen[Richtung.O] = osten
    räume[osten].türen[Richtung.W] = westen
}

fun vertikal(räume: List<Raum>, norden: Int, süden: Int) {
    räume[norden].türen[Richtung.S] = süden
    räume[süden].türen[Richtung.N] = norden
}

@Suppress("unused")
val mensch = Spieler(phase = Phase.gegenstandsAuswahl, steuerung = object : Steuerung {
    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        name: (T) -> String
    ): Auswahl<T> {
        val namen = auswahl.map { name(it) to it }.toMap()
        val kurz = namen.keys.map { it.substring(0, 1).toLowerCase() to it }.toMap()

        println("$nachricht ${namen.keys.joinToString(", ")} (${kurz.keys.joinToString("/")})")

        while (true) {
            val eingabe = readLine()
            if (eingabe in kurz) {
                return Auswahl.Weiter(namen.getValue(kurz.getValue(eingabe!!)))
            }
            println("Falsche Eingabe. Versuche es noch einmal.")
        }
    }

    override fun neuerRaum(spiel: Spiel, raum: Raum, richtung: Richtung): Spiel {
        return kopieren(spiel, spieler = spiel.spieler.copy(
            raum = raum.nummer,
            richtung = richtung,
            phase = Phase.gegenstandsAuswahl))
    }
})

data class Entscheidung(
    var spiel: Spiel?,
    val pfad: List<Any>,
    var fertig: Boolean = false,
    var ausprobiert: Map<Any, Entscheidung>? = null,
    val vorige: Entscheidung?


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
        println("zurück zu $entscheidung / ${spiel.spieler.raum} / ${spiel.spieler.gegenstand}")
        computerSteuerung.aktuelleEntscheidung = entscheidung
        return spiel
    } else {
        entscheidung.fertig = true
    }

    return entscheidung.vorige?.let { zurück(computerSteuerung, it) }
        ?: computerSteuerung.lösungen.takeIf { it.isNotEmpty() }?.let { l ->
            val beste = l.minBy { it.pfad.size }!!
            println("Lösung: $beste. Länge: ${beste.pfad.size} Lösungen: ${l.joinToString { it.toString() + "/" + it.pfad.size }}")
            beste.spiel!!
        }
        ?: entscheidung.spiel!!.copy(aufgabe = true)
}

fun schonMal(entscheidung: Entscheidung?, spiel: Spiel): Boolean {
    if (entscheidung == null) {
        return false
    }

    return entscheidung.spiel == spiel || schonMal(entscheidung.vorige, spiel)
}

class ComputerSteuerung : Steuerung {
    var aktuelleEntscheidung: Entscheidung? = null
    private var züge = 0
    val lösungen: MutableList<Entscheidung> = mutableListOf()

    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        name: (T) -> String
    ): Auswahl<T> {
        if (züge >= 10000) {
            return Auswahl.ZurückAusRaum(spiel.copy(aufgabe = true))
        }
        züge++

        val a = aktuelleEntscheidung!!
        if (schonMal(a.vorige, spiel)) {
            a.fertig = true
            return Auswahl.ZurückAusRaum(zurück(this, a))
        }

        if (a.ausprobiert == null) {
            a.spiel = spiel
            a.ausprobiert = auswahl.map { it as Any to Entscheidung(null, a.pfad + listOf(it as Any), vorige = a) }.toMap()
        }

        return a.ausprobiert?.entries
            ?.firstOrNull { !it.value.fertig }
            ?.let {
                @Suppress("UNCHECKED_CAST") val wahl = it.key as T
                aktuelleEntscheidung = it.value
                println("computer hat $wahl genommen. $aktuelleEntscheidung. Raum:${spiel.spieler.raum}")
                Auswahl.Weiter(wahl)
            }
            ?: Auswahl.ZurückAusRaum(zurück(this, a))
    }

    override fun neuerRaum(spiel: Spiel, raum: Raum, richtung: Richtung): Spiel {
        if (aktuelleEntscheidung == null) {
            aktuelleEntscheidung = Entscheidung(spiel, emptyList(), vorige = null)
        }

        val s = kopieren(spiel, spieler = spiel.spieler.copy(
            raum = raum.nummer,
            phase = Phase.gegenstandsAuswahl))

        if (raum.nummer == spiel.haus.ausgang) {
            val a = aktuelleEntscheidung!!
            a.spiel = s
            lösungen.add(a)
            return zurück(this, a)
        }
        return s
    }
}

val probierComputer = Spieler(steuerung = ComputerSteuerung(), phase = Phase.gegenstandsAuswahl)

@Suppress("unused")
val zuffalsComputer = Spieler(phase = Phase.gegenstandsAuswahl, steuerung = object : Steuerung {
    override fun <T> auswahl(
        spiel: Spiel,
        nachricht: String,
        auswahl: Collection<T>,
        name: (T) -> String
    ): Auswahl<T> {
        val wahl = auswahl.random()
        println("computer hat $wahl genommen")
        return Auswahl.Weiter(wahl)
    }

    override fun neuerRaum(spiel: Spiel, raum: Raum, richtung: Richtung): Spiel {
        return kopieren(spiel, spieler = spiel.spieler.copy(
            raum = raum.nummer,
            phase = Phase.gegenstandsAuswahl))
    }
})

private fun wache(benötigt: Gegenstand, name: String): (Spieler) -> Boolean = { spieler: Spieler ->
    if (spieler.gegenstand == benötigt) {
        println("Du besiegst den $name!")
        true
    } else {
        println("Du siehst einen $name und gehst rückwärts schnell wieder zurück!")
        false
    }
}

fun main() {
    val start = Raum()
    val r1 = Raum(gegenstände = mutableListOf(Gegenstand.Waffe))
    val r2 = Raum(wache = wache(Gegenstand.Waffe, "Troll"))
    val r3 = Raum(wache = wache(Gegenstand.Goldsack, "Zwerg"))
    val r4 = Raum()
    val r5 = Raum(gegenstände = mutableListOf(Gegenstand.Goldsack))
    val ziel = Raum()

    val räume = listOf(start, r1, r2, r3, r4, r5, ziel)
    räume.forEachIndexed { index, raum -> raum.nummer = index }

    horizontal(räume, 0, 1)
    horizontal(räume, 1, 5)
    vertikal(räume, 0, 2)
    horizontal(räume, 2, 3)
    horizontal(räume, 3, 4)
    vertikal(räume, 3, 6)

    val spieler = probierComputer
    spiele(
        Spiel(
            spieler,
            Haus(räume, 0, 6)
        )
    )
}

fun kopieren(
    spiel: Spiel,
    spieler: Spieler = spiel.spieler,
    räume: List<Raum> = listOf(),
    aufgabe: Boolean = false
): Spiel {
    return Spiel(
        spieler,
        if (räume.isNotEmpty()) {
            spiel.haus.copy(räume = spiel.haus.räume.map { r ->
                räume.firstOrNull { it.nummer == r.nummer }
                    ?: r.copy(gegenstände = r.gegenstände.toList())
            })
        } else spiel.haus,
        aufgabe = aufgabe
    )
}

fun spiele(spiel: Spiel): Int {
    var züge = 0
    var s = spiel.spieler.steuerung.neuerRaum(spiel, spiel.haus.räume[spiel.haus.eingang], Richtung.O)
    while (s.spieler.raum != s.haus.ausgang && !s.aufgabe) {
        s = s.spieler.phase.aktion(s)
        züge++
    }
    if (s.aufgabe) {
        println("Aufgegeben! $züge Züge gebraucht.")
    } else {
        println("Endlich wieder frei! $züge Züge gebraucht.")
    }
    return züge
}

private fun raum(spiel: Spiel) = spiel.haus.räume.first { it.nummer == spiel.spieler.raum }

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
    println("Raum: $raum Gegenstand: ${spieler.gegenstand}")

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
    return kopieren(spiel, spieler.copy(phase = Phase.türAuswählen))
}

private fun neuerRaum(tür: Richtung, spiel: Spiel): Spiel {
    val nächster = spiel.haus.räume[raum(spiel).türen.getValue(tür)]
    val spieler = spiel.spieler

    val wache = nächster.wache
    if (wache != null) {
        return if (wache(spieler)) {
            spieler.steuerung.neuerRaum(kopieren(spiel, räume = listOf(nächster.copy(wache = null))), nächster, tür)
        } else {
            spiel
        }
    }

    return spieler.steuerung.neuerRaum(spiel, nächster, tür)
}

fun nimm(spieler: Spieler, gegenstand: Gegenstand, raum: Raum, spiel: Spiel): Spiel {
    return kopieren(spiel,
        spieler = spieler.copy(gegenstand = gegenstand, phase = Phase.türAuswählen),
        räume = listOf(raum.copy(gegenstände = raum.gegenstände + listOfNotNull(spieler.gegenstand) - listOf(gegenstand)))
    )
}






