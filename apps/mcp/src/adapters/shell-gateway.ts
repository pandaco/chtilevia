// Adaptateur v1 : répond au port ChtiGateway en lançant le script shell `chti --json`
// (curl, jq et python3 nécessaires).
import { execFile } from 'node:child_process';
import { fileURLToPath } from 'node:url';

import type { ChtiResponse } from '../ports/chti-gateway.ts';
import { CommandGateway } from './command-gateway.ts';

export const DEFAULT_CHTI_PATH = fileURLToPath(new URL('../../../shell/chti', import.meta.url));

export interface ShellGatewayOptions {
    /** Chemin du script chti (défaut : apps/shell/chti du dépôt) */
    chtiPath?: string;
    /** Variables ajoutées à l'environnement (CHTI_NOW, CHTI_*_URL, XDG_*…), surtout pour les tests */
    env?: Record<string, string>;
    /** Délai maximal d'une commande ; la première mise à jour du GTFS peut être longue */
    timeoutMs?: number;
}

export class ShellGateway extends CommandGateway {
    private readonly chtiPath: string;
    private readonly env: Record<string, string>;
    private readonly timeoutMs: number;

    constructor(options: ShellGatewayOptions = {}) {
        super();
        this.chtiPath = options.chtiPath ?? process.env.CHTI_PATH ?? DEFAULT_CHTI_PATH;
        this.env = options.env ?? {};
        this.timeoutMs = options.timeoutMs ?? 300_000;
    }

    protected run(args: string[]): Promise<ChtiResponse> {
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
