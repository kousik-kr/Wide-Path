export const API_BASE = import.meta.env.VITE_API_BASE ?? '/api';
async function request(input, init) {
    const res = await fetch(input, init);
    if (!res.ok) {
        throw new Error(await res.text());
    }
    return res.json();
}
export function fetchNodes(search) {
    const params = new URLSearchParams({ search });
    return request(`${API_BASE}/nodes?${params.toString()}`);
}
export function runQuery(body) {
    return request(`${API_BASE}/queries/run`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
}
export function fetchNetworkMetadata() {
    return request(`${API_BASE}/network/meta`);
}
export function fetchGraphPreview(maxEdges = 800) {
    const params = new URLSearchParams({ maxEdges: String(maxEdges) });
    return request(`${API_BASE}/network/graph?${params.toString()}`);
}
