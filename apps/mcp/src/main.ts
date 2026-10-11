// Point d'entrée stdio : racine de composition (serveur MCP + adaptateur shell).
// Usage : node apps/mcp/src/main.ts   (journal sur stderr, stdout réservé au protocole)
import { serveStdio } from '@modelcontextprotocol/server/stdio';

import { ShellGateway } from './adapters/shell-gateway.ts';
import { createServer } from './server.ts';

const gateway = new ShellGateway();
const handle = serveStdio(() => createServer(gateway));

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
    process.on(signal, () => {
        void handle.close();
    });
}
