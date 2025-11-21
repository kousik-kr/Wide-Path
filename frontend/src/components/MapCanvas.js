import { jsx as _jsx } from "react/jsx-runtime";
import { useEffect, useRef } from 'react';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { useRouteStore } from '../hooks/useRouteStore';
import { useQuery } from '@tanstack/react-query';
import { fetchGraphPreview, fetchNetworkMetadata } from '../lib/api';
export default function MapCanvas() {
    const containerRef = useRef(null);
    const mapRef = useRef(null);
    const current = useRouteStore((s) => s.current);
    const { data: meta } = useQuery({ queryKey: ['network-meta'], queryFn: fetchNetworkMetadata });
    const { data: graph } = useQuery({
        queryKey: ['graph-preview', 800],
        queryFn: () => fetchGraphPreview(800),
        enabled: !!meta
    });
    useEffect(() => {
        if (!containerRef.current || mapRef.current || !meta)
            return;
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
        if (!map || !current)
            return;
        const updateRoute = () => {
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
            }
            else {
                map.getSource(sourceId).setData(geometry);
            }
        };
        if (!map.isStyleLoaded()) {
            map.once('load', updateRoute);
        }
        else {
            updateRoute();
        }
    }, [current]);
    useEffect(() => {
        const map = mapRef.current;
        if (!map || !graph)
            return;
        const featureCollection = {
            type: 'FeatureCollection',
            features: [
                ...graph.edges.map((edge) => ({
                    type: 'Feature',
                    geometry: { type: 'LineString', coordinates: edge.line },
                    properties: { from: edge.from, to: edge.to }
                })),
                ...graph.nodes.map((node) => ({
                    type: 'Feature',
                    geometry: { type: 'Point', coordinates: node.coord },
                    properties: { id: node.id }
                }))
            ]
        };
        const updateGraph = () => {
            const sourceId = 'graph-preview';
            if (!map.getSource(sourceId)) {
                map.addSource(sourceId, { type: 'geojson', data: featureCollection, lineMetrics: true });
                map.addLayer({
                    id: 'graph-edges',
                    type: 'line',
                    source: sourceId,
                    filter: ['==', '$type', 'LineString'],
                    layout: { 'line-cap': 'round', 'line-join': 'round' },
                    paint: {
                        'line-width': [
                            'interpolate',
                            ['linear'],
                            ['zoom'],
                            10,
                            0.9,
                            14,
                            1.6,
                            16,
                            2.6
                        ],
                        'line-opacity': 0.7,
                        'line-gradient': [
                            'interpolate',
                            ['linear'],
                            ['line-progress'],
                            0,
                            '#22c55e',
                            1,
                            '#7c3aed'
                        ]
                    }
                });
                map.addLayer({
                    id: 'graph-nodes',
                    type: 'circle',
                    source: sourceId,
                    filter: ['==', '$type', 'Point'],
                    paint: {
                        'circle-radius': [
                            'interpolate',
                            ['linear'],
                            ['zoom'],
                            10,
                            2.2,
                            16,
                            4.5
                        ],
                        'circle-color': '#0ea5e9',
                        'circle-stroke-color': '#0f172a',
                        'circle-stroke-width': 0.8,
                        'circle-opacity': 0.8
                    }
                });
            }
            else {
                map.getSource(sourceId).setData(featureCollection);
            }
        };
        if (!map.isStyleLoaded()) {
            map.once('load', updateGraph);
        }
        else {
            updateGraph();
        }
    }, [graph]);
    return _jsx("div", { ref: containerRef, className: "absolute inset-0" });
}
