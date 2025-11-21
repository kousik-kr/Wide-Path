import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import dayjs from 'dayjs';
import { useRouteStore } from '../hooks/useRouteStore';
export default function RouteStats() {
    const current = useRouteStore((s) => s.current);
    if (!current)
        return null;
    const { result, diagnostics } = current.response;
    return (_jsx("div", { className: "rounded-2xl bg-slate-900/80 p-4 shadow-panel", children: _jsxs("dl", { className: "grid grid-cols-2 gap-4 text-sm", children: [_jsxs("div", { children: [_jsx("dt", { className: "text-slate-400", children: "Departure" }), _jsx("dd", { className: "text-lg font-semibold", children: dayjs().startOf('day').add(result.departureTime, 'minute').format('HH:mm') })] }), _jsxs("div", { children: [_jsx("dt", { className: "text-slate-400", children: "Score" }), _jsx("dd", { className: "text-lg font-semibold", children: result.score.toFixed(1) })] }), _jsxs("div", { children: [_jsx("dt", { className: "text-slate-400", children: "Right turns" }), _jsx("dd", { className: "text-lg font-semibold", children: result.rightTurns })] }), _jsxs("div", { children: [_jsx("dt", { className: "text-slate-400", children: "Runtime" }), _jsxs("dd", { className: "text-lg font-semibold", children: [diagnostics.elapsedSeconds.toFixed(2), "s"] })] })] }) }));
}
