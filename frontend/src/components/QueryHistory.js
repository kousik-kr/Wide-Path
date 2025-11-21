import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useRouteStore } from '../hooks/useRouteStore';
export default function QueryHistory() {
    const history = useRouteStore((s) => s.history);
    if (history.length === 0) {
        return (_jsx("div", { className: "rounded-2xl bg-slate-900/60 p-4 text-sm text-slate-400 shadow-panel", children: "No queries yet." }));
    }
    return (_jsxs("div", { className: "rounded-2xl bg-slate-900/60 p-4 shadow-panel", children: [_jsx("h2", { className: "text-lg font-semibold text-white", children: "Recent queries" }), _jsx("ul", { className: "mt-4 space-y-3 text-sm text-slate-300", children: history.map((entry) => (_jsxs("li", { className: "rounded-lg border border-slate-800 p-3", children: [_jsxs("p", { className: "font-semibold text-slate-100", children: [entry.payload.source, " \u2192 ", entry.payload.destination] }), _jsxs("p", { className: "text-xs text-slate-500", children: ["depart ", entry.payload.startDepartureMinutes, "m \u00B7 budget ", entry.payload.budgetMinutes, "m"] }), _jsxs("p", { className: "text-xs text-slate-400", children: ["score ", entry.response.result.score.toFixed(1)] })] }, entry.id))) })] }));
}
