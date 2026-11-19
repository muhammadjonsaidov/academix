import Image from "next/image";
import Link from "next/link";
import { HomeAuthRedirect } from "@/components/landing/HomeAuthRedirect";
import { NotebookDemoLoader } from "@/components/landing/NotebookDemoLoader";
import { GradeMark } from "@/components/shared/GradeMark";
import { buttonVariants } from "@/components/ui/button";

// Real sequence, so it is numbered.
const STEPS = [
  {
    title: "Surat",
    text: "O'quvchi yoki o'qituvchi daftar varag'ini telefonda suratga oladi. Matnli javob ham qabul qilinadi.",
  },
  {
    title: "AI tekshiradi",
    text: "Bir-ikki daqiqada qo'lyozma o'qiladi, har bir qadam yoki xato belgilanadi, ko'chirilgan ish ajratiladi.",
  },
  {
    title: "O'qituvchi tasdiqlaydi",
    text: "Shubhali ishlar navbat boshida turadi. Toza ishlar bitta tugma bilan tasdiqlanadi, kerak bo'lsa baho o'zgartiriladi.",
  },
  {
    title: "Ota-ona ko'radi",
    text: "Baho va o'qituvchi izohi Telegram'ga keladi. Muddat yaqinlashsa, o'quvchiga eslatma boradi.",
  },
];

const MODES = [
  {
    title: "Masala yechish",
    subjects: "Matematika, fizika, kimyo",
    text: "Yechimning har bir qadami: usul, hisob va yakuniy javob alohida tekshiriladi.",
    example: (
      <ul className="space-y-1 font-data text-sm">
        <li>5x = 18 − 3 <span className="text-success">✓</span></li>
        <li>5x = 15 <span className="text-success">✓</span></li>
        <li>
          <span className="text-pen-red line-through">x = 5</span>{" "}
          <span className="text-success">x = 3</span>
        </li>
      </ul>
    ),
  },
  {
    title: "Til va yozuv",
    subjects: "Ona tili, rus tili, ingliz tili",
    text: "Imlo, grammatika va tinish belgilari: har bir xato va uning to'g'ri yozilishi ko'rsatiladi.",
    example: (
      <p className="text-sm">
        Biz kecha <span className="text-pen-red line-through">maktapda</span>{" "}
        <span className="text-success">maktabda</span>{" "}bayram o&apos;tkazdik.
      </p>
    ),
  },
  {
    title: "Yozma javob",
    subjects: "Tarix, biologiya, geografiya, adabiyot",
    text: "Faktlar to'g'riligi, savolga to'liq javob va fikrning dalil bilan asoslangani baholanadi.",
    example: (
      <p className="text-sm">
        «Poytaxt Samarqand edi» <span className="text-success">✓</span>
        <br />
        <span className="text-pencil">«U ilm-fanga homiylik qildi» — misol keltirilmagan</span>
      </p>
    ),
  },
];

const PLANS = [
  { name: "Starter", students: "150 o'quvchigacha", calls: "4 000 tekshiruv", price: "1 800 000" },
  { name: "Standard", students: "400 o'quvchigacha", calls: "12 000 tekshiruv", price: "4 000 000", note: "ko'pchilik maktablar uchun" },
  { name: "Pro", students: "900 o'quvchigacha", calls: "30 000 tekshiruv", price: "7 200 000" },
];

const PROMISES = [
  {
    title: "Yakuniy bahoni o'qituvchi qo'yadi",
    text: "AI faqat taklif beradi va tahlilni ko'rsatadi. O'quvchi va ota-ona o'qituvchi tasdiqlagan bahoni ko'radi.",
  },
  {
    title: "Ish hech qachon yo'qolmaydi",
    text: "Oylik AI limiti tugasa ham ishlar qabul qilinadi, o'qituvchi ularni qo'lda baholaydi. Direktor 80% da ogohlantiriladi.",
  },
  {
    title: "Har maktab ma'lumoti alohida",
    text: "Ma'lumotlar bazasi darajasida ajratilgan: bir maktab boshqasining o'quvchilarini ko'ra olmaydi.",
  },
];

