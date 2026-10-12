// Adaptateur d'entrée MCP : traduit les appels d'outils en requêtes sur le port ChtiGateway.
import { fromJsonSchema, McpServer } from '@modelcontextprotocol/server';
import * as z from 'zod/v4';

import { outputSchemaFor } from './contract-schema.ts';
import type { ChtiGateway, ChtiResponse } from './ports/chti-gateway.ts';
import { errorResponse } from './ports/chti-gateway.ts';

export const SERVER_INFO = { name: 'chtilevia', version: '0.1.0' };

const INSTRUCTIONS = `Transports en commun de la métropole lilloise (réseau Ilévia : bus, tram, métro) et vélos V'Lille.
Les noms d'arrêt et de station sont cherchés sans tenir compte de la casse ni des accents ; une partie du nom suffit.
Pour les arrêts et les directions, les fautes d'orthographe ou de reconnaissance vocale sont tolérées (« gambeta », « croix la roche ») ; si plusieurs arrêts ressemblent autant à la demande, l'erreur ambiguous_stop les liste dans details.suggestions : faire choisir l'utilisateur.
Pour une position (lat, lon), autour_de_moi donne les arrêts proches avec leurs lignes : enchaîner avec prochains_passages.
Les horaires sont en temps réel quand le flux Ilévia le permet (realtime: true), sinon théoriques. Le métro n'a que des horaires théoriques.
En cas d'erreur, la réponse a "type": "error", un code stable et un message en français ; details.directions liste les directions possibles quand la direction demandée n'existe pas.`;

// Texte libre venant de l'assistant : jamais une option de chti
const text = (description: string) =>
    z
        .string()
        .trim()
        .min(1)
        .regex(/^[^-]/, 'ne doit pas commencer par « - »')
        .describe(description);

const transitMode = z.enum(['bus', 'tram', 'metro']).describe('bus, tram ou metro');
const line = text("Ligne : pour le bus, son nom affiché (10, L1, CO3, Z2…) ; pour le métro, 1 ou 2. Inutile pour le tram (une seule ligne).");
const direction = text("Tout ou partie du nom de la destination (ex. « flandres », « roubaix », « chu »).");

const READ_ONLY = { readOnlyHint: true, openWorldHint: true } as const;

export function createServer(gateway: ChtiGateway): McpServer {
    const server = new McpServer(SERVER_INFO, { instructions: INSTRUCTIONS });

    server.registerTool(
        'prochains_passages',
        {
            title: 'Prochains passages',
            description:
                "Prochains passages d'un bus, du tram ou d'un métro Ilévia à un arrêt, regroupés par destination, avec l'heure et les minutes restantes.",
            inputSchema: z.object({
                mode: transitMode,
                ligne: line.optional(),
                arret: text("Tout ou partie du nom de l'arrêt (ex. « gambetta », « croisé laroche »)."),
                direction: direction.optional(),
                nombre: z.number().int().min(1).max(20).optional().describe('Passages par direction (3 par défaut)')
            }),
            outputSchema: fromJsonSchema(outputSchemaFor('departures')),
            annotations: READ_ONLY
        },
        async ({ mode, ligne, arret, direction, nombre }) => {
            const missing = missingLine(mode, ligne);
            if (missing) return toResult(missing);
            return toResult(await gateway.departures({ mode, line: ligne, stop: arret, direction, count: nombre }));
        }
    );

    server.registerTool(
        'lister_arrets',
        {
            title: "Arrêts d'une ligne",
            description: "Liste ordonnée des arrêts d'une ligne Ilévia pour chaque sens, éventuellement pour une seule direction.",
            inputSchema: z.object({ mode: transitMode, ligne: line.optional(), direction: direction.optional() }),
            outputSchema: fromJsonSchema(outputSchemaFor('stops')),
            annotations: READ_ONLY
        },
        async ({ mode, ligne, direction }) => {
            const missing = missingLine(mode, ligne);
            if (missing) return toResult(missing);
            return toResult(await gateway.stops({ mode, line: ligne, direction }));
        }
    );

    server.registerTool(
        'station_velo',
        {
            title: 'Station V’Lille',
            description:
                "Vélos disponibles et places libres des stations V'Lille dont le nom contient la recherche, avec leur état (hors service, location ou dépôt possible).",
            inputSchema: z.object({
                station: text('Tout ou partie du nom de la station (ex. « rihour », « gare lille europe »).'),
                mode: z
                    .enum(['tout', 'prendre', 'deposer'])
                    .optional()
                    .describe("Intention : prendre un vélo, en déposer un, ou tout (par défaut)")
            }),
            outputSchema: fromJsonSchema(outputSchemaFor('bike_stations')),
            annotations: READ_ONLY
        },
        async ({ station, mode }) => {
            const bikeMode = mode === 'prendre' ? 'take' : mode === 'deposer' ? 'return' : 'all';
            return toResult(await gateway.bikeStations(station, bikeMode));
        }
    );

    server.registerTool(
        'autour_de_moi',
        {
            title: 'Autour de moi',
            description:
                "Arrêts Ilévia les plus proches d'une position (avec les lignes qui les desservent) et stations V'Lille les plus proches (avec vélos et places), triés par distance à vol d'oiseau.",
            inputSchema: z.object({
                latitude: z.number().min(-90).max(90).describe('Latitude en degrés décimaux, ex. 50.6366'),
                longitude: z.number().min(-180).max(180).describe('Longitude en degrés décimaux, ex. 3.0630'),
                nombre: z.number().int().min(1).max(10).optional().describe("Arrêts et stations à donner (3 par défaut)")
            }),
            outputSchema: fromJsonSchema(outputSchemaFor('nearby')),
            annotations: READ_ONLY
        },
        async ({ latitude, longitude, nombre }) => toResult(await gateway.nearby({ latitude, longitude, count: nombre }))
    );

    server.registerTool(
        'lister_favoris',
        {
            title: 'Favoris',
            description: "Trajets favoris de l'utilisateur : un nom court et les commandes chti qu'il lance.",
            inputSchema: z.object({}),
            outputSchema: fromJsonSchema(outputSchemaFor('favorites')),
            annotations: { ...READ_ONLY, openWorldHint: false }
        },
        async () => toResult(await gateway.favorites())
    );

    server.registerTool(
        'favori',
        {
            title: 'Lancer un favori',
            description:
                "Lance un trajet favori de l'utilisateur (voir lister_favoris) et renvoie le résultat de chacune de ses commandes (passages, vélos…).",
            inputSchema: z.object({ nom: text('Nom du favori, ex. « travail ».') }),
            outputSchema: fromJsonSchema(outputSchemaFor('favorite')),
            annotations: READ_ONLY
        },
        async ({ nom }) => toResult(await gateway.favorite(nom))
    );

    return server;
}

function missingLine(mode: string, ligne?: string): ChtiResponse | undefined {
    if (mode === 'tram' || ligne) return undefined;
    return errorResponse('missing_line', mode === 'metro' ? 'précise la ligne de métro : 1 ou 2' : 'précise une ligne, ex : 10');
}

function toResult(doc: ChtiResponse) {
    const isError = doc.type === 'error';
    return {
        // Le texte reprend le JSON : les clients sans structuredContent ont la même information
        content: [{ type: 'text' as const, text: JSON.stringify(doc) }],
        structuredContent: doc,
        isError
    };
}
