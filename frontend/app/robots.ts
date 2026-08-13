import type { MetadataRoute } from "next";

// robots.txt — allow everything, point at the sitemap. The site is public marketing
// + authenticated dashboards; dashboards are already gated by auth, so no extra
// disallow rules are needed here.
export default function robots(): MetadataRoute.Robots {
  const siteUrl =
    process.env.NEXT_PUBLIC_SITE_URL?.replace(/\/$/, "") ?? "http://localhost:3000";
  return {
    rules: {
      userAgent: "*",
      allow: "/",
    },
    sitemap: `${siteUrl}/sitemap.xml`,
  };
}
