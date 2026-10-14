#!/bin/sh
# Reprise des dossiers d'avant le renommage : chti trouve ~/.config/chtilevia et ~/.cache/chtilevia,
# les renomme en chtipasse (rien n'est copié), et ne touche pas à l'ancien si le nouveau existe déjà.
#
# Usage : apps/shell/tests/app-dir.sh
set -eu

ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
CHTI="$ROOT/apps/shell/chti"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/chti-app-dir.XXXXXX")
trap 'rm -rf "$WORK"' EXIT INT TERM
export XDG_CONFIG_HOME="$WORK/config" XDG_CACHE_HOME="$WORK/cache"

fail=0
# result <libellé> <0 si réussi> : affiche et note le résultat d'une vérification
result() {
	if [ "$2" -eq 0 ]; then
		printf 'ok    %s\n' "$1"
	else
		printf 'ÉCHEC %s\n' "$1"
		fail=1
	fi
}

# Ancien dossier seul : repris
mkdir -p "$WORK/config/chtilevia" "$WORK/cache/chtilevia"
printf 'travail: tram croise laroche\n' >"$WORK/config/chtilevia/favoris"
listing=$("$CHTI" favoris 2>&1)
case "$listing" in *"travail: tram croise laroche"*) ok=0 ;; *) ok=1 ;; esac
result "favoris repris de chtilevia" "$ok"
if [ -f "$WORK/config/chtipasse/favoris" ] && [ ! -e "$WORK/config/chtilevia" ]; then ok=0; else ok=1; fi
result "ancien dossier de config renommé" "$ok"
if [ -d "$WORK/cache/chtipasse" ] && [ ! -e "$WORK/cache/chtilevia" ]; then ok=0; else ok=1; fi
result "ancien cache renommé" "$ok"

# Les deux dossiers : le nouveau gagne, l'ancien reste intact
mkdir -p "$WORK/config/chtilevia"
printf 'ancien: velo rihour\n' >"$WORK/config/chtilevia/favoris"
listing=$("$CHTI" favoris 2>&1)
case "$listing" in *"ancien:"*) ok=1 ;; *"travail:"*) ok=0 ;; *) ok=1 ;; esac
result "le nouveau dossier gagne" "$ok"
if [ -f "$WORK/config/chtilevia/favoris" ]; then ok=0; else ok=1; fi
result "l'ancien dossier reste intact" "$ok"

exit "$fail"
