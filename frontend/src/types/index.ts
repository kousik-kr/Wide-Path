export interface QueryPayload {
  source: number;
  destination: number;
  startDepartureMinutes: number;
  budgetMinutes: number;
}

export interface RouteResult {
  departureTime: number;
  score: number;
  rightTurns: number;
}

export interface RouteGeometry {
  type: 'Feature';
  geometry: GeoJSON.LineString;
  properties: {
    width?: number;
    widthMeters?: number[];
    clearway?: boolean[];
    timeSeries?: number[];
  };
}

export interface QueryResponse {
  result: RouteResult;
  geometry: RouteGeometry;
  diagnostics: {
    elapsedSeconds: number;
    memoryMb: number;
  };
}

export interface NodeSummary {
  id: number;
  latitude: number;
  longitude: number;
  degree: number;
}

export interface GraphPreview {
  nodes: { id: number; coord: [number, number] }[];
  edges: { from: number; to: number; line: [number, number][] }[];
}
