# RaumBaller web build

This module builds a static browser version of RaumBaller with TeaVM.

## Build

```sh
source scripts/dev-env.sh
./gradlew :web:build
```

The static output is written to:

```txt
web/build/dist/index.html
web/build/dist/raumballer.js
web/build/dist/assets/
```

## Serve locally

Use any static server from `web/build/dist`, for example:

```sh
cd web/build/dist
python3 -m http.server 8080
```

Open `http://localhost:8080/`.

Pointer input maps to the existing mouse-button-1 gameplay path. Keyboard input is browser-only: WASD or arrow keys move, Space is captured so it does not scroll the page while auto-shooting remains enabled.
