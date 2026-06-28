# RaumBaller itch.io release checklist

## Files in this release directory

- `raumballer-itch.zip` — upload this as the playable HTML build.
- `cover/raumballer-cover-630x500.png` — itch.io cover image.
- `screenshots/*.png` — browser screenshots for the itch.io media gallery.
- `itch-page-copy.md` — suggested page copy, controls, tags, credits, and upload notes.

## Build commands used

```sh
source scripts/dev-env.sh
./gradlew --no-daemon :web:build
(cd web/build/dist && zip -qr ../../../release/raumballer-itch.zip .)
```

Full verification command run after the browser port:

```sh
source scripts/dev-env.sh
./gradlew --no-daemon :app:assembleDebug :web:build
```

## Package checks

- [x] `release/raumballer-itch.zip` exists.
- [x] Zip contains `index.html` at the root.
- [x] Zip contains `raumballer.js` at the root.
- [x] Zip contains `assets/` with levels, images, sounds, and `shooter.tbl`.
- [x] Static browser build loads from a local HTTP server.
- [x] Canvas keeps the original 9:16 aspect ratio while filling the window.
- [x] Browser parity scenarios matched deterministic trace checksums.
- [x] Screenshot set captured from the browser build.

## Local smoke test

```sh
python3 -m http.server 18765 --bind 127.0.0.1 --directory web/build/dist
open http://127.0.0.1:18765/index.html
```

Check:

- Title screen appears.
- Tap/click a ship to start.
- Pointer/touch movement works.
- WASD and arrow movement work.
- Space does not scroll the page.
- Audio starts after user interaction, subject to browser autoplay rules.

## itch.io upload steps

1. Create or edit the itch.io project page.
2. Set project kind to **HTML**.
3. Upload `release/raumballer-itch.zip`.
4. Mark the zip as playable in the browser.
5. Add `release/cover/raumballer-cover-630x500.png` as the cover.
6. Add all images in `release/screenshots/` as screenshots.
7. Paste copy from `release/itch-page-copy.md`.
8. Suggested embed size: `630×900` or larger.
9. Save as draft and test with itch.io's browser preview.
10. Publish when the preview plays correctly.

## Notes

The web build is static. It does not require a backend. The only requirement is serving it over HTTP/HTTPS so browser asset fetching works correctly.
