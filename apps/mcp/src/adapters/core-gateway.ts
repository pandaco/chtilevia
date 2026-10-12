// Adaptateur v2 : répond au port ChtiGateway avec le cœur Kotlin compilé pour Node (libs/node,
// `npx nx run :node:build`). Mêmes réponses que le shell, sans curl, jq ni python3.
import { fileURLToPath } from 'node:url';

import type { ChtiResponse } from '../ports/chti-gateway.ts';
import { CommandGateway } from './command-gateway.ts';

/** Module produit par Gradle ; ses dépendances npm se résolvent depuis build/js/node_modules */
export const DEFAULT_CORE_MODULE = fileURLToPath(
    new URL('../../../../build/js/packages/chtilevia-node/kotlin/chtilevia-node.mjs', import.meta.url)
);

/** Ce qu'exporte libs/node (voir ChtiNode.kt) */
interface CoreModule {
    runChti(args: string[], env: Record<string, string | undefined>): { exit: number; out: string; err: string };
}

export interface CoreGatewayOptions {
    /** Chemin du module chtilevia-node.mjs (défaut : sortie de Gradle dans le dépôt) */
    modulePath?: string;
    /** Variables ajoutées à l'environnement (CHTI_NOW, CHTI_*_URL, XDG_*…), surtout pour les tests */
    env?: Record<string, string>;
}

export class CoreGateway extends CommandGateway {
    private readonly modulePath: string;
    private readonly env: Record<string, string>;
    private core?: Promise<CoreModule>;

    constructor(options: CoreGatewayOptions = {}) {
        super();
        this.modulePath = options.modulePath ?? process.env.CHTI_CORE_MODULE ?? DEFAULT_CORE_MODULE;
        this.env = options.env ?? {};
    }

    protected async run(args: string[]): Promise<ChtiResponse> {
        this.core ??= import(this.modulePath) as Promise<CoreModule>;
        // Appel synchrone : le cœur bloque pendant la commande (téléchargement du GTFS compris)
        const { out, err } = (await this.core).runChti(['--json', ...args], { ...process.env, ...this.env });
        try {
            return JSON.parse(out) as ChtiResponse;
        } catch {
            throw new Error(`chti ${args.join(' ')} : réponse illisible (${err.trim()})`);
        }
    }
}
