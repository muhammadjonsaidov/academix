import type { Metadata, Viewport } from "next";
import { Fraunces, Geist_Mono, Manrope } from "next/font/google";
import "./globals.css";

// Body/UI face — Manrope (variable font, covers the 400-700 weights the UI uses).
// Warmer/rounder than Inter, which sits better against Fraunces' serif headings on
// the warm paper palette. Var renamed --font-inter → --font-body to match reality;
// globals.css's @theme references updated in step.
const manrope = Manrope({
  variable: "--font-body",
  subsets: ["latin"],
});

// Heading display face — serif textbook feel, restrained to H1/H2. Feeds the
// --font-heading-override slot globals.css's --font-heading fallback chain
// already expects (see the block comment at the top of globals.css).
const fraunces = Fraunces({
  variable: "--font-heading-override",
  subsets: ["latin"],
  weight: ["600", "700"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

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
    { media: "(prefers-color-scheme: light)", color: "#faf7f0" },
    // Night-notebook background: color-mix(in oklch, var(--ink) 92%, black)
    // with --ink #1c2b3a resolves to ≈ #19262f — keep the chrome bar in step.
    { media: "(prefers-color-scheme: dark)", color: "#19262f" },
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
      className={`${manrope.variable} ${fraunces.variable} ${geistMono.variable} h-full antialiased`}
    >
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
