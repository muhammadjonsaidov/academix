# AcademiX AI — Ishga tushirish qo'llanmasi (RUNBOOK)

Loyihani ishga tushirish, to'xtatish va **cloudflared** tunneli orqali tashqaridan ochish
bo'yicha qisqa qo'llanma. Barcha buyruqlar **`infra/`** papkasidan bajariladi.

---

## 1. Birinchi marta sozlash (faqat 1 marta)

```bash
cd infra
cp .env.example .env
```

Keyin `infra/.env` ni ochib, kamida quyidagilarni o'rnating:

| O'zgaruvchi | Nima uchun |
|---|---|
| `JWT_SECRET` | Majburiy — `openssl rand -hex 32` bilan generatsiya qiling |
| `QWEN_API_KEY` | AI baholash/chat uchun (bo'lmasa ham ishlaydi, AI ishlamaydi) |
| `GOOGLE_VISION_API_KEY` | OCR + qo'lyozma tekshirish uchun |
| `TELEGRAM_BOT_TOKEN` | Telegram bot uchun (bo'lmasa bot polling o'tkazmaydi) |

> **Muhim:** `NEXT_PUBLIC_API_URL=/api/v1` shaklida **nisbiy** qolsin — bu tunneldan ham,
> `localhost:3000` dan ham ishlashini ta'minlaydi (frontend `/api/*` ni backendga o'zi
> yo'naltiradi). Uni `http://localhost:8080/...` qilib yozsangiz tunnel buziladi.

---

## 2. Loyihani ishga tushirish

```bash
cd infra
docker compose up -d --build
```

Bu hammasini ishga tushiradi: Postgres, Redis, RabbitMQ, SeaweedFS, backend (8080),
frontend (3000), telegram-bot va demo-ma'lumotlarni seed qiluvchi bir martalik `seed` xizmati.

Ochish: **http://localhost:3000** (login sahifasi)

### Demo akkauntlar (paroli hammasida: `Test1234!`)

| Rol | Telefon raqam |
|---|---|
| Admin | `+998901234567` |
| O'qituvchi (Matematika) | `+998911112233` |
| O'quvchi (7-A) | `+998933334455` |
| Ota-ona | `+998977001122` |
| Psixolog | `+998955501234` |

> Telefon raqamni **`+` bilan va probelsiz** yozing — backend aynan (byte-for-byte) moslashtiradi.

### Holatini tekshirish / loglarni ko'rish

```bash
docker ps                          # qaysi konteynerlar ishlayapti
docker compose ps                  # xizmatlar holati
docker compose logs -f backend     # backend loglari (jonli)
docker compose logs -f frontend    # frontend loglari
```

### To'xtatish / qayta ishga tushirish

```bash
docker compose stop      # to'xtatadi (ma'lumotlar saqlanadi)
docker compose start     # qayta ishga tushiradi
docker compose down      # butunlay o'chiradi (volume'lar saqlanadi)
```

> `docker compose down` dan keyin ham demo-ma'lumotlar qoladi (volume'lar o'chmaydi).
> Hammasini — jumladan bazani — tozalash uchun: `docker compose down -v`

---

## 3. Cloudflared tunnel — tashqaridan ochish

Istalgan payt telefon/hamkasbga ko'rsatish uchun bepul https-manzil ochadi.

### 3.1. Birinchi marta: cloudflared o'rnatish

```bash
# agar hali o'rnatilmagan bo'lsa:
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o ~/.local/bin/cloudflared
chmod +x ~/.local/bin/cloudflared
```

### 3.2. Tunnelni ishga tushirish

**2 ta terminal kerak** (yoki `&` bilan fon rejimida):

**Terminal 1 — nginx-dev (localhost:8082 da frontend+backend ni bitta origin qiladi):**

```bash
cd infra
docker compose -f docker-compose.yml -f docker-compose.tunnel.yml up -d nginx-dev
```

**Terminal 2 — cloudflared:**

```bash
cloudflared tunnel --url http://localhost:8082
```

Bir necha soniyadan so'ng shunday https-manzil chiqadi — shuni ochasiz:

```
https://random-words.trycloudflare.com
```

> Bu manzil **har safar yangi** bo'ladi. Tunneldan kirishda ham login
> `+998901234567` / `Test1234!` bilan bir xil ishlaydi.

### 3.3. Tunnelni to'xtatish

**Terminal 2:** `Ctrl+C` (cloudflared o'chadi)

**Terminal 1:**

```bash
cd infra
docker compose -f docker-compose.yml -f docker-compose.tunnel.yml stop nginx-dev
docker rm infra-nginx-dev-1       # konteynerni butunlay o'chirish (ixtiyoriy)
```

Tekshirish: `curl -s -o /dev/null -w '%{http_code}' http://localhost:8082` — javob
bo'lmasa tunnel o'chgan. Asosiy stack (backend/frontend/DB) esa **ishlashda davom etadi**.

---

## 4. Tez-tez so'raladigan holatlar

| Holat | Yechim |
|---|---|
| `localhost:3000` da login "Telefon raqam yoki parol noto'g'ri." deydi | Avval `docker compose ps` bilan backend (`backend` = healthy) ni tekshiring. Keyin `curl -X POST http://localhost:3000/api/v1/auth/login -H 'Content-Type: application/json' -d '{"phone":"+998901234567","password":"Test1234!"}'` — 200 bo'lmasa loglarga qarang |
| Manzil telefonda ochilmayapti | Telefon bilan komputer bir xil Wi-Fi'da bo'lishi shart emas (cloudflared internet orqali). Lekin komputerning o'zi internetga ulangan bo'lishi kerak |
| Tunnel manzili o'chib qoldi | Cloudflared yopilgan — 3.2 ni qayta bajaring (manzil yangilanadi) |
| Seed qayta ishga tushmayapti | `seed` bir martalik. Qayta seed uchun: `docker compose down && docker compose up -d` (baza bo'sh bo'lsa) yoki `docker compose run --rm seed` |
| To'liq yangi boshlash (barcha ma'lumotlarni o'chirish) | `cd infra && docker compose down -v && docker compose up -d --build` |

---

## 5. Xizmatlar va portlar

| Xizmat | Manzil |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api/v1 (Swagger: `/swagger-ui`) |
| Backend health | http://localhost:8080/actuator/health |
| Postgres | localhost:5432 |
| RabbitMQ UI | http://localhost:15672 (academix / academix_local_dev) |
| SeaweedFS UI | http://localhost:9333 |
| Tunnel nginx | http://localhost:8082 |




https://github.com/korean-deamon/academix.git va bu private git hammasini push qilib qo'y okay hammasini .claude o'chirib tashla .freebuffni ham o'chir okay va keraksiz narsalarni gitignorega qo'sh ham 