# TODO-NOW

Notes that must be resolved **before the open features can be closed** — the platform-setup refactor
and the NeoForge/Forge lifecycle wiring. "Now" is not "this session" — it is "not left behind". Tick a
box only when the change is in and verified.

## S3 — NeoForge first

*Implemented and committed (`834d24f`); the verify run below is outstanding.*

Order: NeoForge before Forge, because `NeoForgeSetup` owned `loaderModule`/`coreModule` (shared by
both) and because it left the plugin's legacy body holding Forge alone.

- [ ] Verify: `-p build-logic test`, `:platforms:21.1:21.1-neoforge:smokeClient`.

## S4 — Forge last (and it finishes the job)

*Implemented and committed (`cc256b1`); the verify run below is outstanding.*

- [ ] Verify: `-p build-logic test`; `:platforms:21.1:21.1-forge:build` and inspect
      `META-INF/jarjar/metadata.json` + the nested jars; `:platforms:21.1:21.1-forge:smokeClient`.

## S5 — sweep and close

- [ ] Run `smokeAll` across the four loaders and `:platforms:21.1:jar`.

## NeoForge/Forge lifecycle — risks to resolve

Each item is a decision or a check that the lifecycle change must leave settled; none is verified yet.

- [ ] **Content registration timing (largest).** `FMLCommonSetupEvent` fires after the loader's registry
      events, and `LoaderHooks.register` returns the registered value immediately. Today the NeoForge/
      Forge hooks therefore throw instead of silently not registering. Do not make them no-ops: design
      the follow-up (replay registrations from `RegisterEvent`, or collect `DeferredRegister`s made
      during `initCommon`) and track it in a bean. Until then, a mod that calls `block()`/`item()`/
      `entity()`/`sound()`/`channel()` on these loaders must fail loudly.
- [ ] **Registry location vs universal-jar relocation.** `ModularModRegistry` must stay in
      `modularity-api`, because `UniversalJar` relocates `platform.*` to
      `universal.<loader>.platform.*` and adapters of other mods can only link against unrelocated API
      classes. Verify on the built universal jar that an adapter class would still resolve the registry.
- [ ] **NeoForge dist classes.** Each of the three `@Mod` classes must have exactly one public
      constructor taking `IEventBus`, and the common class must not hold or reference client types.
      Check the dedicated-server smoke log for `NoClassDefFoundError` and for a `client` entry-point line
      that must not appear.
- [ ] **Forge constructor injection.** Forge 52.1.21 injects only `FMLJavaModLoadingContext`; there is
      no `IEventBus` injection and no `FMLModLoadingContext` class. Use
      `FMLJavaModLoadingContext.get().getModEventBus()` (or the injected context). Do not copy the
      NeoForge constructor signature.
- [ ] **Event-bus mismatch.** Setup events and reload listeners are mod-bus events; `ClientTickEvent`
      (NeoForge) and `TickEvent.ClientTickEvent` (Forge) are game-bus events. Verify a registered client
      tick handler actually runs and that a reload listener fires, not just that registration compiles.
- [ ] **Lazy `LoaderInfo`.** `ModList.get()` is only valid from common setup onward; the NeoForge/Forge
      `LoaderInfo` must not be called earlier, and `McRuntime.INSTANCE` is constructed inside
      `initCommon`. Check the constructor installs seams without touching mod lists.
- [ ] **Parallel dispatch.** `FMLCommonSetupEvent` runs listeners in parallel; run
      `initCommon`/`initServer` through `enqueueWork` so they execute on the main thread after the
      parallel phase, and register the listeners themselves outside the enqueued work.
- [ ] **Entry-point log compatibility.** Keep `entrypoint mod-constructor active (neoforge|forge)`
      verbatim (beans `modularity-8bjy` and `modularity-jzqc` match it); the new `common`, `client` and
      `server` lines are additive and must not replace it.
