declare const process: {
  env: Record<string, string | undefined>;
};

type VercelConfig = {
  buildCommand: string;
  outputDirectory: string;
  rewrites: Array<{
    source: string;
    destination: string;
  }>;
};

const configuredApiOrigin = process.env.API_ORIGIN?.replace(/\/+$/, "");

if (!configuredApiOrigin && process.env.VERCEL) {
  throw new Error("API_ORIGIN must be set in the Vercel project environment variables.");
}

const apiOrigin = configuredApiOrigin ?? "http://127.0.0.1:8080";

export const config: VercelConfig = {
  buildCommand: "npm run build",
  outputDirectory: "dist",
  rewrites: [
    {
      source: "/api/:match*",
      destination: `${apiOrigin}/api/:match*`,
    },
    {
      source: "/(.*)",
      destination: "/index.html",
    },
  ],
};
