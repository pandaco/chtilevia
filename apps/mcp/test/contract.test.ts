// Contrat : chaque appel d'outil MCP doit rendre exactement le document attendu par libs/contract
// (mêmes fixtures, même heure figée que le cas json-* correspondant), et respecter le schéma.
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readdirSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, before, describe, test } from 'node:test';
import { fileURLToPath } from 'node:url';

import { Client, InMemoryTransport } from '@modelcontextprotocol/client';
import { Ajv2020 } from 'ajv/dist/2020.js';
import addFormatsModule from 'ajv-formats';

import { DEFAULT_CHTI_PATH, ShellGateway } from '../src/adapters/shell-gateway.ts';
import { RESPONSE_SCHEMA_PATH } from '../src/contract-schema.ts';
import { createServer } from '../src/server.ts';

const ROOT = fileURLToPath(new URL('../../../', import.meta.url)).replace(/\/$/, '');
const CONTRACT = join(ROOT, 'libs/contract');
const FIX = join(CONTRACT, 'fixtures');

const clocks = new Map(
    readFileSync(join(CONTRACT, 'clocks.txt'), 'utf8')
        .split('\n')
        .filter(l => l.trim() && !l.trimStart().startsWith('#'))
        .map(l => l.split('|').map(c => c.trim()))
        .map(([name, epoch]) => [name, epoch] as [string, string])
);
const cases = new Map(
    readFileSync(join(CONTRACT, 'cases.txt'), 'utf8')
        .split('\n')
        .filter(l => l.trim() && !l.trimStart().startsWith('#'))
        .map(l => l.split('|').map(c => c.trim()))
        .map(([name, clock, rt]) => [name, { clock, rt }] as [string, { clock: string; rt: string }])
);

/** Document attendu et code retour d'un cas du contrat (chemin de la racine rétabli) */
function expected(name: string): { doc: unknown; exit: number } {
    const lines = readFileSync(join(CONTRACT, 'expected', `${name}.txt`), 'utf8').trimEnd().split('\n');
    const exit = Number(/^\[exit (\d+)\]$/.exec(lines.pop() ?? '')?.[1]);
    return { doc: JSON.parse(lines.join('\n').replaceAll('<racine>', ROOT)), exit };
}

const addFormats = addFormatsModule as unknown as (ajv: Ajv2020) => Ajv2020;
const ajv = addFormats(new Ajv2020({ allErrors: true, strict: true }));
const validateResponse = ajv.compile(JSON.parse(readFileSync(RESPONSE_SCHEMA_PATH, 'utf8')));

let cache: string;

before(() => {
    // Index GTFS construit une fois, comme apps/shell/tests/contract.sh
    cache = mkdtempSync(join(tmpdir(), 'chti-mcp-tests.'));
    execFileSync(DEFAULT_CHTI_PATH, ['update'], { env: { ...process.env, ...fixtureEnv('MIDI', 'aucun') }, stdio: 'ignore' });
});
after(() => rmSync(cache, { recursive: true, force: true }));

function fixtureEnv(clock: string, rt: string): Record<string, string> {
    const epoch = clocks.get(clock);
    assert.ok(epoch, `heure inconnue : ${clock}`);
    return {
        CHTI_NOW: epoch,
        CHTI_GTFS_URL: `file://${FIX}/gtfs.zip`,
        CHTI_GBFS_URL: `file://${FIX}`,
        CHTI_RT_URL: `file://${FIX}/${rt === 'aucun' ? 'absent.pb' : rt}`,
        XDG_CACHE_HOME: cache,
        XDG_CONFIG_HOME: join(FIX, 'config')
    };
}

/** Client MCP relié en mémoire à un serveur dont la passerelle tourne dans les conditions du cas */
async function connect(caseName: string): Promise<Client> {
    const c = cases.get(caseName);
    assert.ok(c, `cas inconnu dans cases.txt : ${caseName}`);
    const server = createServer(new ShellGateway({ env: fixtureEnv(c.clock, c.rt) }));
    const client = new Client({ name: 'chti-tests', version: '0.0.0' });
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    await server.connect(serverTransport);
    await client.connect(clientTransport);
    return client;
}

