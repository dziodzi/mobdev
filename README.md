# Advanced Calculator

Flutter laboratory project: an advanced expression calculator with arithmetic
precedence, parentheses, decimal numbers, percentage behaviour, sign toggle,
calculation history, calculator memory, error handling, and responsive portrait /
landscape layout.

## UI concept

The visual layer is inspired by early/mid-2000s smartphones and BlackBerry-era
industrial UI: graphite/silver plastic, inset LCD-style display, compact beveled
keys, cyan status accents, and dense information layout.

Design code is separated from calculator logic:

- `lib/calculator_engine.dart` — expression parsing and calculation logic.
- `lib/theme/retro_theme.dart` — 00s palette and global theme.
- `lib/widgets/retro_calculator_button.dart` — tactile beveled calculator key.
- `lib/widgets/retro_display.dart` — LCD-style display.
- `lib/widgets/retro_header.dart` — compact retro title/status bar.
- `lib/main.dart` — screen state and calculator behaviour.
