import type { Metadata, Viewport } from "next";
import "./globals.css";

// Fonts are self-hosted from public/fonts (@font-face rules in globals.css), so neither CI nor
// the production Docker build depends on reaching Google Fonts. See docs/design.md.

// The site's canonical origin. Locally it's localhost; on Vercel set
// NEXT_PUBLIC_SITE_URL=https://academix.vercel.app so OG/canonical URLs point at the
// public domain, not localhost.
const siteUrl =
  process.env.NEXT_PUBLIC_SITE_URL?.replace(/\/$/, "") ?? "http://localhost:3000";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: "AcademiX AI — AI yordamida qo'lyozmani baholash platformasi",
    template: "%s · AcademiX AI",
  },
  description:
    "AcademiX AI — maktablar, xususiy maktablar va o'quv markazlari uchun AI baholash platformasi. Daftar suratini AI o'qiydi, bosqichma-bosqich tekshiradi, ko'chirishni aniqlaydi va baho taklif qiladi. O'qituvchi, o'quvchi, ota-ona, rahbariyat va psixolog uchun yagona platforma.",
  keywords: [
    "AI baholash",
    "qo'lyozmani o'qish",
    "uy vazifalarini tekshirish",
    "maktab platformasi",
    "O'zbekiston maktablari",
    "AI ustoz",
    "nazorat ishlari",
    "dasturiy ta'lim",
    "AcademiX",
  ],
  authors: [{ name: "AcademiX AI" }],
  applicationName: "AcademiX AI",
  category: "education",
  alternates: {
    canonical: "/",
  },
  openGraph: {
    type: "website",
    locale: "uz_UZ",
    url: "/",
    siteName: "AcademiX AI",
    title: "AcademiX AI — AI yordamida qo'lyozmani baholash platformasi",
    description:
      "Daftar suratini AI o'qiydi, bosqichma-bosqich tekshiradi va baho taklif qiladi. O'qituvchi, o'quvchi, ota-ona, rahbariyat va psixolog uchun yagona platforma.",
    images: [
      {
        url: "/og-image.png",
        width: 1200,
        height: 630,
        alt: "AcademiX AI — qo'lyozmani AI bilan baholash",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "AcademiX AI — AI yordamida qo'lyozmani baholash",
    description:
      "Daftar suratini AI o'qiydi, bosqichma-bosqich tekshiradi va baho taklif qiladi.",
    images: ["/og-image.png"],
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-image-preview": "large",
      "max-snippet": -1,
    },
  },
  icons: {
    icon: "/logo-mark.svg",
    shortcut: "/logo-mark.svg",
    apple: "/logo-mark.svg",
  },
  manifest: "/manifest.webmanifest",
  appleWebApp: {
    capable: true,
    title: "AcademiX AI",
    statusBarStyle: "black-translucent",
  },
  formatDetection: {
    telephone: false,
  },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f6f8fb" },
    { media: "(prefers-color-scheme: dark)", color: "#111c2e" },
  ],
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="uz"
      // The pre-paint theme script below adds `dark` to this element before hydration —
      // an expected, deliberate server/client difference (standard theming pattern).
      suppressHydrationWarning
      className="h-full antialiased"
    >
      <head>
        <link
          rel="preload"
          href="/fonts/onest-latin-wght-normal.woff2"
          as="font"
          type="font/woff2"
          crossOrigin="anonymous"
        />
      </head>
      <body className="min-h-full flex flex-col">
        {/* Apply the saved theme before first paint — avoids a light-mode flash for dark-mode
            users on every reload. Reads the same key ThemeToggle writes. */}
        <script
          dangerouslySetInnerHTML={{
            __html:
              "try{if(localStorage.getItem('academix-theme')==='dark'){document.documentElement.classList.add('dark')}}catch(e){}",
          }}
        />
        {/* Structured data — schema.org SoftwareApplication, so search engines can surface
            the product name, description and rating-rich snippets. */}
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{
            __html: JSON.stringify({
              "@context": "https://schema.org",
              "@type": "SoftwareApplication",
              name: "AcademiX AI",
              applicationCategory: "EducationalApplication",
              operatingSystem: "Web",
              inLanguage: "uz",
              description:
                "AI yordamida qo'lyozma uy vazifalari va nazorat ishlarini baholovchi maktab platformasi. Suratdagi yozuvni o'qiydi, bosqichma-bosqich tekshiradi va baho taklif qiladi.",
              offers: {
                "@type": "Offer",
                price: "0",
                priceCurrency: "UZS",
              },
              url: siteUrl,
            }),
          }}
        />
        {children}
      </body>
    </html>
  );
}
