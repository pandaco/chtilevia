#!/bin/sh
# Tests de non-régression : chaque cas lance chti sur les fixtures figées, à une heure figée,
# et compare la sortie (stdout + stderr + code retour) à tests/expected/<cas>.txt.
#
# Usage : tests/run.sh            lance les tests
#         tests/run.sh --update   régénère les sorties attendues (à relire avant de commiter !)
set -eu

ROOT=$(cd "$(dirname "$0")/.." && pwd)
FIX="$ROOT/tests/fixtures"
EXPECTED="$ROOT/tests/expected"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/chti-tests.XXXXXX")
trap 'rm -rf "$WORK"' EXIT INT TERM

update=0
[ "${1:-}" = --update ] && update=1

# Heures figées (epoch) : horodatage des instantanés du flux temps réel
MIDI=1791629160   # 2026-10-10 12:46:00
SOIR=1791669000   # 2026-10-10 23:50:00
MINUIT=1791669618 # 2026-10-11 00:00:18

export XDG_CACHE_HOME="$WORK/cache"
export CHTI_GTFS_URL="file://$FIX/gtfs.zip"
export CHTI_GBFS_URL="file://$FIX"
export XDG_CONFIG_HOME="$FIX/config"

# Index GTFS construit une fois pour tous les cas
"$ROOT/chti" update >/dev/null 2>&1

# nom | heure | flux temps réel (fichier de fixtures, ou « aucun ») | arguments
CASES='
bus-liste-arrets        | MIDI   | rt-midi.pb   | bus 10
bus-liste-direction     | MIDI   | rt-midi.pb   | bus 10 -d hegel
bus-temps-reel          | MIDI   | rt-midi.pb   | bus 10 marais
bus-direction-nombre    | MIDI   | rt-midi.pb   | bus 10 marais -d flandres -n 5
bus-options-avant-arret | MIDI   | rt-midi.pb   | bus 10 --nombre=2 --vers=chu marais
tram-temps-reel         | MIDI   | rt-midi.pb   | tram croise laroche -d roubaix
metro-theorique         | MIDI   | rt-midi.pb   | metro 1 republique -n 2
bus-minuit-lendemain    | MINUIT | rt-minuit.pb | bus 10 marais -d flandres -n 4
bus-sans-temps-reel     | SOIR   | aucun        | bus 10 gambetta -n 3
metro-fin-de-service    | SOIR   | aucun        | metro 1 republique --vers chu -n 4
erreur-direction        | MIDI   | rt-midi.pb   | bus 10 marais -d zzz
erreur-ligne            | MIDI   | rt-midi.pb   | bus 999
erreur-arret            | MIDI   | rt-midi.pb   | bus 10 zzz
erreur-nombre-zero      | MIDI   | rt-midi.pb   | bus 10 marais -n 0
erreur-nombre-texte     | MIDI   | rt-midi.pb   | tram -n abc
erreur-metro            | MIDI   | rt-midi.pb   | metro 3
velo-station            | MINUIT | aucun        | velo rihour
velo-plusieurs          | MINUIT | aucun        | velo gare
velo-accents            | MINUIT | aucun        | velo SAINTE HELENE
velo-hors-service       | MINUIT | aucun        | velo port de lille
erreur-velo             | MINUIT | aucun        | velo zzz
velo-prendre            | MINUIT | aucun        | velo --prendre rihour
velo-deposer            | MINUIT | aucun        | velo sainte helene --déposer
velo-prendre-hs         | MINUIT | aucun        | velo port de lille --prendre
bus-vers-sans-tiret     | MIDI   | rt-midi.pb   | bus 10 marais vers lille flandres -n 1
favoris-liste           | MIDI   | aucun        | favoris
favori-tram             | MIDI   | rt-midi.pb   | travail
favori-velo-casse       | MINUIT | aucun        | RENTRER-VELO
favori-en-erreur        | MIDI   | rt-midi.pb   | casse
erreur-favori-inconnu   | MIDI   | aucun        | inconnu
voix-bus                | MIDI   | rt-midi.pb   | --voix bus 10 marais
voix-option-a-la-fin    | MIDI   | rt-midi.pb   | bus 10 marais vers flandres --voix -n 3
voix-metro-theorique    | MIDI   | rt-midi.pb   | --voix metro 1 republique
voix-minuit-prevu       | MINUIT | rt-minuit.pb | --voix bus 10 marais -d flandres -n 3
voix-velo               | MINUIT | aucun        | --voix velo rihour
voix-velo-prendre       | MINUIT | aucun        | --voix velo gare --prendre
voix-velo-hors-service  | MINUIT | aucun        | --voix velo port de lille
voix-favori             | MINUIT | aucun        | --voix rentrer-velo
voix-sans-arret         | MIDI   | aucun        | --voix bus 10
voix-erreur-ligne       | MIDI   | aucun        | --voix bus 999
voix-erreur-inconnu     | MIDI   | aucun        | --voix inconnu
'

pass=0
fail=0
set -f # pas d'expansion de jokers dans les arguments
mkdir -p "$EXPECTED"
while IFS='|' read -r name clock rt args; do
	name=$(echo "$name" | tr -d ' ')
	[ -n "$name" ] || continue
	clock=$(echo "$clock" | tr -d ' ')
	rt=$(echo "$rt" | tr -d ' ')
	case "$clock" in
	MIDI) now=$MIDI ;;
	SOIR) now=$SOIR ;;
	MINUIT) now=$MINUIT ;;
	esac
	if [ "$rt" = aucun ]; then rt_url="file://$FIX/absent.pb"; else rt_url="file://$FIX/$rt"; fi

	# shellcheck disable=SC2086 # découpage volontaire des arguments
	set -- $args
	out="$WORK/$name.txt"
	code=0
	CHTI_NOW=$now CHTI_RT_URL=$rt_url "$ROOT/chti" "$@" >"$out" 2>&1 || code=$?
	# Le message d'erreur de curl varie selon les versions : on ne garde que celui de chti.
	# Les chemins absolus dépendent de la machine : on les remplace par <racine>.
	grep -v '^curl: ' "$out" | sed "s|$ROOT|<racine>|g" >"$out.tmp" || true
	printf '[exit %s]\n' "$code" >>"$out.tmp"
	mv "$out.tmp" "$out"

	if [ "$update" = 1 ]; then
		cp "$out" "$EXPECTED/$name.txt"
		echo "maj   $name"
	elif diff -u "$EXPECTED/$name.txt" "$out" >"$WORK/$name.diff" 2>&1; then
		pass=$((pass + 1))
		echo "ok    $name"
	else
		fail=$((fail + 1))
		echo "ÉCHEC $name  (chti $args)"
		sed 's/^/      /' "$WORK/$name.diff"
	fi
done <<EOF
$CASES
EOF

[ "$update" = 1 ] && exit 0
echo
echo "$pass réussi(s), $fail échec(s)"
[ "$fail" -eq 0 ]
