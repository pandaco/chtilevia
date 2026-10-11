#!/bin/sh
# Tests de non-régression : chaque cas de libs/contract/cases.txt lance chti sur les fixtures
# figées, à une heure figée (libs/contract/clocks.txt), et compare la sortie (stdout + stderr +
# code retour) à libs/contract/expected/<cas>.txt.
#
# Usage : apps/shell/tests/contract.sh            lance les tests
#         apps/shell/tests/contract.sh --update   régénère les sorties attendues (à relire avant de commiter !)
set -eu

# ROOT : racine du dépôt (remplacée par <racine> dans les sorties) ; données dans libs/contract
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
CHTI="$ROOT/apps/shell/chti"
CONTRACT="$ROOT/libs/contract"
FIX="$CONTRACT/fixtures"
EXPECTED="$CONTRACT/expected"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/chti-tests.XXXXXX")
trap 'rm -rf "$WORK"' EXIT INT TERM

update=0
[ "${1:-}" = --update ] && update=1

export XDG_CACHE_HOME="$WORK/cache"
export CHTI_GTFS_URL="file://$FIX/gtfs.zip"
export CHTI_GBFS_URL="file://$FIX"
export XDG_CONFIG_HOME="$FIX/config"

# Epoch d'une heure figée de clocks.txt (MIDI, SOIR…)
clock_epoch() {
	epoch=$(awk -F'|' -v name="$1" '$1 !~ /^[[:space:]]*#/ {
		k = $1; gsub(/[[:space:]]/, "", k)
		if (k == name) { v = $2; gsub(/[[:space:]]/, "", v); print v }
	}' "$CONTRACT/clocks.txt")
	[ -n "$epoch" ] || {
		echo "heure inconnue dans clocks.txt : $1" >&2
		exit 2
	}
	printf '%s\n' "$epoch"
}

# Index GTFS construit une fois pour tous les cas
"$CHTI" update >/dev/null 2>&1

pass=0
fail=0
set -f # pas d'expansion de jokers dans les arguments
mkdir -p "$EXPECTED"
while IFS='|' read -r name clock rt args; do
	name=$(echo "$name" | tr -d ' ')
	[ -n "$name" ] || continue
	clock=$(echo "$clock" | tr -d ' ')
	rt=$(echo "$rt" | tr -d ' ')
	now=$(clock_epoch "$clock")
	if [ "$rt" = aucun ]; then rt_url="file://$FIX/absent.pb"; else rt_url="file://$FIX/$rt"; fi

	# shellcheck disable=SC2086 # découpage volontaire des arguments
	set -- $args
	out="$WORK/$name.txt"
	code=0
	CHTI_NOW=$now CHTI_RT_URL=$rt_url "$CHTI" "$@" >"$out" 2>&1 || code=$?
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
$(sed -e '/^[[:space:]]*#/d' "$CONTRACT/cases.txt")
EOF

[ "$update" = 1 ] && exit 0
echo
echo "$pass réussi(s), $fail échec(s)"
[ "$fail" -eq 0 ]
