declare const process: {
  env: Record<string, string | undefined>;
};

const apiOrigin = (process.env.API_ORIGIN ?? "http://127.0.0.1:8080").replace(/\/+$/, "");

export const config = {
  buildCommand: "npm run build",
  outputDirectory: "dist",
  rewrites: [
    {
      source: "/api/:match*",
      destination: `${apiOrigin}/api/:match*`,
    },
    {
      source: "/((?!api/).*)",
      destination: "/index.html",
    },
  ],
};
