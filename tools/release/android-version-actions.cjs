// Version de l'appli Android pour Nx Release : lue et écrite dans <projet>/gradle.properties.
// versionName est le numéro choisi par Nx (semver, vu par les utilisateurs) ; versionCode, l'entier
// qu'Android compare pour accepter une mise à jour, augmente de 1 à chaque nouvelle version.
const { join } = require('node:path');
const { VersionActions } = require('nx/release');

const MANIFEST = 'gradle.properties';

/** Valeur de la propriété [key] dans le texte d'un fichier .properties, ou null */
function readProperty(text, key) {
    const match = new RegExp(`^${key}\\s*=\\s*(.*)$`, 'm').exec(text);
    return match ? match[1].trim() : null;
}

/** Texte avec la propriété [key] remplacée par [value] (elle doit exister) */
function writeProperty(text, key, value) {
    const pattern = new RegExp(`^(${key}\\s*=\\s*).*$`, 'm');
    if (!pattern.test(text)) throw new Error(`${MANIFEST} : propriété ${key} absente`);
    return text.replace(pattern, `$1${value}`);
}

class AndroidVersionActions extends VersionActions {
    validManifestFilenames = [MANIFEST];
    excludeManifestsFromFormatting = true;

    manifestPath() {
        return join(this.projectGraphNode.data.root, MANIFEST);
    }

    async readCurrentVersionFromSourceManifest(tree) {
        const text = tree.read(this.manifestPath(), 'utf-8');
        const currentVersion = text && readProperty(text, 'versionName');
        return currentVersion ? { currentVersion, manifestPath: this.manifestPath() } : null;
    }

    // L'APK n'est publié dans aucun registre interrogeable
    async readCurrentVersionFromRegistry() {
        return null;
    }

    // Les bibliothèques du cœur (libs/*) ne sont pas versionnées : aucune dépendance à mettre à jour
    async readDependencies() {
        return [];
    }

    async readCurrentVersionOfDependency() {
        return { currentVersion: null, dependencyCollection: null };
    }

    async updateProjectDependencies() {
        return [];
    }

    async updateProjectVersion(tree, newVersion) {
        const path = this.manifestPath();
        const text = tree.read(path, 'utf-8');
        const currentCode = Number(readProperty(text, 'versionCode'));
        if (!Number.isInteger(currentCode) || currentCode < 1) throw new Error(`${path} : versionCode invalide`);
        // 0.0.0 : jamais publiée, la première release garde le versionCode de départ (1)
        const newCode = readProperty(text, 'versionName') === '0.0.0' ? currentCode : currentCode + 1;
        tree.write(path, writeProperty(writeProperty(text, 'versionName', newVersion), 'versionCode', String(newCode)));
        return [`✍️  ${path} : versionName ${newVersion}, versionCode ${newCode}`];
    }
}

module.exports = AndroidVersionActions;
