// Préchargé (node --require) par les tests Kotlin/JS, voir buildSrc/…/chtilevia.kmp-library.gradle.kts.
// Retire le seul avertissement « alsoWithHtml » de kotlin-web-helpers (sans effet, corrigé en 3.5.4) ;
// tout autre message passe tel quel.
'use strict';
const warn = console.warn;
console.warn = (...args) => {
    if (typeof args[0] === 'string' && args[0].startsWith("Reporter option 'alsoWithHtml' has no effect.")) return;
    warn(...args);
};
