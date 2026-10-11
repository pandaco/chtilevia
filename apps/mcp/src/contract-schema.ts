// Schémas de sortie des outils, tirés du contrat commun (libs/contract/schema/response.schema.json)
// plutôt que recopiés : le serveur MCP ne peut pas diverger du format de chti --json.
import { readFileSync } from 'node:fs';

export const RESPONSE_SCHEMA_PATH = new URL('../../../libs/contract/schema/response.schema.json', import.meta.url);

interface ResponseSchema {
    $defs: Record<string, unknown>;
}

const responseSchema = JSON.parse(readFileSync(RESPONSE_SCHEMA_PATH, 'utf8')) as ResponseSchema;

/**
 * Schéma JSON d'un outil : l'un des types de réponse donnés, ou une erreur.
 * Les $defs du contrat sont embarquées pour que les $ref restent résolubles.
 */
export function outputSchemaFor(...types: string[]): Record<string, unknown> {
    for (const t of types) {
        if (!(t in responseSchema.$defs)) throw new Error(`type de réponse inconnu du contrat : ${t}`);
    }
    return {
        type: 'object',
        oneOf: [...types, 'error'].map(t => ({ $ref: `#/$defs/${t}` })),
        $defs: responseSchema.$defs
    };
}
