import { useQuery } from '@tanstack/react-query';
import { API_BASE } from '../lib/api';

interface LiveMetrics {
  elapsedSeconds: number;
  memoryMb: number;
}

export function useLiveMetrics(enabled: boolean) {
  return useQuery<LiveMetrics>({
    queryKey: ['live-metrics'],
    queryFn: async () => {
      const res = await fetch(`${API_BASE}/metrics/live`);
      if (!res.ok) throw new Error('Failed to load metrics');
      return res.json() as Promise<LiveMetrics>;
    },
    enabled,
    refetchInterval: enabled ? 2000 : false
  });
}
