// Parité des catalogues : les AppFunctions de l'appli Android (apps/android, ChtiAppFunctions.kt)
// exposent les outils de ce serveur, mêmes noms (en camelCase) et mêmes paramètres, avec le même
// caractère obligatoire ou facultatif. Lu dans le source Kotlin : pas besoin de construire l'appli.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

import { Client, InMemoryTransport } from '@modelcontextprotocol/client';

import type { ChtiGateway } from '../src/ports/chti-gateway.ts';
import { createServer } from '../src/server.ts';

const APP_FUNCTIONS = fileURLToPath(
    new URL('../../android/src/main/kotlin/fr/chtilevia/android/ChtiAppFunctions.kt', import.meta.url)
);

interface Signature {
    /** Paramètre → obligatoire */
    params: Record<string, boolean>;
}

/** Fonctions `@AppFunction` du source Kotlin ; un paramètre nullable ou avec valeur par défaut est facultatif */
function appFunctions(): Record<string, Signature> {
    const source = readFileSync(APP_FUNCTIONS, 'utf8');
    const functions: Record<string, Signature> = {};
    for (const [, name, list] of source.matchAll(/@AppFunction\([^)]*\)\s*suspend fun (\w+)\(([^)]*)\)/g)) {
        const params: Record<string, boolean> = {};
        for (const [, param, type, value] of list.matchAll(/(\w+)\s*:\s*([\w.?]+)(\s*=[^,]+)?/g)) {
            params[param] = !type.endsWith('?') && value === undefined;
        }
        functions[name] = { params };
    }
    return functions;
}

const camelCase = (name: string) => name.replace(/_(\w)/g, (_, c: string) => c.toUpperCase());

test('les AppFunctions Android exposent les outils MCP, avec les mêmes paramètres', async () => {
    // Le catalogue seul est comparé : la passerelle n'est jamais appelée
    const server = createServer(new Proxy({}, { get: () => () => assert.fail('passerelle appelée') }) as ChtiGateway);
    const client = new Client({ name: 'parite', version: '0' });
    const [clientSide, serverSide] = InMemoryTransport.createLinkedPair();
    await Promise.all([server.connect(serverSide), client.connect(clientSide)]);
    const { tools } = await client.listTools();
    await client.close();

    const mcp: Record<string, Signature> = {};
    for (const tool of tools) {
        const required = new Set(tool.inputSchema.required ?? []);
        const params: Record<string, boolean> = {};
        for (const param of Object.keys(tool.inputSchema.properties ?? {})) params[param] = required.has(param);
        mcp[camelCase(tool.name)] = { params };
    }
    const android = appFunctions();
    assert.ok(Object.keys(android).length > 0, 'aucune @AppFunction trouvée dans ChtiAppFunctions.kt');
    assert.deepEqual(android, mcp);
});
