// Point d'entrée stdio : racine de composition (serveur MCP + passerelle vers chti).
// Usage : node apps/mcp/src/main.ts   (journal sur stderr, stdout réservé au protocole)
// CHTI_GATEWAY=core (défaut) : cœur Kotlin compilé pour Node ; CHTI_GATEWAY=shell : script apps/shell/chti.
import { serveStdio } from '@modelcontextprotocol/server/stdio';

import { CoreGateway } from './adapters/core-gateway.ts';
import { ShellGateway } from './adapters/shell-gateway.ts';
import { createServer } from './server.ts';

const kind = process.env.CHTI_GATEWAY ?? 'core';
if (kind !== 'core' && kind !== 'shell') {
    console.error(`CHTI_GATEWAY inconnu : ${kind} (core ou shell)`);
    process.exit(2);
}
const gateway = kind === 'shell' ? new ShellGateway() : new CoreGateway();
const handle = serveStdio(() => createServer(gateway));

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
    process.on(signal, () => {
        void handle.close();
    });
}
