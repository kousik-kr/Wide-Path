import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { runQuery } from '../lib/api';
import { useRouteStore } from '../hooks/useRouteStore';
import NodeSelect from './NodeSelect';
const defaultValues = {
    source: 0,
    destination: 0,
    startDepartureMinutes: 7 * 60 + 30,
    budgetMinutes: 45
};
export default function QueryForm() {
    const { register, handleSubmit, setValue, watch } = useForm({ defaultValues });
    const setCurrent = useRouteStore((s) => s.setCurrent);
    const mutation = useMutation({
        mutationFn: runQuery,
        onSuccess: (data, variables) => setCurrent(variables, data)
    });
    const onSubmit = (values) => mutation.mutate(values);
    return (_jsxs("form", { className: "space-y-4", onSubmit: handleSubmit(onSubmit), children: [_jsx(NodeSelect, { label: "Source node", value: watch('source'), onSelect: (node) => setValue('source', node.id) }), _jsx(NodeSelect, { label: "Destination node", value: watch('destination'), onSelect: (node) => setValue('destination', node.id) }), _jsxs("label", { className: "block text-sm font-medium text-slate-200", children: ["Departure time (minutes from 00:00)", _jsx("input", { type: "number", min: 0, max: 24 * 60, step: 5, ...register('startDepartureMinutes', { valueAsNumber: true }), className: "mt-2 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white" })] }), _jsxs("label", { className: "block text-sm font-medium text-slate-200", children: ["Budget (minutes)", _jsx("input", { type: "number", min: 1, max: 240, step: 1, ...register('budgetMinutes', { valueAsNumber: true }), className: "mt-2 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white" })] }), _jsx("button", { type: "submit", className: "w-full rounded-lg bg-brand-500 py-2 font-semibold text-white transition hover:bg-brand-600 disabled:opacity-60", disabled: mutation.isLoading, children: mutation.isLoading ? 'Computing…' : 'Run Query' }), mutation.isError ? (_jsx("p", { className: "text-sm text-red-400", children: mutation.error.message })) : null] }));
}
