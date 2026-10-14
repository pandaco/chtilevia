package fr.chtipasse.domain

import java.text.Normalizer

internal actual fun decomposeCompatibility(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKD)
