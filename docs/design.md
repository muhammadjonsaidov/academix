# AcademiX AI — design system

## Who it is for and what the screens must do

| Person | Where they use it | The one job the screen does |
|---|---|---|
| Teacher | Laptop in the staff room, phone between lessons | Get through the stack of checked work: look at what the AI flagged, confirm the rest, move on |
| Director / admin | Laptop, once a week | See that teachers use it and the AI budget holds; buyer of the product |
| Parent | Phone, from a Telegram notification | See the grade and the teacher's comment for their child, trust it |
| Student | Phone | See what is due, what came back, and why points were lost |

The product's promise is "teacher time and parent trust", so every screen is judged by two questions:
does it save the teacher a click, and does it make a grade easier to trust.

## Concept: pencil and red pen on a graph-paper notebook

Uzbek schoolwork is done in *katak daftar*, the squared notebook, and graded on the 5-point scale,
written by the teacher in red pen. The interface borrows exactly that, and gives each mark a meaning:

- **The AI writes in pencil.** Its analysis (step checks, spelling corrections, the suggested score)
  is drawn in the pencil colour: provisional, something the teacher can rub out.
- **The teacher writes in red pen.** Only a grade the teacher confirmed is shown as the red, circled
  numeral. This is the product rule "the teacher's grade is final", made visible.
- **The highlighter marks what needs a human look.** Work the AI flagged as copied or machine-written
  gets a yellow highlighter swipe in the review queue, and nothing else on the screen is yellow.

The memorable element is the circled red grade. It appears on the student's results, in the parent's
view and on the landing page, always for a confirmed grade and nowhere else.

## Colour

| Token | Light | Dark | Use |
|---|---|---|---|
| `--paper` | `#F6F8FB` | `#111C2E` | Page. Cool notebook white, not cream |
| `--ink` | `#16284A` | `#E6ECF5` | Text, primary buttons |
| `--katak` | `#CAD8EC` | `#2A3A55` | Grid lines, borders, dividers |
| `--pencil` | `#5A6A84` | `#9AAAC2` | AI marks, secondary text |
| `--pen-red` | `#C8233A` | `#FF6B7A` | Confirmed grades, errors. Never decoration |
| `--chalk-green` | `#127A57` | `#4CC79A` | Correct steps, "graded" |
| `--marker` | `#FFE14D` | `#E8C547` | Highlighter: flagged work only |

Dark mode is "doska", the classroom board: deep blue-slate, not a tinted black.

The previous indigo→violet "AI gradient" is gone. The AI does not get a decorative colour; it gets
the pencil, because that is its role in the product.

## Type

- **Onest** for everything: UI, headings, numbers. It was drawn for Latin and Cyrillic interfaces,
  handles Uzbek `o'`/`g'` cleanly, and keeps one voice across the app. Headings use weight 650–700
  with slightly tight tracking; body is 400 at 15–16px, line length under 75 characters.
- **Caveat** only for the teacher's hand: the circled grade numeral and red-pen margin notes on the
  landing page. Never for labels, buttons or body text.
- Tabular figures (`font-variant-numeric: tabular-nums`) for scores, counts and dates.
- Sentence case everywhere; no all-caps labels, no eyebrow text above headings.

## Layout

Teacher home is a to-do list, not a KPI wall:

```
┌ sidebar ┐┌──────────────────────────────────────────────────────────┐
│         ││ Salom, Gulnora                                           │
│ Bosh    ││ 14 ta ish sizni kutmoqda, 3 tasi diqqat talab qiladi.    │
│ Ishlar  ││                                                          │
│ Vazifa  ││ Tekshirish navbati                                       │
│ Sinflar ││ ┌──────────────────────────────────────────────────────┐ │
│         ││ │ Kasrlarni qo'shish · 7-A        9 ta  [▓3 shubhali▓]  │ │
│         ││ │ [Tekshirishni boshlash] [6 ta toza ishni tasdiqlash] │ │
│         ││ ├──────────────────────────────────────────────────────┤ │
│         ││ │ Insho: Mening oilam · 6-B       5 ta                 │ │
│         ││ └──────────────────────────────────────────────────────┘ │
│         ││ Bu hafta          Sinflar o'zlashtirishi                  │
└─────────┘└──────────────────────────────────────────────────────────┘
```

- Content is left-aligned; one column on phones, the queue keeps the widest column on desktop.
- Lists are lists (rows with dividers), not a grid of identical cards. Cards are kept for things that
  really are separate objects (a child on the parent page, a class).
- Radii: 6px for inputs and buttons, 10px for panels. Borders over shadows; one soft shadow only for
  floating things (menus, dialogs).
- The katak grid texture appears in exactly two places: the landing hero's notebook and empty states.

## Motion

One orchestrated moment: on the landing page the AI's pencil marks appear on the notebook, then the
teacher's red circle is drawn. Elsewhere motion only answers an action (dialog open, row approved).
`prefers-reduced-motion` shows the final state.

## Copy

Plain Uzbek (Latin), from the user's side: "Tekshirishni boshlash", not "Ko'rish"; "6 ta toza ishni
tasdiqlash", and the toast says "6 ta ish baholandi". Errors say what happened and what to do. Dates
are written the Uzbek way: `19-noyabr, 16:00`, never `11/19/2026, 4:00:00 PM`.

## Checking modes (shown in the UI)

| Subject | Mode | What the analysis rows are called |
|---|---|---|
| Matematika, fizika, kimyo | Masala yechish | Qadamlar (steps) |
| Ona tili, rus tili, ingliz tili | Til va yozuv | Xatolar va to'g'risi (error → correction) |
| Tarix, biologiya, geografiya, adabiyot, boshqa | Yozma javob | Asosiy fikrlar (claims) |
