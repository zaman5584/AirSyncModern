# Air Sync Remote Z — Design System & UI/UX Specification (Material 3)

## 1. Overview & Design Philosophy
Air Sync Remote Z is a dedicated, high-performance companion app for Formuler / HiSilicon Android Set-Top Boxes. The refreshed design replaces the cluttered, skeuomorphic, glossy 2013-era interface with a refined, tactile, and modern **Material 3 (Expressive Dark)** design system.

### Core Principles
1. **OLED Ergonomics (One-Thumb Reach):** The most frequent actions (D-Pad, OK, Back, Home, Volume) are located strictly in the primary thumb sweep zone (bottom 60% of the screen).
2. **Tactile Haptic Affirmation:** Every button press delivers immediate haptic feedback (`HapticFeedbackType.TextHandleMove` / `VirtualKey`) and distinct active micro-interactions.
3. **Glanceable Status:** Instant visual awareness of STB connection, Wi-Fi latency, and current active remote mode.
4. **Dark Atmosphere:** An authentic deep surface palette designed for living room environments with the lights dimmed.

---

## 2. Color Palette (Material 3 Tokens)

| Token Name | Hex Code | Purpose & Usage |
|---|---|---|
| `md_theme_dark_background` | `#0B0E14` | Primary app canvas (Deep OLED Black/Navy) |
| `md_theme_dark_surface` | `#141822` | Card backgrounds, remote chassis, panels |
| `md_theme_dark_surfaceVariant` | `#1E2433` | Inactive buttons, D-Pad base ring, dividers |
| `md_theme_dark_surfaceContainerHigh` | `#273042` | Button pressed states, elevated toolbars |
| `md_theme_dark_primary` | `#00D2FF` | Active accent, selected mode indicator, glow |
| `md_theme_dark_onPrimary` | `#003544` | High contrast icon on primary active pill |
| `md_theme_dark_secondary` | `#FF9E00` | Alert/warning, power button accent, recording state |
| `md_theme_dark_onSurface` | `#F0F3FA` | Primary text and active icon glyphs |
| `md_theme_dark_onSurfaceVariant` | `#8C97AC` | Secondary labels, subtitle info, inactive icons |
| `md_theme_dark_success` | `#00E676` | STB Connected status dot |
| `md_theme_dark_error` | `#FF5252` | Connection lost / timeout indicators |

---

## 3. Typography Scale (Inter / Roboto)

```
Display Large    - 34sp / Line: 40sp / Weight: Bold        (Mode titles, Hero screens)
Headline Medium  - 22sp / Line: 28sp / Weight: SemiBold    (STB Name, Dialog titles)
Title Medium     - 16sp / Line: 24sp / Weight: Medium      (Section headers, Button labels)
Body Medium      - 14sp / Line: 20sp / Weight: Normal      (Status descriptions, IP info)
Label Small      - 11sp / Line: 16sp / Weight: SemiBold    (Button sub-labels, Keypad caps)
```

---

## 4. Spacing & Shape Tokens

- **Grid Base:** 4dp
- **Padding Margins:** 16dp horizontal gutter, 24dp screen padding
- **Elevation / Corner Radius:**
  - `Remote Chassis:` Rounded 32dp container
  - `D-Pad Ring:` Outer circle 240dp × 240dp with circular cutouts
  - `OK Button (Center):` Circular 84dp × 84dp with primary gradient glow
  - `Standard Remote Buttons:` Rounded 20dp squircles (min 56dp × 56dp touch target)
  - `Mode Selector Tabs:` Pill capsules 36dp height, rounded 18dp

---

## 5. Remote Interaction Modes

### Mode A: Directional D-Pad (Default Mode)
- **Header:** Device indicator (`Formuler Z8 - 192.168.1.45`), Connection Ping dot (Green 12ms), Power button (Right), Settings (Left).
- **Mode Segmented Bar:** `[ D-Pad | Touchpad | Gamepad | Keyboard | Numpad ]`
- **Center Stage:**
  - Radial D-Pad disc with directional arrows (Up, Down, Left, Right) positioned at 90° intervals.
  - Center OK button with elevated tactile surface and ripple glow.
- **Flanking Quick Controls:**
  - Left flank: Back, Menu, Info.
  - Right flank: Home, EPG, Exit.
- **Bottom Bar:**
  - Rocker switches for Volume (`+` / `-` / Mute) and Channel (`CH+` / `CH-`).
  - Dedicated Voice button in the center (push-to-talk).

### Mode B: Air Mouse / Touchpad Mode
- Large seamless glass card filling the center screen.
- Two-finger scroll support, single-tap Left Click, two-finger tap Right Click.
- Top toggle for "Gyro Sensor Air Mouse" (shake to engage cursor control).

### Mode C: Virtual Gamepad Mode
- Landscape orientation lock.
- Left thumb: 8-way digital thumbstick.
- Right thumb: Classic diamond layout (A, B, X, Y buttons) + L1/L2 and R1/R2 shoulder triggers.

### Mode D: Virtual Keyboard / VIME Mode
- Integrates with system Android soft keyboard.
- Quick action pills: `Send`, `Clear`, `Paste from Clipboard`.
- Live preview showing the STB input box text.

### Mode E: Number Pad Mode
- 3×4 telephone grid (1-9, 0, Backspace, Dash `-`).
- Rapid channel zapping and PIN code entry.

---

## 6. Motion & Feedback Choreography
- **Press Transition:** Fast 80ms shrink scale (`0.94f`) on button press, spring back (`stiffness = Spring.StiffnessMediumLow`).
- **Mode Switch Transition:** Smooth slide + fade horizontally across remote faces using `AnimatedContent`.
- **Haptic Profile:**
  - Button Click: `HapticFeedbackType.Confirm` (Light click)
  - Long Press (Volume / Rapid Seek): Repeated ticks every 150ms
  - Voice Recording Start/Stop: Heavy double haptic pulse.
