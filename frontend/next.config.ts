import type { NextConfig } from "next";

const backend =
  process.env.BACKEND_URL?.replace(/\/$/, "") || "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/proxy/:path*",
        destination: `${backend}/:path*`,
      },
    ];
  },
};

export default nextConfig;
