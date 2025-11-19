import { useRouteStore } from '../hooks/useRouteStore';

export default function QueryHistory() {
  const history = useRouteStore((s) => s.history);
  if (history.length === 0) {
    return (
      <div className="rounded-2xl bg-slate-900/60 p-4 text-sm text-slate-400 shadow-panel">
        No queries yet.
      </div>
    );
  }

  return (
    <div className="rounded-2xl bg-slate-900/60 p-4 shadow-panel">
      <h2 className="text-lg font-semibold text-white">Recent queries</h2>
      <ul className="mt-4 space-y-3 text-sm text-slate-300">
        {history.map((entry) => (
          <li key={entry.id} className="rounded-lg border border-slate-800 p-3">
            <p className="font-semibold text-slate-100">
              {entry.payload.source} → {entry.payload.destination}
            </p>
            <p className="text-xs text-slate-500">
              depart {entry.payload.startDepartureMinutes}m · budget {entry.payload.budgetMinutes}m
            </p>
            <p className="text-xs text-slate-400">score {entry.response.result.score.toFixed(1)}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}
