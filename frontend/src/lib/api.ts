import type { GraphPreview, NodeSummary, QueryPayload, QueryResponse } from '../types';

export const API_BASE = import.meta.env.VITE_API_BASE ?? '/api';

async function request<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  const res = await fetch(input, init);
  if (!res.ok) {
    throw new Error(await res.text());
  }
  return res.json() as Promise<T>;
}

export function fetchNodes(search: string) {
  const params = new URLSearchParams({ search });
  return request<NodeSummary[]>(`${API_BASE}/nodes?${params.toString()}`);
}

export function runQuery(body: QueryPayload, signal?: AbortSignal) {
  return request<QueryResponse>(`${API_BASE}/queries/run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    signal
  });
}

export function fetchNetworkMetadata() {
  return request<{ vertexCount: number; bounds: [number, number, number, number] }>(
    `${API_BASE}/network/meta`
  );
}

export function fetchGraphPreview(maxEdges = 800) {
  const params = new URLSearchParams({ maxEdges: String(maxEdges) });
  return request<GraphPreview>(`${API_BASE}/network/graph?${params.toString()}`);
}
