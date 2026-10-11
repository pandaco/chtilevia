#!/bin/sh
# chti-widget — lance chti pour un bouton du widget Termux et rend la réponse :
#   --parle : notification + lecture à voix haute (question posée à la voix)
#   --ecrit : notification + message à l'écran, sans voix (raccourci touché)
# Installé dans Termux par apps/shell/termux/install.sh.
#   ex : chti-widget --parle vocal "métro 1 république vers chu"    chti-widget --ecrit travail
set -u

case "${1:-}" in
--parle | --ecrit) mode=$1 ;;
*)
	echo "usage : chti-widget --parle|--ecrit <arguments chti>" >&2
	exit 1
	;;
esac
shift

err=$(mktemp "${TMPDIR:-/tmp}/chti-widget.XXXXXX")
trap 'rm -f "$err"' EXIT INT TERM

# Réponse en phrases courtes dans les deux modes : lisibles à l'écran comme à l'oral
if message=$(chti --voix "$@" 2>"$err"); then
	:
else
	# En cas d'échec, on rend le message d'erreur (sans la ligne de mise à jour du cache)
	message=$(grep -v '^Mise à jour du référentiel' "$err")
fi
[ -n "$message" ] || message="Désolé, je n'ai pas de réponse."

termux-notification --id chti --title "chti · $*" --content "$message"
if [ "$mode" = --parle ]; then
	printf '%s\n' "$message" | termux-tts-speak -l fr
else
	termux-toast -g top "$message"
fi
