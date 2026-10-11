#!/bin/sh
# Teste la partie Termux (apps/shell/termux/) sans téléphone : les commandes Termux (pkg, termux-*) sont
# remplacées par des imitations qui notent ce qu'on leur demande, et chti tourne sur les fixtures.
#
# Usage : apps/shell/tests/termux.sh
set -eu

ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
FIX="$ROOT/libs/contract/fixtures"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/chti-android.XXXXXX")
trap 'rm -rf "$WORK"' EXIT INT TERM

# Faux Termux : $PREFIX/bin dans le PATH, $HOME isolé
export PREFIX="$WORK/prefix" HOME="$WORK/home"
LOG="$WORK/log"
export LOG
mkdir -p "$PREFIX/bin" "$HOME"
export PATH="$PREFIX/bin:$PATH"

fake() {
	printf '#!/bin/sh\n%s\n' "$2" >"$PREFIX/bin/$1"
	chmod +x "$PREFIX/bin/$1"
}
# shellcheck disable=SC2016 # code des imitations : développé à leur exécution, pas ici
{
	fake pkg 'echo "pkg $*" >>"$LOG"'
	fake termux-speech-to-text 'printf "%s\n" "${FAKE_PHRASE:-}"'
	fake termux-notification 'echo "notification: $*" >>"$LOG"'
	fake termux-tts-speak 'echo "voix ($*): $(tr "\n" " ")" >>"$LOG"'
	fake termux-toast 'echo "écran: $*" | tr "\n" " " >>"$LOG"; echo >>"$LOG"'
}
ln -s /bin/sh "$PREFIX/bin/sh" # Termux fournit son sh dans $PREFIX/bin

# Données figées, comme tests/contract.sh (12:46 le 10/10/2026)
export XDG_CACHE_HOME="$WORK/cache" XDG_CONFIG_HOME="$WORK/config"
export CHTI_GTFS_URL="file://$FIX/gtfs.zip" CHTI_GBFS_URL="file://$FIX"
export CHTI_RT_URL="file://$FIX/rt-midi.pb" CHTI_NOW=1791629160

pass=0
fail=0
check_absent() { # check_absent <description> <texte qui ne doit pas être dans le journal>
	if grep -qF -- "$2" "$LOG"; then
		fail=$((fail + 1))
		echo "ÉCHEC $1 : « $2 » présent dans le journal :"
		sed 's/^/      /' "$LOG"
	else
		pass=$((pass + 1))
		echo "ok    $1"
	fi
}
check() { # check <description> <texte attendu dans le journal>
	if grep -qF -- "$2" "$LOG"; then
		pass=$((pass + 1))
		echo "ok    $1"
	else
		fail=$((fail + 1))
		echo "ÉCHEC $1 : « $2 » absent du journal :"
		sed 's/^/      /' "$LOG"
	fi
}

# 1. Installation
: >"$LOG"
sh "$ROOT/apps/shell/termux/install.sh" >"$WORK/install.out" 2>&1 || {
	cat "$WORK/install.out"
	exit 1
}
check "install : dépendances" "pkg install -y python curl jq unzip findutils gawk termux-api"
for f in chti chti-widget; do
	[ -x "$PREFIX/bin/$f" ] && head -1 "$PREFIX/bin/$f" >>"$LOG"
done
check "install : lanceur chti via le sh de Termux" "#!$PREFIX/bin/sh"
ls "$HOME/.shortcuts/tasks" >>"$LOG"
check "install : raccourci Parler" "Parler"
check "install : raccourci Travail" "Travail"
check "install : raccourci Rentrer à vélo" "Rentrer à vélo"
cp "$FIX/config/chtilevia/favoris" "$XDG_CONFIG_HOME/chtilevia/favoris" # favoris des fixtures

shortcut() { sh "$HOME/.shortcuts/tasks/$1"; }

# 2. Widget « Parler »
: >"$LOG"
FAKE_PHRASE="prochain bus 10 à marais vers lille flandres" shortcut Parler
check "Parler : voix en français, réponse complète" "voix (-l fr): Bus 10, arrêt Eglise Du Marais, vers Lille Gare Lille Flandres : dans 5 minutes, puis dans 28 minutes. Bus 10, arrêt Marais, vers Lille Gare Lille Flandres : dans 7 minutes, puis dans 30 minutes."
check "Parler : notification" "--content Bus 10, arrêt Eglise Du Marais"
check_absent "Parler : pas de message écrit à l'écran" "écran:"

: >"$LOG"
FAKE_PHRASE="" shortcut Parler
check "Parler : rien entendu" "voix (-l fr): Je n'ai rien entendu."

: >"$LOG"
FAKE_PHRASE="bonjour" shortcut Parler
check "Parler : phrase incomprise" "voix (-l fr): Je n'ai pas compris « bonjour »."

# 3. Raccourcis favoris : réponse écrite (notification + écran), jamais de voix
: >"$LOG"
shortcut Travail
check "Travail : notification titrée" "notification: --id chti --title chti · travail --content Tram, arrêt Croise Laroche, vers Roubaix Eurotéléport : dans 1 minute, puis dans 13 minutes."
check "Travail : message à l'écran" "écran: -g top Tram, arrêt Croise Laroche, vers Roubaix Eurotéléport : dans 1 minute, puis dans 13 minutes."
check_absent "Travail : pas de voix" "voix ("

: >"$LOG"
CHTI_NOW=1791669618 shortcut "Rentrer à vélo"
check "Rentrer à vélo : vélos au départ" "Gare Lille Europe : 14 vélos disponibles."
check "Rentrer à vélo : places à l'arrivée" "La Madeleine Gare : 13 places libres."
check_absent "Rentrer à vélo : pas de voix" "voix ("

# 4. Un favori demandé à la voix répond à la voix
: >"$LOG"
FAKE_PHRASE="travail" shortcut Parler
check "Parler « travail » : voix" "voix (-l fr): Tram, arrêt Croise Laroche"
check_absent "Parler « travail » : pas de message écrit" "écran:"

# 5. chti-widget sans mode : erreur d'usage
if chti-widget travail 2>/dev/null; then
	fail=$((fail + 1))
	echo "ÉCHEC chti-widget sans mode devrait échouer"
else
	pass=$((pass + 1))
	echo "ok    chti-widget sans mode : erreur d'usage"
fi

echo
echo "$pass réussi(s), $fail échec(s)"
[ "$fail" -eq 0 ]
