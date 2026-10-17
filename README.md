# Lab 1 - Advanced Calculator

A simple Flutter calculator created for the first Mobile Development lab.

The project intentionally uses only standard Flutter classes and contains no
third-party packages. The calculation code is separate from the interface so it
can be tested independently.

## Project structure

- `lib/main.dart` starts the application.
- `lib/calculator_page.dart` contains the interface and button actions.
- `lib/calculator_engine.dart` validates and calculates expressions.
- `lib/history_entry.dart` stores one history item.

## Required features

- `+`, `-`, `×`, `÷`, decimal numbers, parentheses and operation priority
- percent, sign change, clear and backspace
- the last 10 calculations
- `M+`, `M-`, `MR` and `MC`
- state preservation and safe error messages

## Run

```sh
flutter pub get
flutter test
flutter run
```

For a quick web run, use `flutter run -d chrome`.
