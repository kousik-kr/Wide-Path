import { useEffect, useMemo, useState } from 'react';
import classNames from 'classnames';
import { twMerge } from 'tailwind-merge';
import { useQuery } from '@tanstack/react-query';
import { fetchNodes } from '../lib/api';
import type { NodeSummary } from '../types';

interface NodeSelectProps {
  label: string;
  value: number;
  onSelect: (node: NodeSummary) => void;
}

export default function NodeSelect({ label, value, onSelect }: NodeSelectProps) {
  const [search, setSearch] = useState('');
  const [manualValue, setManualValue] = useState(value.toString());
  const [debounced, setDebounced] = useState('');

  useEffect(() => {
    const id = setTimeout(() => setDebounced(search), 250);
    return () => clearTimeout(id);
  }, [search]);

  const query = useQuery({
    queryKey: ['nodes', debounced],
    queryFn: () => fetchNodes(debounced),
    enabled: debounced.length > 1
  });

  useEffect(() => {
    setManualValue(String(value));
  }, [value]);

  const items = query.data ?? [];
  const showDropdown = debounced.length > 1 && items.length > 0;

  const helperText = useMemo(() => {
    if (query.isLoading) return 'Searching…';
    if (debounced.length > 1 && items.length === 0) return 'No matches';
    if (debounced.length <= 1) return 'Type at least 2 characters';
    return '';
  }, [debounced.length, items.length, query.isLoading]);

  return (
    <div>
      <label className="block text-sm font-medium text-slate-200">{label}</label>
      <div className="mt-2 space-y-2">
        <input
          type="text"
          value={search}
          placeholder="Search by id or metadata"
          onChange={(event) => setSearch(event.target.value)}
          className="w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white"
        />
        {helperText ? <p className="text-xs text-slate-500">{helperText}</p> : null}
        {showDropdown ? (
          <ul className="max-h-48 space-y-1 overflow-y-auto rounded-lg border border-slate-700 bg-slate-900/80 p-2 text-sm">
            {items.map((node) => (
              <li
                key={node.id}
                className={twMerge(
                  classNames(
                    'cursor-pointer rounded-md px-3 py-2 hover:bg-slate-800',
                    node.id === value && 'bg-slate-800'
                  )
                )}
                onClick={() => {
                  onSelect(node);
                  setSearch('');
                }}
              >
                <div className="font-semibold text-white">Node #{node.id}</div>
                <p className="text-xs text-slate-400">
                  deg:{node.degree} ({node.latitude.toFixed(4)}, {node.longitude.toFixed(4)})
                </p>
              </li>
            ))}
          </ul>
        ) : null}
        <div>
          <label className="block text-xs text-slate-400">Manual entry</label>
          <input
            type="number"
            value={manualValue}
            onChange={(event) => {
              setManualValue(event.target.value);
              const parsed = Number(event.target.value);
              if (!Number.isNaN(parsed)) {
                onSelect({ id: parsed, latitude: 0, longitude: 0, degree: 0 });
              }
            }}
            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white"
          />
        </div>
      </div>
    </div>
  );
}
