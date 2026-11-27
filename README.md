# Vanilla Extras

A Fabric mod for Minecraft 26.3 that adds features while staying vanilla-compatible: a server running it still accepts unmodded clients. Players can install the same jar on their client for optional extras, but it is never required.

One jar works on both servers and clients. See [docs/DESIGN.md](docs/DESIGN.md) for the rules every feature follows.

## Requirements

| Component | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5+ |
| Fabric API | 0.163.0+26.3 |
| Java (JDK) | 25 |

## Building

```bash
./gradlew build
```

The mod jar lands in `build/libs/`.

Every push and pull request also builds on GitHub Actions. Download the jar from the run's **vanilla-extras** artifact on the Actions tab.

## Running in development

```bash
./gradlew runClient
./gradlew runServer
```
