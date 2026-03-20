import { useQuery } from "@tanstack/react-query";
import { api } from "../../api/axios";
import { API } from "../../api/config";

export type TraceSpan = {
  service: string;
  name: string;
  offsetMs: number;
  durationMs: number;
  status: string;
};

export type TraceTimeline = {
  traceId: string;
  available: boolean;
  message: string | null;
  spans: TraceSpan[];
};

export function useTraceTimeline(traceId: string | null) {
  return useQuery({
    enabled: !!traceId,
    queryKey: ["trace-timeline", traceId],
    queryFn: async (): Promise<TraceTimeline> => {
      const { data } = await api.get(`${API.order}/traces/${traceId}`);
      return data;
    },
    retry: false,
    refetchInterval: (query) => {
      if (!traceId) {
        return false;
      }

      const data = query.state.data as TraceTimeline | undefined;
      const maxPolls = data?.available ? 10 : 6;

      return query.state.dataUpdateCount >= maxPolls ? false : 1500;
    },
  });
}
