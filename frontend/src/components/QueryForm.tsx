import { useMutation } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { runQuery } from '../lib/api';
import { useRouteStore } from '../hooks/useRouteStore';
import type { QueryPayload } from '../types';
import NodeSelect from './NodeSelect';

const defaultValues: QueryPayload = {
  source: 0,
  destination: 0,
  startDepartureMinutes: 7 * 60 + 30,
  budgetMinutes: 45,
  intervalDurationMinutes: 360
};

export default function QueryForm() {
  const { register, handleSubmit, setValue, watch, reset } = useForm<QueryPayload>({ defaultValues });
  const setCurrent = useRouteStore((s) => s.setCurrent);
  const clearCurrent = useRouteStore((s) => s.clearCurrent);
  const abortRef = useRef<AbortController | null>(null);

  const mutation = useMutation({
    mutationFn: async (values: QueryPayload) => {
      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;
      return runQuery(values, controller.signal);
    },
    onSuccess: (data, variables) => setCurrent(variables, data)
  });

  const onSubmit = (values: QueryPayload) => mutation.mutate(values);

  const handleNewQuery = () => {
    abortRef.current?.abort();
    clearCurrent();
    mutation.reset();
    reset(defaultValues);
  };

  useEffect(() => () => abortRef.current?.abort(), []);

  return (
    <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
      <NodeSelect
        label="Source node"
        value={watch('source')}
        onSelect={(node) => setValue('source', node.id)}
      />
      <NodeSelect
        label="Destination node"
        value={watch('destination')}
        onSelect={(node) => setValue('destination', node.id)}
      />
      <label className="block text-sm font-medium text-slate-200">
        Departure time (minutes from 00:00)
        <input
          type="number"
          min={0}
          max={24 * 60}
          step={5}
          {...register('startDepartureMinutes', { valueAsNumber: true })}
          className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white"
        />
      </label>
      <label className="block text-sm font-medium text-slate-200">
        Interval duration (minutes)
        <input
          type="number"
          min={1}
          max={24 * 60}
          step={1}
          {...register('intervalDurationMinutes', { valueAsNumber: true })}
          className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white"
        />
      </label>
      <label className="block text-sm font-medium text-slate-200">
        Budget (minutes)
        <input
          type="number"
          min={1}
          max={240}
          step={1}
          {...register('budgetMinutes', { valueAsNumber: true })}
          className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white"
        />
      </label>
      <div className="flex gap-3">
        <button
          type="submit"
          className="flex-1 rounded-lg bg-brand-500 py-2 font-semibold text-white transition hover:bg-brand-600 disabled:opacity-60"
          disabled={mutation.status === 'pending'}
        >
          {mutation.status === 'pending' ? 'Computing…' : 'Run Query'}
        </button>
        <button
          type="button"
          onClick={handleNewQuery}
          className="rounded-lg border border-slate-700 px-4 py-2 text-sm font-semibold text-white transition hover:border-slate-500"
        >
          New Query
        </button>
      </div>
      {mutation.isError ? (
        <p className="text-sm text-red-400">{(mutation.error as Error).message}</p>
      ) : null}
    </form>
  );
}
