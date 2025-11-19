import { useEffect, useRef } from 'react';
import maplibregl, { Map } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { useRouteStore } from '../hooks/useRouteStore';
import { useQuery } from '@tanstack/react-query';
import { fetchNetworkMetadata } from '../lib/api';

export default function MapCanvas() {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<Map | null>(null);
  const current = useRouteStore((s) => s.current);
  const { data: meta } = useQuery({ queryKey: ['network-meta'], queryFn: fetchNetworkMetadata });

  useEffect(() => {
    if (!containerRef.current || mapRef.current || !meta) return;
    const [west, south, east, north] = meta.bounds;
    mapRef.current = new maplibregl.Map({
      container: containerRef.current,
      style: 'https://demotiles.maplibre.org/style.json',
      bounds: [west, south, east, north],
      fitBoundsOptions: { padding: 40 }
    });
    mapRef.current.addControl(new maplibregl.NavigationControl());
  }, [meta]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !current) return;

    const sourceId = 'route';
    const { geometry } = current.response;
    // Keep a single source/layer pair alive and simply swap the GeoJSON data so
    // MapLibre preserves any built-in controls and camera state between runs.
    if (!map.getSource(sourceId)) {
      map.addSource(sourceId, { type: 'geojson', data: geometry });
      map.addLayer({
        id: 'route-layer',
        type: 'line',
        source: sourceId,
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: {
          'line-color': [
            'interpolate',
            ['linear'],
            ['get', 'width'],
            2.0,
            '#f97316',
            4.0,
            '#16a34a'
          ],
          'line-width': [
            'interpolate',
            ['linear'],
            ['zoom'],
            10,
            3,
            16,
            9
          ]
        }
      });
    } else {
      (map.getSource(sourceId) as maplibregl.GeoJSONSource).setData(geometry);
    }
  }, [current]);

  return <div ref={containerRef} className="absolute inset-0" />;
}
