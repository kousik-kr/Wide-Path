import { useQuery } from '@tanstack/react-query';
import { API_BASE } from '../lib/api';
export function useLiveMetrics(enabled) {
    return useQuery({
        queryKey: ['live-metrics'],
        queryFn: async () => {
            const res = await fetch(`${API_BASE}/metrics/live`);
            if (!res.ok)
                throw new Error('Failed to load metrics');
            return res.json();
        },
        enabled,
        refetchInterval: enabled ? 2000 : false
    });
}
