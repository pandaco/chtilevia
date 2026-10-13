// Base des adaptateurs qui répondent au port ChtiGateway en lançant une commande chti --json :
// traduction des requêtes en arguments, commune au script shell et au cœur Kotlin.
import type { BikeMode, ChtiGateway, ChtiResponse, DeparturesQuery, NearbyQuery, StopsQuery, TransitMode } from '../ports/chti-gateway.ts';
import { errorResponse } from '../ports/chti-gateway.ts';

const BIKE_FLAGS: Record<BikeMode, string[]> = { all: [], take: ['--prendre'], return: ['--deposer'] };

export abstract class CommandGateway implements ChtiGateway {
    /** Lance `chti --json <args>` et renvoie le document JSON produit */
    protected abstract run(args: string[]): Promise<ChtiResponse>;

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

    nearby(query: NearbyQuery): Promise<ChtiResponse> {
        // Notation décimale fixe : jamais d'exposant (1e-7), que chti refuserait
        const args = ['autour', ...(query.mode ? [query.mode] : []), query.latitude.toFixed(6), query.longitude.toFixed(6)];
        if (query.count !== undefined) args.push(`--nombre=${query.count}`);
        return this.run(args);
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
}

function lineArgs(mode: TransitMode, line?: string): string[] {
    if (mode === 'tram') return ['tram'];
    return [mode, line ?? ''];
}
