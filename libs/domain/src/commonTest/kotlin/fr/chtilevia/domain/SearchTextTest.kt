package fr.chtilevia.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTextTest {
    // Attendus produits par le fold Python de chti : même résultat sur toutes les plateformes
    private val expected = listOf(
        "Croisé Laroche" to "croise laroche",
        "  RÉPUBLIQUE   Beaux-Arts " to "republique beaux arts",
        "Gare Lille-Flandres" to "gare lille flandres",
        "Canon D'Or" to "canon d or",
        "Château Bigo Danel" to "chateau bigo danel",
        "Roubaix Eurotéléport" to "roubaix euroteleport",
        "Sainte-Hélène" to "sainte helene",
        "ÎLE ÇA Ù" to "ile ca u",
        "naïve façade" to "naive facade",
        "Chu - Centre O. Lambret" to "chu centre o lambret",
        "4 Cantons Stade P. Mauroy" to "4 cantons stade p mauroy",
        "V'Lille" to "v lille",
        "CO3" to "co3",
        "Ñandú" to "nandu",
        "Ångström" to "angstrom",
        "" to "",
        "---" to "",
        // Compatibilité : ligatures, pleine chasse, exposants
        "ﬁlle" to "fille",
        "Ｗide" to "wide",
        "²3" to "23",
        // Lettres sans décomposition : retirées, comme dans chti (à améliorer des deux côtés)
        "Cœur de Lille" to "cur de lille",
        "Æsop" to "sop",
        "Straße" to "strae",
        "Øresund" to "resund",
    )

    @Test
    fun foldMatchesShellImplementation() {
        for ((input, folded) in expected) {
            assertEquals(folded, fold(input), "fold(\"$input\")")
        }
    }
}
