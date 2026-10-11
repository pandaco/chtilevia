// Adaptateur : répond au port ChtiGateway en lançant le script shell `chti --json`.
import { execFile } from 'node:child_process';
import { fileURLToPath } from 'node:url';

import type { BikeMode, ChtiGateway, ChtiResponse, DeparturesQuery, StopsQuery, TransitMode } from '../ports/chti-gateway.ts';
import { errorResponse } from '../ports/chti-gateway.ts';

export const DEFAULT_CHTI_PATH = fileURLToPath(new URL('../../../shell/chti', import.meta.url));

export interface ShellGatewayOptions {
    /** Chemin du script chti (défaut : apps/shell/chti du dépôt) */
    chtiPath?: string;
    /** Variables ajoutées à l'environnement (CHTI_NOW, CHTI_*_URL, XDG_*…), surtout pour les tests */
    env?: Record<string, string>;
    /** Délai maximal d'une commande ; la première mise à jour du GTFS peut être longue */
    timeoutMs?: number;
}

const BIKE_FLAGS: Record<BikeMode, string[]> = { all: [], take: ['--prendre'], return: ['--deposer'] };

export class ShellGateway implements ChtiGateway {
    private readonly chtiPath: string;
    private readonly env: Record<string, string>;
    private readonly timeoutMs: number;

    constructor(options: ShellGatewayOptions = {}) {
        this.chtiPath = options.chtiPath ?? process.env.CHTI_PATH ?? DEFAULT_CHTI_PATH;
        this.env = options.env ?? {};
        this.timeoutMs = options.timeoutMs ?? 300_000;
    }

    departures(query: DeparturesQuery): Promise<ChtiResponse> {
        // Arrêt et direction passés d'un seul tenant : un mot comme « vers » dans un nom reste un nom
        const args = [...lineArgs(query.mode, query.line), query.stop];
        if (query.direction) args.push(`--vers=${query.direction}`);
        if (query.count !== undefined) args.push(`--nombre=${query.count}`);
        return this.run(args);
    }

    stops(query: StopsQuery): Promise<ChtiResponse> {
        const args = lineArgs(query.mode, query.line);
        if (query.direction) args.push(`--vers=${query.direction}`);
        return this.run(args);
    }

    bikeStations(station: string, mode: BikeMode): Promise<ChtiResponse> {
        return this.run(['velo', station, ...BIKE_FLAGS[mode]]);
    }

    favorites(): Promise<ChtiResponse> {
        return this.run(['favoris']);
    }

    async favorite(name: string): Promise<ChtiResponse> {
        // chti <nom> lancerait aussi une commande (update, vocal…) : on n'accepte qu'un vrai favori
        const list = await this.favorites();
        const known = (list.favorites as { name: string }[] | undefined) ?? [];
        if (!known.some(f => f.name.toLowerCase() === name.toLowerCase())) {
            return errorResponse('unknown_command', `favori inconnu : ${name} (voir lister_favoris)`);
        }
        return this.run([name]);
    }

    private run(args: string[]): Promise<ChtiResponse> {
        return new Promise((resolve, reject) => {
            execFile(
                this.chtiPath,
                ['--json', ...args],
                { env: { ...process.env, ...this.env }, timeout: this.timeoutMs, maxBuffer: 10 * 1024 * 1024 },
                (error, stdout, stderr) => {
                    // Code retour 1 = erreur métier, déjà décrite en JSON sur stdout
                    try {
                        resolve(JSON.parse(stdout) as ChtiResponse);
                    } catch {
                        reject(new Error(`chti ${args.join(' ')} : réponse illisible (${stderr.trim() || error?.message})`));
                    }
                }
            );
        });
    }
}

function lineArgs(mode: TransitMode, line?: string): string[] {
    if (mode === 'tram') return ['tram'];
    return [mode, line ?? ''];
}
