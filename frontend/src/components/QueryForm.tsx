import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { runQuery } from '../lib/api';
import { useRouteStore } from '../hooks/useRouteStore';
import type { QueryPayload } from '../types';
import NodeSelect from './NodeSelect';

const defaultValues: QueryPayload = {
  source: 0,
  destination: 0,
  startDepartureMinutes: 7 * 60 + 30,
  budgetMinutes: 45
};

export default function QueryForm() {
  const { register, handleSubmit, setValue, watch } = useForm<QueryPayload>({ defaultValues });
  const setCurrent = useRouteStore((s) => s.setCurrent);

  const mutation = useMutation({
    mutationFn: runQuery,
    onSuccess: (data, variables) => setCurrent(variables, data)
  });

  const onSubmit = (values: QueryPayload) => mutation.mutate(values);

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
      <button
        type="submit"
        className="w-full rounded-lg bg-brand-500 py-2 font-semibold text-white transition hover:bg-brand-600 disabled:opacity-60"
        disabled={mutation.isLoading}
      >
        {mutation.isLoading ? 'Computing…' : 'Run Query'}
      </button>
      {mutation.isError ? (
        <p className="text-sm text-red-400">{(mutation.error as Error).message}</p>
      ) : null}
    </form>
  );
}
