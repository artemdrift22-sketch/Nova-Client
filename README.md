# Nova — визуальный мод (Fabric 1.21.4)

## Что уже есть (пакет 1: этапы 0–3 + старт этапа 4)
- **Ядро:** система модулей, категории, бинды (срабатывают только без открытого экрана).
- **Настройки:** Bool / Number (слайдер) / Mode / Color (+RGB), условная видимость.
- **Конфиг:** `config/nova/default.json`, атомарное сохранение, битый конфиг → `.broken.json` и сброс, профили.
- **Рендер:** rect / outline / rounded / градиент, анимации независимые от FPS, единая тема с акцентом.
- **ClickGUI (Right Shift):** панели по категориям, drag, ЛКМ — вкл, ПКМ — настройки, СКМ — бинд, поиск печатью, тултипы, анимация открытия.
- **HUD Editor:** drag & drop, масштаб колесом, снап к краям и центру с направляющими, позиции в % экрана.
- **HUD-модули:** Watermark, ModuleList, FPS, Ping, Coords (+Незер), ArmorHUD, Keystrokes, CPS, Potions.
- **Мир:** Fullbright.

## Сборка
1. JDK 21, IntelliJ IDEA + плагин Minecraft Development.
2. Сверь версии в `gradle.properties` с https://fabricmc.net/develop.
3. `gradle wrapper` (один раз), потом `./gradlew runClient` — тест, `./gradlew build` — jar в `build/libs`.

## Fair-play
Только визуал. Никаких X-Ray, ESP сквозь стены, автоклика и пакетных фич — чтобы мод не банили.

## Дальше
Пакет 2 — остаток HUD (этап 4). Пакет 3 — PvP-визуал (этап 5). Пакет 4 — мир (этап 6). Пакет 5 — косметика (этап 7).

## v0.2.0 — пакет 2
HUD: HealthBars (стекло ХП/броня/голод), InventoryHUD, HeldItem, Speedometer, LookInfo (блок/сущность), LowHPWarning, Crosshair (5 стилей, динамика, цвет на цели).
PvP (только инфо): TargetHUD, ComboCounter, ReachDisplay, Cooldowns (атака/лук/щит/перл), Counters (тотемы, гепплы, перлы, стрелы, попы тотема).
Мир: Time&Weather (своё время/цикл/погода, только клиент).
QoL: Zoom (C + колесо, свой FOV), ToggleSprint/Sneak, NoOverlays (огонь, виньетка, hurt cam, портал, вода, боссбар), HideNametags, ChatAlerts, CopyData.
Клиент: Presets (PvP/Анархия/Ванила+/Стример/Минимал + профили), ServerMode.
Миксины (могут потребовать правки имён под версию): InGameHud, BossBarHud, GameRenderer(getFov, tiltViewWhenHurt), InGameOverlayRenderer, EntityRenderer.hasLabel, ClientWorld.Properties.getTimeOfDay.
