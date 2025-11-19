import dayjs from 'dayjs';
import { useRouteStore } from '../hooks/useRouteStore';

export default function RouteStats() {
  const current = useRouteStore((s) => s.current);
  if (!current) return null;
  const { result, diagnostics } = current.response;

  return (
    <div className="rounded-2xl bg-slate-900/80 p-4 shadow-panel">
      <dl className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <dt className="text-slate-400">Departure</dt>
          <dd className="text-lg font-semibold">
            {dayjs().startOf('day').add(result.departureTime, 'minute').format('HH:mm')}
          </dd>
        </div>
        <div>
          <dt className="text-slate-400">Score</dt>
          <dd className="text-lg font-semibold">{result.score.toFixed(1)}</dd>
        </div>
        <div>
          <dt className="text-slate-400">Right turns</dt>
          <dd className="text-lg font-semibold">{result.rightTurns}</dd>
        </div>
        <div>
          <dt className="text-slate-400">Runtime</dt>
          <dd className="text-lg font-semibold">{diagnostics.elapsedSeconds.toFixed(2)}s</dd>
        </div>
      </dl>
    </div>
  );
}
