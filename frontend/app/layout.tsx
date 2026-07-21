import type { Metadata } from "next";
import { Fraunces, Geist_Mono, Inter } from "next/font/google";
import "./globals.css";

// Body/UI grotesk — replaces Geist Sans.
const inter = Inter({
  variable: "--font-inter",
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

export const metadata: Metadata = {
  title: "AcademiX AI",
  description: "AI yordamida uy vazifalari va nazorat ishlarini baholovchi maktab platformasi",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${inter.variable} ${fraunces.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
