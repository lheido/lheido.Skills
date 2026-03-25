# Lheido Skills

A Hytale server plugin that adds a skill system with collectible upgrades, custom item interactions, persistent player skill components, and an in-game skill selection HUD.

## Overview

- **Project type:** Hytale server plugin (`JavaPlugin`)
- **Main entry point:** `lheido.skills.LheidoSkillsPlugin`
- **Language:** Java
- **Build tool:** Gradle
- **Required dependency:** `libs/HytaleServer.jar`

## Features

This plugin currently includes:

- **Flying**
- **Water Breathing**
- **Stamina**
- **Poison Resistance**
- **Fire Resistance**
- **Life Steal**

The project also includes:

- skill upgrade items defined in JSON resources
- custom interaction classes for unlocking and upgrading skills
- ECS components for persistent skill state
- server systems for handling each skill's gameplay behavior
- a skill selection UI and HUD
- a skill essence drop system
- a `/skills` command

## Project Structure

### Java source

- `src/main/java/lheido/skills/` - plugin entry point
- `src/main/java/lheido/skills/components/` - ECS components for skill data
- `src/main/java/lheido/skills/interactions/` - custom item interaction logic
- `src/main/java/lheido/skills/systems/` - gameplay systems for each skill
- `src/main/java/lheido/skills/events/` - event handlers
- `src/main/java/lheido/skills/hud/` - HUD-related classes
- `src/main/java/lheido/skills/commands/` - command implementations

### Resources

- `src/main/resources/Server/Item/Items/Ingredient/` - ingredient items such as skill essence
- `src/main/resources/Server/Item/Items/Upgrades/` - skill and upgrade item definitions
- `src/main/resources/Server/Languages/en-US/` - localization files
- `src/main/resources/Common/UI/Custom/` - custom UI definitions
- `src/main/resources/Common/Icons/ItemsGenerated/` - generated item icons

## Build

Before building, make sure `libs/HytaleServer.jar` exists.

Run:

```/dev/null/build.sh#L1-1
./gradlew clean build
```

The build is configured to:

- compile the plugin with **Java 25**
- expand `serverVersion` in `manifest.json` from the Hytale server JAR manifest
- run tests with JUnit 5
- copy the built JAR into the local Hytale mods directory during `build`

## Development Notes

### Adding a new skill

To add a new skill, you typically need to:

1. Create the item JSON in `src/main/resources/Server/Item/Items/Upgrades/`
2. Add the corresponding translations in `src/main/resources/Server/Languages/en-US/server.lang`
3. Implement the Java interaction and gameplay logic
4. Register the interaction codec in `LheidoSkillsPlugin`
5. Register any required ECS component and system

A skill item is connected to Java code through the interaction system.

Example interaction skeleton:

```/dev/null/MyCustomInteraction.java#L1-14
public class MyCustomInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<MyCustomInteraction> CODEC = BuilderCodec.builder(
            MyCustomInteraction.class, MyCustomInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType,
                            @Nonnull InteractionContext interactionContext,
                            @Nonnull CooldownHandler cooldownHandler) {
        // Custom behavior when the item is used
    }
}
```

Example registration in the plugin setup:

```/dev/null/LheidoSkillsPlugin.java#L1-3
this.getCodecRegistry(Interaction.CODEC).register(
    "my_custom_interaction_id", MyCustomInteraction.class, MyCustomInteraction.CODEC
);
```

## Skill Naming Convention

Skill item names follow this format:

- `Skill_<SkillName>_<Level>`

Level mapping:

- `A` = level 1
- `B` = level 2
- `C` = level 3
- `X` = ultimate or final upgrade tier

Example:

- `Skill_Flying_A` = Flying level 1

## Useful References

- Hytale item interaction guide: <https://hytalemodding.dev/en/docs/guides/plugin/item-interaction>
- Hytale ECS / codec guide: <https://hytalemodding.dev/en/docs/guides/ecs/hytale-ecs-theory#codec>

## Claude Skills

Additional project guidance is available in `.claude/skills/`:

- `create-hytale-skill`
- `project-structure`
