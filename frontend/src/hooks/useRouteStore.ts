import { create } from 'zustand';
import type { QueryPayload, QueryResponse } from '../types';

type RouteState = {
  current?: { payload: QueryPayload; response: QueryResponse };
  history: Array<{ id: string; payload: QueryPayload; response: QueryResponse }>;
  setCurrent: (payload: QueryPayload, response: QueryResponse) => void;
};

export const useRouteStore = create<RouteState>((set) => ({
  history: [],
  setCurrent: (payload, response) =>
    set((state) => {
      const entry = { id: crypto.randomUUID(), payload, response };
      return {
        current: entry,
        history: [entry, ...state.history].slice(0, 20)
      };
    })
}));
