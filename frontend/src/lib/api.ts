import type { NodeSummary, QueryPayload, QueryResponse } from '../types';

const BASE_URL = import.meta.env.VITE_API_BASE ?? '/api';

async function request<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  const res = await fetch(input, init);
  if (!res.ok) {
    throw new Error(await res.text());
  }
  return res.json() as Promise<T>;
}

export function fetchNodes(search: string) {
  const params = new URLSearchParams({ search });
  return request<NodeSummary[]>(`${BASE_URL}/nodes?${params.toString()}`);
}

export function runQuery(body: QueryPayload) {
  return request<QueryResponse>(`${BASE_URL}/queries/run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export function fetchNetworkMetadata() {
  return request<{ vertexCount: number; bounds: [number, number, number, number] }>(
    `${BASE_URL}/network/meta`
  );
}
