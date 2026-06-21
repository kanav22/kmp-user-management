# Contributing

Thanks for your interest in this project. Contributions are welcome — issues, documentation improvements, and pull requests.

## Getting started

1. Fork the repository and clone your fork.
2. Follow [README.md](README.md) for prerequisites and token setup.
3. Create a branch from `main`:

   ```bash
   git checkout -b feature/your-change
   ```

## Development workflow

```bash
./gradlew :composeApp:jvmTest              # shared/JVM tests
./gradlew :composeApp:testDebugUnitTest    # Android unit tests
./gradlew :composeApp:assembleDebug        # debug build
```

CI runs the same checks on every pull request.

## Pull request guidelines

- Keep changes focused and explain the **why** in the PR description.
- Include tests when changing behavior.
- Update README/docs when setup or architecture changes.
- Ensure `./gradlew :composeApp:jvmTest :composeApp:testDebugUnitTest` passes locally.

## Code style

- Follow existing Kotlin and Compose conventions in the codebase.
- Prefer unidirectional data flow (MVI) for new UI state.
- Keep platform-specific code in `androidMain` / `iosMain`; share logic in `commonMain`.

## Reporting issues

When filing a bug, include:

- Steps to reproduce
- Expected vs actual behavior
- Android/iOS version and device or simulator
- Relevant logs or screenshots

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
