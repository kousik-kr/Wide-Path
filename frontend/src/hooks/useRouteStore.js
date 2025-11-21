import { create } from 'zustand';
export const useRouteStore = create((set) => ({
    history: [],
    setCurrent: (payload, response) => set((state) => {
        const entry = { id: crypto.randomUUID(), payload, response };
        return {
            current: entry,
            history: [entry, ...state.history].slice(0, 20)
        };
    })
}));
