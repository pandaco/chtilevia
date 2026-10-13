// Port de sortie du serveur MCP : ce dont les outils ont besoin, sans savoir qui y répond
// (script shell aujourd'hui, cœur Kotlin compilé pour Node demain).

export type TransitMode = 'bus' | 'tram' | 'metro';
export type BikeMode = 'all' | 'take' | 'return';

export interface DeparturesQuery {
    mode: TransitMode;
    /** Nom de ligne (10, L1, 1 pour le métro) ; absent pour le tram */
    line?: string;
    stop: string;
    direction?: string;
    count?: number;
}

export interface StopsQuery {
    mode: TransitMode;
    line?: string;
    direction?: string;
}

export interface NearbyQuery {
    /** Degrés décimaux (WGS 84) */
    latitude: number;
    longitude: number;
    /** Arrêts et stations V'Lille à donner (3 par défaut) */
    count?: number;
    /** Seulement ces arrêts (bus, tram, metro) ou seulement V'Lille (velo) ; tout par défaut */
    mode?: 'bus' | 'tram' | 'metro' | 'velo';
}

/** Document décrit par libs/contract/schema/response.schema.json (champ `type` discriminant) */
export interface ChtiResponse {
    type: string;
    [key: string]: unknown;
}

export interface ChtiGateway {
    departures(query: DeparturesQuery): Promise<ChtiResponse>;
    stops(query: StopsQuery): Promise<ChtiResponse>;
    bikeStations(station: string, mode: BikeMode): Promise<ChtiResponse>;
    nearby(query: NearbyQuery): Promise<ChtiResponse>;
    favorites(): Promise<ChtiResponse>;
    favorite(name: string): Promise<ChtiResponse>;
}

/** Réponse d'erreur au format du contrat */
export function errorResponse(code: string, message: string): ChtiResponse {
    return { type: 'error', code, message, details: {} };
}
