import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        background: "var(--background)",
        foreground: "var(--foreground)",
        panel: "var(--panel)",
        muted: "var(--muted)",
        line: "var(--line)",
        accent: "var(--accent)",
        "accent-strong": "var(--accent-strong)",
        success: "var(--success)",
        danger: "var(--danger)",
      },
      fontFamily: {
        sans: ['"Geist"', '"Geist Fallback"', "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ['"Geist Mono"', '"Geist Mono Fallback"', "ui-monospace", "monospace"],
      },
    },
  },
  plugins: [],
};

export default config;
