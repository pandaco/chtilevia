// Le cœur Kotlin sous Node garde le référentiel en mémoire entre deux recherches, comme l'appli
// Android : un serveur MCP qui reste lancé ne relit pas ses fichiers GTFS à chaque appel.
import assert from 'node:assert/strict';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { after, test } from 'node:test';
import { fileURLToPath } from 'node:url';

import { CoreGateway } from '../src/adapters/core-gateway.ts';

const FIX = fileURLToPath(new URL('../../../libs/contract/fixtures', import.meta.url));
const cache = mkdtempSync(join(tmpdir(), 'chti-core-gateway.'));
after(() => rmSync(cache, { recursive: true, force: true }));

test('référentiel gardé en mémoire entre deux recherches', async () => {
    const gateway = new CoreGateway({
        env: {
            CHTI_NOW: '1791629160',
            CHTI_GTFS_URL: `file://${FIX}/gtfs.zip`,
            CHTI_GBFS_URL: `file://${FIX}`,
            CHTI_RT_URL: `file://${FIX}/absent.pb`,
            XDG_CACHE_HOME: cache,
            XDG_CONFIG_HOME: join(FIX, 'config')
        }
    });
    const query = { mode: 'metro' as const, line: '1', stop: 'republique', count: 2 };
    const first = await gateway.departures(query);
    assert.equal(first.type, 'departures', JSON.stringify(first));

    // Fichiers du référentiel retirés, horodatage gardé : seule la mémoire peut encore répondre
    const gtfs = join(cache, 'chtipasse', 'gtfs');
    for (const name of ['routes.txt', 'trips.txt', 'stops.txt', 'calendar_dates.txt', 'idx']) {
        rmSync(join(gtfs, name), { recursive: true, force: true });
    }

    assert.deepEqual(await gateway.departures(query), first);
});
