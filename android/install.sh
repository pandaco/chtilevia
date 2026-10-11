#!/bin/sh
# Installe chtilevia dans Termux (Android) : dépendances, commandes chti et chti-widget,
# favoris d'exemple, raccourcis du widget Termux:Widget, puis construction du cache.
# Usage, depuis le dépôt cloné dans Termux : sh android/install.sh
set -eu

if [ -z "${PREFIX:-}" ] || [ ! -d "$PREFIX/bin" ]; then
	echo "À lancer dans Termux (variable PREFIX absente)." >&2
	exit 1
fi
ROOT=$(cd "$(dirname "$0")/.." && pwd)
SHELL_PATH="$PREFIX/bin/sh"

echo "1/5 Dépendances"
pkg install -y python curl jq unzip findutils gawk termux-api

echo "2/5 Commandes chti et chti-widget"
# /bin/sh n'existe pas sous Termux : les scripts sont lancés via le sh de Termux
printf '#!%s\nexec sh "%s/chti" "$@"\n' "$SHELL_PATH" "$ROOT" >"$PREFIX/bin/chti"
sed "1s|.*|#!$SHELL_PATH|" "$ROOT/android/widget.sh" >"$PREFIX/bin/chti-widget"
chmod +x "$PREFIX/bin/chti" "$PREFIX/bin/chti-widget"

echo "3/5 Favoris"
config="${XDG_CONFIG_HOME:-$HOME/.config}/chtilevia"
mkdir -p "$config"
if [ -f "$config/favoris" ]; then
	echo "    $config/favoris existe déjà, conservé"
else
	cp "$ROOT/exemples/favoris" "$config/favoris"
	echo "    exemple copié dans $config/favoris, à adapter"
fi

echo "4/5 Raccourcis du widget"
# tasks/ : lancés en arrière-plan, sans ouvrir de terminal
mkdir -p "$HOME/.shortcuts/tasks"
for shortcut in "$ROOT"/android/shortcuts/*; do
	target="$HOME/.shortcuts/tasks/$(basename "$shortcut")"
	sed "1s|.*|#!$SHELL_PATH|" "$shortcut" >"$target"
	chmod +x "$target"
	echo "    $(basename "$shortcut")"
done

echo "5/5 Cache des horaires (une fois, de préférence en Wi-Fi)"
"$PREFIX/bin/chti" update

cat <<'EOF'

Installation terminée. Reste à faire sur le téléphone :
  - ajouter le widget « Termux:Widget » sur l'écran d'accueil ;
  - toucher « Parler » une première fois et autoriser le micro pour Termux:API ;
  - adapter les favoris : nano ~/.config/chtilevia/favoris
Guide complet : ~/chtilevia/docs/android.md
EOF