// Server-rendered; only the notebook demo hydrates.
export default function HomePage() {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <HomeAuthRedirect />
      <header className="sticky top-0 z-40 border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <Link href="/" className="flex items-center gap-2.5" aria-label="AcademiX AI — bosh sahifa">
            <Image src="/logo-mark.svg" alt="" width={30} height={27} priority />
            <span className="text-base font-bold tracking-tight">AcademiX AI</span>
          </Link>
          <nav aria-label="Sahifa bo'limlari" className="hidden items-center gap-6 text-sm text-muted-foreground md:flex">
            <a href="#qanday-ishlaydi" className="hover:text-foreground">Qanday ishlaydi</a>
            <a href="#fanlar" className="hover:text-foreground">Fanlar</a>
            <a href="#narxlar" className="hover:text-foreground">Narxlar</a>
            <a href="#ota-onalar" className="hover:text-foreground">Ota-onalar uchun</a>
          </nav>
          <Link href="/login" className={buttonVariants({ variant: "outline" })}>
            Kirish
          </Link>
        </div>
      </header>

      <main className="flex-1">
        <section className="mx-auto grid max-w-6xl items-center gap-12 px-4 pt-14 pb-20 sm:px-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,30rem)] lg:pt-20">
          <div className="max-w-xl space-y-6">
            <h1 className="text-[2.4rem] leading-[1.08] font-bold tracking-[-0.03em] text-balance sm:text-5xl">
              Daftar tekshirishga ketadigan soatlarni o&apos;qituvchiga qaytaramiz
            </h1>
            <p className="text-lg leading-relaxed text-muted-foreground">
              O&apos;quvchi ishini suratga oling: AI har bir qadamni tekshiradi, ko&apos;chirilgan ishni belgilaydi,
              yakuniy bahoni esa o&apos;qituvchi qo&apos;yadi. Ota-ona natijani Telegram&apos;da ko&apos;radi.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link href="/login" className={buttonVariants({ size: "lg" })}>
                Tizimga kirish
              </Link>
              <a href="#narxlar" className={buttonVariants({ variant: "outline", size: "lg" })}>
                Maktab uchun narxlar
              </a>
            </div>
            <p className="text-sm text-muted-foreground">
              Matematika, ona tili, ingliz tili, tarix va boshqa fanlar. O&apos;zbek, rus va ingliz tilidagi ishlar.
            </p>
          </div>
          <div className="flex justify-center lg:justify-end">
            <NotebookDemoLoader />
          </div>
        </section>

        <section id="qanday-ishlaydi" className="border-y border-border bg-card">
          <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
            <h2 className="mb-10 text-3xl font-bold tracking-tight">Qanday ishlaydi</h2>
            <ol className="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
              {STEPS.map((step, i) => (
                <li key={step.title} className="space-y-2">
                  <span className="font-data text-sm font-semibold text-muted-foreground">{i + 1}</span>
                  <h3 className="text-lg font-semibold">{step.title}</h3>
                  <p className="text-[0.9375rem] leading-relaxed text-muted-foreground">{step.text}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section id="fanlar" className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
          <div className="mb-10 max-w-2xl space-y-3">
            <h2 className="text-3xl font-bold tracking-tight">Har bir fan o&apos;z usulida tekshiriladi</h2>
            <p className="text-muted-foreground">
              Matematikadagi qadam, diktantdagi imlo xatosi va tarixdagi fakt — uch xil narsa. AI fanga qarab
              tekshirish usulini o&apos;zi tanlaydi; o&apos;qituvchi esa o&apos;z baholash mezonlarini qo&apos;yishi mumkin.
            </p>
          </div>
          <div className="grid gap-px overflow-hidden rounded-[10px] border border-border bg-border md:grid-cols-3">
            {MODES.map((mode) => (
              <article key={mode.title} className="flex flex-col gap-3 bg-card p-6">
                <div>
                  <h3 className="text-lg font-semibold">{mode.title}</h3>
                  <p className="text-sm text-muted-foreground">{mode.subjects}</p>
                </div>
                <p className="text-[0.9375rem] leading-relaxed">{mode.text}</p>
                <div className="bg-grid-paper mt-auto rounded-md border border-border px-4 py-3">{mode.example}</div>
              </article>
            ))}
          </div>
        </section>

        <section id="narxlar" className="border-y border-border bg-card">
          <div className="mx-auto grid max-w-6xl gap-12 px-4 py-16 sm:px-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
            <div className="space-y-4">
              <h2 className="text-3xl font-bold tracking-tight">Maktab uchun hisob-kitob</h2>
              <p className="text-[0.9375rem] leading-relaxed text-muted-foreground">
                O&apos;qituvchi haftasiga 6–8 soat daftar tekshiradi. AcademiX bilan bu vaqt 40–60% qisqaradi —
                har bir o&apos;qituvchiga oyiga 12–18 soat.
              </p>
              <p className="text-[0.9375rem] leading-relaxed text-muted-foreground">
                Pilot: bir chorak, ikki sinf, bitta fan — 50% chegirma bilan. Natijani oldindan kelishilgan
                ko&apos;rsatkichlar bo&apos;yicha birga o&apos;lchaymiz.
              </p>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[30rem] text-left">
                <caption className="sr-only">Oylik tariflar</caption>
                <thead className="text-sm text-muted-foreground">
                  <tr className="border-b border-border">
                    <th scope="col" className="py-2 font-medium">Tarif</th>
                    <th scope="col" className="py-2 font-medium">Kimga</th>
                    <th scope="col" className="py-2 text-right font-medium">Oyiga</th>
                  </tr>
                </thead>
                <tbody>
                  {PLANS.map((plan) => (
                    <tr key={plan.name} className="border-b border-border">
                      <th scope="row" className="py-4 pr-4 align-top">
                        <span className="block font-semibold">{plan.name}</span>
                        {plan.note ? <span className="text-sm font-normal text-muted-foreground">{plan.note}</span> : null}
                      </th>
                      <td className="py-4 pr-4 align-top text-[0.9375rem]">
                        {plan.students}
                        <span className="block text-sm text-muted-foreground">{plan.calls} oyiga</span>
                      </td>
                      <td className="py-4 text-right align-top">
                        <span className="font-data text-lg font-semibold whitespace-nowrap">{plan.price}</span>
                        <span className="block text-sm text-muted-foreground">so&apos;m</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <p className="mt-3 text-sm text-muted-foreground">
                900 dan ortiq o&apos;quvchi bo&apos;lsa, narx kelishiladi. Yillik to&apos;lovda 12% chegirma.
              </p>
            </div>
          </div>
        </section>

        <section id="ota-onalar" className="mx-auto grid max-w-6xl items-center gap-12 px-4 py-16 sm:px-6 md:grid-cols-2">
          <div className="space-y-4">
            <h2 className="text-3xl font-bold tracking-tight">Ota-ona baho ortidagi sababni ko&apos;radi</h2>
            <p className="text-[0.9375rem] leading-relaxed text-muted-foreground">
              Faqat raqam emas: qaysi qadamda xato qilingani va o&apos;qituvchi nima degani. Telegram&apos;ga ulangan
              ota-ona har bir yangi bahoni darhol oladi, farzandining fanlar bo&apos;yicha o&apos;sishini kuzatadi.
            </p>
          </div>
          <div className="mx-auto w-full max-w-sm rounded-[10px] border border-border bg-card p-4">
            <p className="mb-3 text-sm font-semibold">AcademiX AI</p>
            <div className="space-y-2 rounded-md bg-muted p-3 text-[0.9375rem]">
              <p className="font-medium">Ish baholandi</p>
              <p>
                Aziz Rahimov: «Kasrlarni qo&apos;shish» — 88 ball, baho: 5.
              </p>
              <p className="text-sm text-muted-foreground">O&apos;qituvchi: «Barakalla, 3-misolda ham ishorani tekshir.»</p>
            </div>
            <div className="mt-3 flex items-center gap-3">
              <GradeMark grade={5} size="sm" />
              <span className="text-sm text-muted-foreground">O&apos;qituvchi tasdiqlagan baho</span>
            </div>
          </div>
        </section>

        <section className="border-t border-border bg-card">
          <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
            <h2 className="mb-8 text-3xl font-bold tracking-tight">Ishonch qayerdan</h2>
            <div className="grid gap-8 md:grid-cols-3">
              {PROMISES.map((promise) => (
                <div key={promise.title} className="space-y-2">
                  <h3 className="text-lg font-semibold">{promise.title}</h3>
                  <p className="text-[0.9375rem] leading-relaxed text-muted-foreground">{promise.text}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-border py-6 text-sm text-muted-foreground">
        <div className="mx-auto flex max-w-6xl flex-col items-start justify-between gap-2 px-4 sm:flex-row sm:items-center sm:px-6">
          <span className="flex items-center gap-2">
            <Image src="/logo-mark.svg" alt="" width={20} height={18} />
            AcademiX AI
          </span>
          <span>Maktablar uchun daftar tekshirish yordamchisi</span>
        </div>
      </footer>
    </div>
  );
}
