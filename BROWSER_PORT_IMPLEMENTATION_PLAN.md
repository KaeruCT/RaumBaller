# Browser Port Implementation Plan

## Objective

Make RaumBaller playable in the browser with exact feature parity to the current Android game, plus optional keyboard controls for desktop play.

Exact parity means the browser build keeps the same gameplay rules, assets, levels, timing model, collision behavior, touch/pointer input semantics, sounds, state transitions, and current quirks. Keyboard controls are an additive browser feature and must not change touch behavior.

## Recommended approach

Use TeaVM to compile the existing Java game logic to browser JavaScript, and replace only the Android/JGame platform layer with a browser Canvas/WebAudio platform layer.

This is the most effective route because the gameplay code remains the source of truth. A TypeScript rewrite would be easier to start but much harder to prove equivalent.

## Current codebase shape

- The actual game logic is small and mostly isolated under the RaumBaller package.
- The larger JGame engine contains reusable object, timer, animation, collision, and state-machine logic.
- Android-specific coupling is concentrated in the platform layer.
- Assets are already browser-friendly: PNG sprites/backgrounds/fonts, WAV audio, text level files, and a media manifest.
- The game runs at a fixed logical size of 288x512 pixels.
- Current Android input is pointer-based: press/move/release map to mouse button 1.
- Browser input should preserve pointer behavior and add keyboard controls: WASD or arrow keys to move, Space to shoot.

## Feature parity scope

The browser version must preserve:

- Title screen and instructions
- Three selectable ships
- Four levels
- All enemy waves and enemy types
- Player movement by tapping/holding destination
- Optional keyboard movement with WASD or arrow keys
- Automatic shooting for parity, with Space as an additional explicit shoot input
- Bullet behavior and collisions
- Enemy collision damage
- Health pickup behavior
- Score calculation
- Cannon upgrades at the existing score thresholds
- Level-complete flow
- Game-over flow
- Winner flow
- Parallax backgrounds and stars
- Sprite animations and flashing behavior
- Pixel font rendering
- Sound effects and looping title music
- Current no-op high-score entry behavior

## Architecture

Create a new web build alongside the existing Android build.

```txt
:app   Android build, unchanged
:web   Browser build using TeaVM
```

The web module should reuse:

```txt
app/src/main/java/com/kaeruct/raumballer/**
app/src/main/java/jgame/**
app/src/main/assets/**
```

The web module should exclude Android platform classes and replace them with browser implementations:

```txt
exclude app/src/main/java/jgame/platform/**
include web/src/main/java/jgame/platform/**
include web/src/main/java/com/kaeruct/raumballer/web/**
```

## Implementation phases

### Phase 1: Add web module

Add a Gradle `web` module that:

1. Applies the TeaVM Gradle plugin.
2. Compiles Java 8-compatible sources.
3. Reuses the existing game and engine source sets.
4. Excludes Android-only platform files.
5. Copies all assets into the browser distribution.
6. Produces a static web output directory with `index.html`, JavaScript, and assets.

Expected local command:

```sh
./gradlew :web:build
```

Expected output:

```txt
web/build/dist/index.html
web/build/dist/raumballer.js
web/build/dist/assets/**
```

### Phase 2: Add platform-neutral asset loading

Current game code opens levels through Android assets. Replace that dependency with a platform-neutral method.

Add an engine-level method:

```java
InputStream openAsset(String path)
```

Android implementation:

```java
return getAssets().open(path);
```

Browser implementation:

- Load from the browser asset manifest or fetched static files.
- Return an `InputStream` backed by asset bytes or UTF-8 text.

Update all asset reads to use `openAsset`, including:

- media manifest loading
- level file loading
- image loading
- sound loading metadata

### Phase 3: Remove runtime reflection from gameplay factories

TeaVM can support some reflection, but explicit factories are safer and easier to verify.

Replace wave class reflection with:

```java
WaveFactory.create(name, game, reader, maxAmount)
```

