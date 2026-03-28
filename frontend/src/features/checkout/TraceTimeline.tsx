import type { TraceTimeline as TraceTimelineData } from "./traceApi";

type Props = {
  timeline: TraceTimelineData;
};

const SERVICE_COLORS: Record<string, string> = {
  "order-service": "bg-slate-900",
  "inventory-service": "bg-emerald-600",
  "payment-service": "bg-amber-500",
  "cart-service": "bg-sky-600",
  "product-service": "bg-violet-600",
  "auth-service": "bg-rose-600",
};

export default function TraceTimeline({ timeline }: Props) {
  const maxEnd = Math.max(...timeline.spans.map((span) => span.offsetMs + span.durationMs), 1);

  return (
    <div className="surface overflow-hidden p-5 sm:p-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="section-heading">Order trace</h2>
          <p className="mt-1 text-sm text-slate-600">Timeline relative to the first captured span.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <span className="chip border-slate-200 bg-slate-50 text-slate-600 shadow-none">
            {timeline.spans.length} spans
          </span>
          <span className="rounded-full bg-slate-100 px-3 py-1 font-mono text-xs text-slate-700">
            {timeline.traceId}
          </span>
        </div>
      </div>

      <div className="mt-6 space-y-3">
        {timeline.spans.map((span, index) => {
          const left = (span.offsetMs / maxEnd) * 100;
          const width = Math.max((span.durationMs / maxEnd) * 100, 3);
          const color = SERVICE_COLORS[span.service] ?? "bg-gray-700";

          return (
            <div
              key={`${span.service}-${span.name}-${index}`}
              className="rounded-2xl bg-slate-50 p-3 sm:grid sm:grid-cols-[160px_minmax(0,1fr)] sm:items-center sm:gap-4"
            >
              <div className="mb-3 text-sm sm:mb-0">
                <div className="font-medium text-slate-900">{span.service}</div>
                <div className="mt-1 text-xs text-slate-500">
                  {span.status.replace("STATUS_CODE_", "")} • {span.durationMs} ms
                </div>
              </div>
              <div className="space-y-2">
                <div className="flex items-center justify-between gap-3 text-xs text-slate-500">
                  <span className="truncate pr-4 font-medium text-slate-700">{span.name}</span>
                  <span>{span.offsetMs} ms</span>
                </div>
                <div className="relative h-9 overflow-hidden rounded-xl bg-white shadow-inner shadow-slate-200/50">
                  <div
                    className={`absolute top-2 h-5 rounded-lg ${color}`}
                    style={{ left: `${left}%`, width: `${width}%` }}
                    title={`${span.service}: ${span.name}`}
                  />
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
