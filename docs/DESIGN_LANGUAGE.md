# LiveBridge Web Design Language

This document defines the design system, layout standards, and styling guidelines for the LiveBridge landing page website. If you are making modifications, adding sections, or improving components, please adhere to these design principles and tokens to maintain visual harmony.

---

## 1. Typography

We use Google Fonts loaded at the top of the document:
*   **Headings**: `Plus Jakarta Sans` (sans-serif) — bold, organic, and clean with a slight geometric form.
*   **Body Text**: `Inter` (sans-serif) — highly readable at all font sizes, optimized for system screens.
*   **Monospace/Code**: System monospace font (for architecture types, package identifiers, etc.).

### Visual Hierarchy

| Style | Element / Class | Desktop Size | Mobile Size (<600px) | Weight | Line Height |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Hero Title** | `hero h1` | Clamped `2.5rem` to `3.8rem` | Clamped `1.8rem` to `2.4rem` | `800` (Extra Bold) | `1.15` |
| **Section Title** | `section-header h2` | `2.5rem` | `1.85rem` | `800` (Extra Bold) | `1.25` |
| **Component Title** | `showcase-text h2` | `2.2rem` | `1.5rem` | `800` (Extra Bold) | `1.25` |
| **Body Paragraph** | `body`, `p` | `1.1rem` to `1.15rem` | `0.98rem` to `1.0rem` | `400` / `500` | `1.6` to `1.65` |
| **Subtitles / Labels** | `.banner-stat-label`, etc. | `0.72rem` | `0.72rem` | `700` (Bold, Uppercase) | `1.4` |

---

## 2. Color Palette (CSS Tokens)

Colors are managed globally through CSS Custom Properties inside the `:root` and `@media (prefers-color-scheme: dark)` blocks. Do not use hardcoded hex or RGB colors in new components.

```css
:root {
  /* Light Theme Colors */
  --bg: #F4F7F6;
  --surface: #FFFFFF;
  --surface-container: #E8ECEB;
  --surface-container-highest: #DFE4E0;
  --text: #1A2120;
  --text-muted: #53605E;
  --primary: #0D9488;                 /* Deep Teal */
  --on-primary: #FFFFFF;
  --primary-container: #99F6E4;
  --on-primary-container: #00201D;
  --outline: rgba(13, 148, 136, 0.3);
  --border: rgba(26, 33, 32, 0.1);
  --radius: 16px;
  --header-bg: rgba(244, 247, 246, 0.85);
  --card-shadow: 0 4px 20px -2px rgba(26, 33, 32, 0.05);
}

@media (prefers-color-scheme: dark) {
  :root {
    /* Dark Theme Colors (Default view for most developer devices) */
    --bg: #060A0A;                    /* Deep, rich dark green-black */
    --surface: #0A0F0F;               /* Slightly lighter elevated surface */
    --surface-container: #101919;     /* Tertiary background for details/badges */
    --surface-container-highest: #1B2929;
    --text: #F1F3F2;                  /* Soft white */
    --text-muted: #808B89;            /* Greyish teal */
    --primary: #3DD1C4;               /* Electric Mint / Cyan */
    --on-primary: #012421;
    --primary-container: #084942;
    --on-primary-container: #C2FFF7;
    --outline: rgba(61, 209, 196, 0.4);
    --border: #162424;                /* Very dark green-grey border */
    --header-bg: rgba(6, 10, 10, 0.85);
    --card-shadow: 0 8px 30px rgba(0, 0, 0, 0.45);
  }
}
```

---

## 3. Grid & Layout System

We use a standard container grid system to ensure content lines up perfectly across all devices.

*   **Max Grid Width**: `1100px` (managed by `.container`).
*   **Padding**: `16px` on the left and right edges (prevents content touching the screen edges on mobile).
*   **Full-Bleed Sections**: Wrap background sections (like `header`, `footer`, `showcase`, `downloads`) in full-width blocks, then wrap their inner contents in a `.container` block.
    *   *Example*: The `footer` element must sit outside the main page `.container` to stretch its background color 100% full-bleed, centering its contents internally with nested `.container` classes.

---

## 4. UI Components

### Buttons (`.btn`)
*   **Tactile Feedback**: Every button must include the `:active` scale state for touch response:
    ```css
    .btn:active {
      transform: scale(0.97) !important;
    }
    ```
*   **Types**:
    *   `.btn-filled`: Primary action button. Features custom mint/teal drop shadows on hover.
    *   `.btn-outlined`: Secondary action button. Features a subtle outline border and light overlay on hover.

### Stats Blocks (`.stats`)
*   **Desktop**: Grid row showing 3 columns of floating numbers.
*   **Mobile (<600px)**: Displays as 3 micro-cards side-by-side using `var(--surface)` background, a `1px` border, and `12px` rounded corners.
*   **Metrics Spacing**: Keep mobile gaps at `8px` and stats font size at `1.15rem` to avoid line-wrapping overflows of numbers on small 360px viewports.

### Circular Badge Ticks (`.feature-tick-wrapper`)
*   Replaces basic ASCII checkmark characters with custom SVG vectors.
*   **Styling**: A circular badge of `24px` (desktop) or `20px` (mobile), styled with a `1.5px` border of `var(--primary)` and a translucent background tint (`rgba(61, 209, 196, 0.12)` in dark mode).
*   **Vertical Alignment**: On mobile lists, apply `margin-top: 2px` to the tick wrapper to align the check icon perfectly with the vertical center of the first line of text.

### Download Banner (`.download-banner`)
*   **Desktop**: Displays as a single, wide flexbox row with vertical divider lines.
*   **Mobile (<760px)**: Refactors into a vertical column stack:
    *   Dividers (`.banner-divider`) are hidden (`display: none`).
    *   The release info block overrides its desktop width parameters with `flex: none` to collapse its height and remove unwanted vertical gaps.
    *   Stats columns (`.banner-stat-col`) transform into horizontal dashboard rows using `flex-direction: row; justify-content: space-between;` inside containers styled with border and container background.

---

## 5. Navigation Menu (Drawer Stacking Contexts)

The mobile drawer menu (`.nav-links`) uses a fixed full-screen layout.

*   **Stacking Context Warning**: The parent header (`.site-header`) uses `backdrop-filter`. In CSS specs, this forces the header to act as the containing block for all positioned elements inside it.
*   **Correct Drawer Sizing**: Do **not** use `bottom: 0` to stretch the drawer vertically on mobile, as it will evaluate against the header height (collapsing the menu height to `0px`). Always use:
    ```css
    .nav-links {
      position: fixed;
      top: 72px;
      left: 0;
      right: 0;
      height: calc(100dvh - 72px); /* Correctly fills viewport using Dynamic Viewport Height */
      background-color: var(--bg); /* Solid background to block out page text behind */
    }
    ```

---

## 6. Animation Guidelines

*   **Speed**: Standard transitions use `--transition-smooth: all 0.3s cubic-bezier(0.4, 0, 0.2, 1)`.
*   **Live Counters**: The download numbers update live using the `.live-counter` wrapper, which dynamically creates flipping text segments. Keep these animation containers to **numbers-only**. Do not wrap static sentences in `.live-counter` tags, as it prevents normal text wrapping on mobile viewports.
*   **Reduced Motion**: Respect system preferences by wrapping flip animations in `@media (prefers-reduced-motion: reduce)`.
