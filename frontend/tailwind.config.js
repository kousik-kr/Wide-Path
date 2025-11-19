import { defineConfig } from 'tailwindcss';

export default defineConfig({
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f0f6ff',
          500: '#2563eb',
          600: '#1d4ed8',
          900: '#0f172a'
        }
      },
      boxShadow: {
        panel: '0 10px 40px rgba(15, 23, 42, 0.12)'
      }
    }
  },
  plugins: []
});
