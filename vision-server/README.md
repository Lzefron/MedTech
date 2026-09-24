# MedTrack optional vision gateway

The app works offline without this service. When local OCR misses a row, the user can approve sending **one photo** to a private HTTPS gateway, which calls Gemini and returns editable fields. No database is needed on the server. The user reviews the extraction before saving it locally.

## Recommended API

Start with the stable **Gemini 3.8 Flash** (`gemini-3.8-flash`), listed in Google's model catalog when reviewed on September 25, 2026. It accepts images and structured JSON output. This is a practical starting choice, not a claim that it is the most accurate reader of medical handwriting. Evaluate it using consented, representative reports, especially small decimals and handwritten numbers, before wider use. A configurable route can use another available Gemini vision model; preview models should be evaluated separately.

Get a key in [Google AI Studio](https://aistudio.google.com/apikey) and enable billing for the associated project. **Do not submit patient reports to unpaid Gemini services.** Google's unpaid-service terms prohibit sensitive personal information. Paid-service terms say prompts/responses are not used to improve Google's products, while limited safety/abuse processing still applies. Check current terms and your obligations before deployment.

Provider keys must stay in server secrets. Do not paste them in chat, commit them, put them in Android resources/BuildConfig, or distribute them in the APK. The app stores only a revocable gateway access token, encrypted using Android Keystore. A token authenticates gateway use; it is not a Gemini key.

## Configure and run

Node 22+; no third-party runtime packages. Run `npm test` to exercise the gateway with simulated provider responses (no patient data or live keys).

Set these using your hosting provider's secret manager/environment configuration:

| Setting | Meaning |
| --- | --- |
| `GEMINI_PAID_CONFIRMED` | `true` after checking that your Gemini project has billing enabled and reviewing its terms |
| `GEMINI_ROUTES_JSON` | Ordered JSON array of `{ "project": "your-project-id", "key": "YOUR_SECRET_KEY", "model": "gemini-3.8-flash" }` objects; 1–6 routes |
| `MEDTRACK_ACCESS_TOKENS` | Comma-separated random per-device tokens, at least 32 characters each; keep private and revoke by removing a token |
| `HOST` | Defaults to `127.0.0.1`; use `0.0.0.0` in a container |
| `PORT` | Defaults to `8080` |

Generate a gateway token locally with `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`. Keep its output private.

Run `npm start`, or build the supplied Dockerfile and inject secrets at runtime. Place the service behind managed HTTPS (for example, a private deployment configured on your hosting account). The Android app refuses HTTP and redirects. Do not put tokens into URLs or proxy access logs. Configure the hosting/proxy layer to avoid logging request bodies, disable request caching, cap uploads at 6 MB and allow a 100-second response timeout.

On the phone: **Scan a report → AI connection**. Enter `https://YOUR-HOST/v1/extract` and that device's gateway token. Choose a photo, allow local OCR, then tap **Try AI for missing or unclear fields**. Confirm the displayed destination and photo upload. The returned reading only fills missing fields and adds missing rows when you choose to apply it; disagreements are shown alongside existing values.

This repository does not contain a deployed endpoint or provider credentials. Live model calls cannot be verified until the owner configures a billing-enabled project and hosting. The local app and simulated failover tests do not require these.

## Failure behavior

- At most three provider attempts per request; 25-second attempt timeout and an 85-second gateway deadline.
- Invalid credentials, unavailable models, transient network/server failures and malformed/truncated model output try the next configured route.
- A 429 response puts the **whole project** into cooldown, honoring `Retry-After` with a minimum of 60 seconds. Other keys from that same project are skipped. Multiple keys do not create extra quota. Only configure legitimately authorized backup projects; this is resilience, not a quota bypass.
- Other rejected requests stop rather than repeatedly spending quota. Full Gemini outages ultimately fall back to existing local fields/manual entry.
- If all routes fail, the app keeps all local readings and edits. There is no automatic diagnosis, fabricated range, silent unit conversion or guessed unreadable number.
- The gateway requires an access token and explicit consent, limits each token to 60 requests/hour and allows at most two active requests per process. For a public multi-instance service, add managed per-user authentication, shared rate limiting, budget controls and operational monitoring before distributing it broadly.

## Data handling

The gateway has no database, image uploads, file writes or request-content logging. It sends JPEG bytes directly to Gemini and handles them in memory for the duration of a request; Google and the chosen host have their own processing/retention policies. The Android client re-encodes the selected photo as JPEG without EXIF metadata; visible identifying text remains present. Saving the report retains its original image and the unverified OCR/AI transcription privately on the device, alongside the reviewed result fields.

## Official references

- [Gemini models](https://ai.google.dev/gemini-api/docs/models)
- [Image understanding](https://ai.google.dev/gemini-api/docs/image-understanding)
- [Structured extraction](https://ai.google.dev/gemini-api/docs/generate-content/structured-output)
- [API key security](https://ai.google.dev/gemini-api/docs/api-key)
- [Rate limits apply per project](https://ai.google.dev/gemini-api/docs/rate-limits)
- [Gemini service and data terms](https://ai.google.dev/gemini-api/terms)
