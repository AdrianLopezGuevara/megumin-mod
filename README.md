# 🔮 Megumin Explosion Mod

A KonoSuba-inspired NeoForge 1.21.1 mod that lets you cast Megumin's legendary Explosion spell.

## Status: In Development 🚧

## Features (v1.0.0)
- **Explosion Staff** — obtainable via `/give @s megumin:explosion_staff`
- **Full chant audio** — real Megumin explosion chant from KonoSuba plays on cast
- **13-second charge** — synced so the explosion fires exactly when she shouts "EXPLOSION!!!"
- **Magic circles** — ground rune circle (2 rings + pentagon + pentagram) + 5 horizontal halo rings above the target
- **World goes dark** — Darkness mob effect applied to caster during the charge
- **Massive explosion** — power 6, TNT-tier blast at your target point (10 blocks ahead)
- **Visual lightning bolt** — cosmetic strike at the explosion point
- **Megumin collapses** — Weakness + Slowness + Exhaustion for 10s after casting
- **10s cooldown** — can't spam it; if you release early, audio stops and 3s penalty

## Pending / Next Session
- [ ] Crafting recipe for the Explosion Staff
- [ ] Custom texture improvements
- [ ] Fine-tune magic circle visuals (horizontal sky rings feedback in progress)
- [ ] Possibly: screen shake on explosion
- [ ] Possibly: server deployment to Apex Hosting

## Stack
- **Minecraft**: 1.21.1
- **Loader**: NeoForge 21.1.219
- **NeoGradle**: 7.0.165
- **Gradle**: 8.8
- **Java**: 21

## Build
```bash
gradle build
# Output: build/libs/megumin-mod-1.0.0.jar
```

## Dev Environment
Built and maintained on VPS via Migi (OpenClaw AI assistant).
Deployed to local Minecraft via SCP to `~/.minecraft/mods/`.