Replace enemy ship reflection with:

```java
EnemyShipFactory.create(name, x, y, velocity, angle, game)
```

The factories should include every currently referenced type:

```txt
SparkWave
FireFormationWave
SpaceBallWave
SparkEyeWave
CibumWave
BobbaWave

SparkDefender
FireStriker
SpaceBall
SparkEye
CibumDestroyer
BobbaDestroyer
Asterisk
```

This change should not alter gameplay behavior.

### Phase 4: Implement browser image support

Create a browser `JGImage` implementation backed by HTML image/canvas objects.

It must support the operations used by the existing media manifest:

- load PNG
- crop sprite sheet cells
- flip horizontally/vertically
- rotate 90/180/270 degrees
- scale
- report exact image size
- preserve transparency

Canvas rendering must use nearest-neighbor scaling:

```js
ctx.imageSmoothingEnabled = false;
```

### Phase 5: Implement browser engine shell

Create a browser `jgame.platform.JGEngine` that delegates core behavior to existing `EngineLogic`.

It must implement:

- canvas setup
- fixed-size logical viewport
- scale-to-window layout
- frame loop
- object rendering
- background rendering
- game-state method dispatch
- timer ticking
- collision and object flushing through existing engine logic
- media definition from `shooter.tbl`
- debug/error reporting to console

Keep the fixed game loop equivalent to Android:

```txt
60 fps
max frame skip: 2
game speed: 1
```

Use `requestAnimationFrame`, but run updates on a fixed timestep.

### Phase 6: Implement browser input

Map pointer events to the same JGame mouse state used by Android:

- `pointerdown` sets mouse button 1 true
- `pointermove` updates mouse position
- `pointerup` and `pointercancel` set mouse button 1 false

Convert browser coordinates into logical game coordinates after accounting for canvas scale and letterboxing.

This is required for exact parity because player movement uses mouse position directly.

Add keyboard controls as a browser-only input layer:

- `W` or `ArrowUp`: accelerate/move up
- `A` or `ArrowLeft`: accelerate/move left
- `S` or `ArrowDown`: accelerate/move down
- `D` or `ArrowRight`: accelerate/move right
- `Space`: shoot

Implementation detail:

- Preserve current auto-shooting by default.
- Space should force/enable shooting but must not disable auto-shooting unless a future design explicitly changes that.
- Keyboard movement should feed the same acceleration model as touch movement, not teleport the player.
- When both pointer and keyboard are active, combine keyboard acceleration with pointer steering in a predictable way. Recommended rule: keyboard vector adds to pointer-derived acceleration, then clamp to the existing max velocity.
- Prevent browser defaults for game keys so Space and arrow keys do not scroll the page.
- Keep keyboard state in the browser platform layer and expose it through engine methods such as `getKey()` or a small browser input adapter used by the player ship.

### Phase 7: Implement browser audio

Use WebAudio or `HTMLAudioElement` to implement the existing audio API:

```java
defineAudioClip(id, filename)
playAudio(channel, clipid, loop)
stopAudio(channel)
stopAudio()
lastPlayedAudio(channel)
```

Preserve channel behavior:

- Playing on a named channel stops the previous stream on that channel.
- Title music loops on the `state` channel.
- Jingles replace title music on the `state` channel.
- Laser, explosion, and health sounds can overlap as they do today unless channel use prevents it.

Handle browser audio unlock on the first pointer interaction.

### Phase 8: Add browser launcher

Create a web launcher that:

1. Creates the canvas.
2. Preloads or lazily loads assets.
3. Instantiates the game.
4. Calls the equivalent of Android engine initialization.
5. Starts the game loop.

The launcher should keep the same logical dimensions and portrait presentation as Android.

### Phase 9: Keep Android build working

All shared changes must preserve the Android app.

Run after each phase that touches shared Java:

```sh
./gradlew :app:assembleDebug
```

Run the web build too:

```sh
./gradlew :web:build
```

## Parity verification plan

