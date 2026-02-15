# TODO - Code Review LheidoSkills

Liste des améliorations identifiées lors de la review de code.

## 🔴 Priorité haute

- [ ] **Internationalisation des messages**
  - Fichier: `SkillFlyingInteraction.java`, `FlyingSystem.java`
  - Remplacer les messages hardcodés par des clés de traduction
  - Utiliser `Message.translatable()` au lieu de `Message.raw()`
  - Ajouter les traductions dans `Server/Languages/en-US/server.lang`

## 🟡 Priorité moyenne

- [x] **Extraire les magic numbers en constantes**
  - Fichier: `FlyingSkillComponent.java`
  - Créer des constantes pour `flyDurationMs` (10000L) et `cooldownMs` (20000L)
  - Exemple: `public static final long DEFAULT_FLY_DURATION_MS = 10_000L;`

- [x] **Gestion des niveaux de skill**
  - Fichier: `FlyingSkillComponent.java`
  - Le champ `level` est stocké mais jamais utilisé
  - Créer des factory methods pour chaque niveau (`createLevelB()`, `createLevelC()`)
  - Ou créer un enum `SkillLevel` avec les configurations

- [x] **Thread-safety pour `COMPONENT_TYPE`**
  - Fichier: `FlyingSkillComponent.java`
  - Ajouter `volatile` à la variable statique `COMPONENT_TYPE`
  - Ou utiliser `AtomicReference`

- [x] **Utiliser `SchedulerUtils` de manière cohérente**
  - Fichiers: `SkillFlyingInteraction.java`, `FlyingSystem.java`
  - Remplacer les divisions manuelles par `SchedulerUtils.msToSeconds()`

## 🟢 Priorité basse

- [ ] **Améliorer la méthode `clone()`**
  - Fichier: `FlyingSkillComponent.java`
  - Soit implémenter correctement `Cloneable` avec `super.clone()`
  - Soit renommer la méthode en `copy()` pour éviter la confusion

- [ ] **Ajouter un commentaire explicatif dans `FlyingSystem`**
  - Fichier: `FlyingSystem.java`
  - Expliquer pourquoi `player` est récupéré via `commandBuffer` et `playerRef` via `chunk`

- [ ] **Améliorer la testabilité**
  - Fichier: `FlyingSkillComponent.java`
  - Injecter une source de temps au lieu d'utiliser `System.currentTimeMillis()` directement
  - Permettre de passer le temps en paramètre pour les méthodes `isOnCooldown()`, `getRemainingFlyTime()`

## 🧪 Tests

- [ ] **Créer des tests unitaires**
  - Tester `SchedulerUtils` (conversions ms/secondes/ticks)
  - Tester `FlyingSkillComponent` (state machine: flying, cooldown, etc.)
  - Le projet est déjà configuré avec JUnit 5

## 📝 Notes

### Résumé de la review

| Aspect | Note |
|--------|------|
| Architecture | ⭐⭐⭐⭐⭐ |
| Lisibilité | ⭐⭐⭐⭐⭐ |
| Documentation | ⭐⭐⭐⭐⭐ |
| Maintenabilité | ⭐⭐⭐⭐ |
| Testabilité | ⭐⭐⭐ |
| Internationalisation | ⭐⭐ |

### Points positifs identifiés

- Architecture ECS bien respectée
- Excellente Javadoc
- Classes utilitaires bien conçues
- Bonne gestion des null checks
- Logging approprié