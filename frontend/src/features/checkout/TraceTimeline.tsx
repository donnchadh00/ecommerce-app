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
    <div className="space-y-3 rounded-2xl border bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold">Order trace</h2>
          <p className="text-sm text-gray-600">Timeline relative to the first captured span.</p>
        </div>
        <span className="rounded-full bg-gray-100 px-3 py-1 font-mono text-xs text-gray-700">
          {timeline.traceId}
        </span>
      </div>

      <div className="space-y-3">
        {timeline.spans.map((span, index) => {
          const left = (span.offsetMs / maxEnd) * 100;
          const width = Math.max((span.durationMs / maxEnd) * 100, 3);
          const color = SERVICE_COLORS[span.service] ?? "bg-gray-700";

          return (
            <div key={`${span.service}-${span.name}-${index}`} className="grid grid-cols-[140px_1fr] items-center gap-3">
              <div className="text-sm">
                <div className="font-medium text-gray-900">{span.service}</div>
                <div className="text-xs text-gray-500">{span.status.replace("STATUS_CODE_", "")}</div>
              </div>
              <div className="space-y-1">
                <div className="flex justify-between text-xs text-gray-500">
                  <span className="truncate pr-4">{span.name}</span>
                  <span>{span.durationMs} ms</span>
                </div>
                <div className="relative h-8 rounded-lg bg-gray-100">
                  <div
                    className={`absolute top-1.5 h-5 rounded-md ${color}`}
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