// Appel d'outil -> cas du contrat dont le document doit être identique
const PARITY: [string, string, Record<string, unknown>][] = [
    ['json-bus-temps-reel', 'prochains_passages', { mode: 'bus', ligne: '10', arret: 'marais', direction: 'flandres', nombre: 2 }],
    ['json-bus-sans-temps-reel', 'prochains_passages', { mode: 'bus', ligne: '10', arret: 'gambetta', nombre: 2 }],
    ['json-metro-theorique', 'prochains_passages', { mode: 'metro', ligne: '1', arret: 'republique', nombre: 2 }],
    ['json-minuit-lendemain', 'prochains_passages', { mode: 'bus', ligne: '10', arret: 'marais', direction: 'flandres', nombre: 4 }],
    ['json-tram-temps-reel', 'prochains_passages', { mode: 'tram', arret: 'croise laroche', direction: 'roubaix', nombre: 2 }],
    ['json-liste-arrets', 'lister_arrets', { mode: 'bus', ligne: '10' }],
    ['json-velo-station', 'station_velo', { station: 'rihour' }],
    ['json-velo-plusieurs', 'station_velo', { station: 'gare' }],
    ['json-velo-hors-service', 'station_velo', { station: 'port de lille' }],
    ['json-velo-prendre', 'station_velo', { station: 'rihour', mode: 'prendre' }],
    ['json-favoris-liste', 'lister_favoris', {}],
    ['json-favori-tram', 'favori', { nom: 'travail' }],
    ['json-favori-velo', 'favori', { nom: 'rentrer-velo' }],
    ['json-favori-en-erreur', 'favori', { nom: 'casse' }],
    ['json-erreur-ligne', 'prochains_passages', { mode: 'bus', ligne: '999', arret: 'marais' }],
    ['json-erreur-direction', 'prochains_passages', { mode: 'bus', ligne: '10', arret: 'marais', direction: 'zzz' }],
    ['json-erreur-arret', 'prochains_passages', { mode: 'bus', ligne: '10', arret: 'zzz' }],
    ['json-erreur-arret-autre-sens', 'prochains_passages', { mode: 'bus', ligne: '10', arret: 'esplanade', direction: 'flandres' }],
    ['json-erreur-velo', 'station_velo', { station: 'zzz' }],
    ['json-approx-ambigu', 'prochains_passages', { mode: 'bus', ligne: '10', arret: 'marie' }]
];

describe('parité avec le contrat chti --json', () => {
    for (const [caseName, tool, args] of PARITY) {
        test(`${tool} ${JSON.stringify(args)} ≡ ${caseName}`, async () => {
            const client = await connect(caseName);
            const result = await client.callTool({ name: tool, arguments: args });
            const { doc } = expected(caseName);
            assert.deepEqual(result.structuredContent, doc);
            assert.equal(result.isError ?? false, (doc as { type: string }).type === 'error');
            assert.deepEqual(JSON.parse((result.content as { text: string }[])[0].text), doc);
            await client.close();
        });
    }
});

describe('garde-fous du serveur', () => {
    test('liste des outils', async () => {
        const client = await connect('json-favoris-liste');
        const { tools } = await client.listTools();
        assert.deepEqual(tools.map(t => t.name).sort(), ['favori', 'lister_arrets', 'lister_favoris', 'prochains_passages', 'station_velo']);
        for (const t of tools) {
            assert.ok(t.description, `${t.name} sans description`);
            assert.equal(t.outputSchema?.type, 'object');
            assert.equal(t.annotations?.readOnlyHint, true);
        }
        await client.close();
    });

    test('schémas portables : un seul type par nœud (anyOf pour les valeurs nullables)', async () => {
        // Certains clients (déclarations de fonctions Gemini…) refusent « type » sous forme de tableau
        const client = await connect('json-favoris-liste');
        const { tools } = await client.listTools();
        await client.close();
        const arrayTypes: string[] = [];
        const visit = (node: unknown, path: string): void => {
            if (Array.isArray(node)) return node.forEach((n, i) => visit(n, `${path}[${i}]`));
            if (node === null || typeof node !== 'object') return;
            for (const [key, value] of Object.entries(node)) {
                if (key === 'type' && Array.isArray(value)) arrayTypes.push(path);
                visit(value, `${path}.${key}`);
            }
        };
        for (const t of tools) {
            visit(t.inputSchema, `${t.name}.inputSchema`);
            visit(t.outputSchema, `${t.name}.outputSchema`);
        }
        assert.deepEqual(arrayTypes, []);
    });

    test('bus sans ligne : erreur missing_line, sans lancer chti', async () => {
        const client = await connect('json-bus-temps-reel');
        const result = await client.callTool({ name: 'prochains_passages', arguments: { mode: 'bus', arret: 'marais' } });
        assert.equal(result.isError, true);
        assert.equal((result.structuredContent as { code: string }).code, 'missing_line');
        await client.close();
    });

    test("favori : une commande chti (update) n'est pas un favori", async () => {
        const client = await connect('json-favoris-liste');
        const result = await client.callTool({ name: 'favori', arguments: { nom: 'update' } });
        assert.equal(result.isError, true);
        assert.equal((result.structuredContent as { code: string }).code, 'unknown_command');
        await client.close();
    });

    test('un texte qui ressemble à une option est refusé', async () => {
        const client = await connect('json-bus-temps-reel');
        const result = await client.callTool({
            name: 'prochains_passages',
            arguments: { mode: 'bus', ligne: '10', arret: '--voix' }
        });
        assert.equal(result.isError, true);
        assert.match((result.content as { text: string }[])[0].text, /ne doit pas commencer par « - »/);
        await client.close();
    });
});

describe('schéma du contrat', () => {
    const files = readdirSync(join(CONTRACT, 'expected')).filter(f => f.startsWith('json-'));

    test('il y a des sorties JSON à valider', () => assert.ok(files.length > 0));
    for (const f of files) {
        test(`${f} respecte response.schema.json`, () => {
            const { doc } = expected(f.replace(/\.txt$/, ''));
            assert.ok(validateResponse(doc), JSON.stringify(validateResponse.errors, null, 1));
        });
    }
});