Do not rely on manual playtesting alone. Add deterministic parity gates.

### Deterministic random seed

Add a test-only hook to seed the engine random generator.

Production behavior should remain unchanged.

### State checksum harness

Create a headless driver that runs scripted inputs and records a per-frame state trace.

Record at least:

- frame number
- active game state
- level
- score
- player position
- player health
- player animation
- object names/classes
- object positions
- object collision IDs
- object graphics
- active timers
- audio events

Run the same script through:

1. JVM/headless reference build
2. Browser build through Playwright

Compare checksums.

### Required scripted scenarios

Cover:

1. Title screen idle
2. Select each of the three ships
3. Early level 1 gameplay
4. Player movement toward fixed pointer paths
5. Keyboard movement with WASD
6. Keyboard movement with arrow keys
7. Space shoot input while auto-shoot remains active
8. Mixed pointer plus keyboard input
9. Enemy collision
10. Bullet collision
11. Health pickup
12. Score upgrade thresholds
13. Level-complete transition
14. Game-over transition
15. Winner transition after level 4

### Visual snapshot parity

Capture the browser canvas at logical 288x512 pixels and compare against reference frames.

Minimum snapshots:

- title screen
- each selected ship start
- early level 1 gameplay
- dense enemy/bullet frame
- level done
- game over
- winner screen

### Audio event parity

Record ordered audio API events:

```txt
play channel clip loop
stop channel
```

The browser event log must match Android/reference order and loop flags for the scripted scenarios.

## Acceptance criteria

The browser port is complete only when:

- `./gradlew :app:assembleDebug` succeeds.
- `./gradlew :web:build` succeeds.
- Browser build loads from a static server.
- Title screen renders with the same pixel art and font.
- All three ships can be selected.
- All four levels can be played.
- Enemy waves spawn in the same order.
- Pointer-based player movement, shooting, damage, health, score, and upgrades match the reference traces.
- Keyboard movement works with WASD and arrow keys without changing pointer parity.
- Space triggers shooting behavior without breaking existing auto-shoot parity.
- Level done, game over, and winner states match the reference traces.
- Audio events match the reference traces.
- Visual snapshots match at logical resolution or have documented, approved browser-only rendering differences.
- Android behavior remains unchanged.

## Risks and mitigations

### Risk: TeaVM incompatibility with some Java APIs

Mitigation:

- Remove reflection in gameplay factories.
- Avoid Android APIs in shared code.
- Keep platform replacements small.

### Risk: Rendering differs from Android Canvas

Mitigation:

- Render at logical resolution first.
- Disable smoothing.
- Compare pixel snapshots at 288x512 before scaling.

### Risk: Audio timing differs by browser

Mitigation:

- Verify API event order, channel behavior, and loop flags.
- Treat exact waveform scheduling as lower priority than gameplay parity.

### Risk: Randomness changes alter gameplay

Mitigation:

- Add seedable test mode.
- Keep production random default unchanged.

### Risk: Hidden Android dependency remains in shared code

Mitigation:

- Web module should fail compilation if Android classes leak in.
- Keep browser source set explicit.

## Suggested commit sequence

1. Add web module skeleton and asset copy task.
2. Add platform-neutral asset API while preserving Android behavior.
3. Replace reflection with explicit factories.
4. Add browser image/media loader.
5. Add browser canvas engine and launcher.
6. Add browser pointer input mapping.
7. Add browser keyboard controls.
8. Add browser audio implementation.
9. Add parity test harness and scripted scenarios.
10. Fix parity gaps found by checksums and snapshots.
11. Document how to build and run the browser version.

## Recommended first implementation slice

Start with the smallest playable vertical slice:

1. Build TeaVM web module.
2. Load `shooter.tbl`.
3. Load PNG assets.
4. Render title screen.
5. Select a ship by pointer.
6. Enter level 1 with player visible.

Do not implement all audio or all verification before this slice. Once this slice works, add the rest of platform behavior and parity gates incrementally.
