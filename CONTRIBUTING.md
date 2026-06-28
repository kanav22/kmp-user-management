# Contributing

Thanks for your interest in this project. This repo is a reference implementation for production KMP architecture — contributions that improve clarity, test coverage, or platform support are welcome.

## Getting started

1. Fork the repo and clone your fork
2. Ensure JDK 17+ is installed
3. Run tests before making changes:

```bash
./gradlew :composeApp:allTests
```

## Code conventions

- Follow existing **MVI** patterns: `State`, `Intent`, `Effect`, and `ViewModel` boundaries stay explicit
- Keep **offline-first** semantics: reads from local cache, network syncs asynchronously
- Shared code lives in `commonMain`; platform-specific code only in `*Main` source sets
- Match existing naming and package structure under `com.kanav.usermanager`

## Pull requests

1. Open an issue first for large changes (new features, architecture shifts)
2. Keep PRs focused — one concern per PR
3. Ensure CI passes (GitHub Actions runs on every push)
4. Update docs if you change behavior or architecture

## Reporting bugs

Use the [bug report template](.github/ISSUE_TEMPLATE/bug_report.yml) and include:

- Platform (Android / iOS / Desktop)
- Steps to reproduce
- Expected vs actual behavior

## Questions

For architecture questions, read [docs/architecture/kmp-mvi-offline-first.md](docs/architecture/kmp-mvi-offline-first.md) first.

For other inquiries, open a [GitHub issue](https://github.com/kanav22/sliide-kmp-user-management/issues) or reach out via [kanavwadhawan.com](https://www.kanavwadhawan.com).
