package lv.cebbys.mcmods.modularity.testing.allure;

/**
 * Values of the Allure labels the test pyramid uses (docs/07, 7.11). One child class per label, so a report
 * filter names one constant and a renamed label is edited once.
 *
 * <p>The report tree has four levels: epic, feature, story and functionality. The epic is always Modularity.
 * The feature is the layer of the test pyramid. The story is the API area, for example Version Detection. The
 * functionality is the behaviour inside that area, for example Version ranges.
 *
 * <p>Every value is a compile-time constant, so it can stand inside an annotation. The class holds only strings,
 * so it needs no Allure dependency.
 */
public final class ModularityAllureConstants {

    private ModularityAllureConstants() {
    }

    /** Values of the Allure {@code @Epic} annotation. */
    public static final class Epics {

        /** The one epic: every test belongs to Modularity. */
        public static final String MODULARITY = "Modularity";

        private Epics() {
        }
    }

    /** Values of the Allure {@code @Feature} annotation: one per layer of the test pyramid. */
    public static final class Features {

        /** Unit tests that need no Minecraft and no loader. */
        public static final String L1_UNIT = "L1 Unit Tests";
        /** Tests of the build contract: layering, table, metadata, universal jar. */
        public static final String L2_INTEGRATION = "L2 Integration Tests";
        /** One test per smoke run, with the log, the summary and the screenshot attached. */
        public static final String L3_END_TO_END = "L3 End to End Tests";
        /** The shipped jar on every loader (docs/07, PR-13). */
        public static final String L4_PRODUCTION_JAR = "L4 Production Jar";

        private Features() {
        }
    }

    /** Values of the Allure {@code @Story} annotation: one per API area. */
    public static final class Stories {

        /** Minecraft and platform version values: numbers, text form, comparison. */
        public static final String VERSION_API = "Version API";
        /** Ids, ranges, versioned code, block definitions and data codecs. */
        public static final String CORE_API = "Core API";
        /** The loader is found from marker classes on the class path. */
        public static final String PLATFORM_DETECTION = "Platform Detection";
        /** The Minecraft version is found from version.json and from the game classes. */
        public static final String VERSION_DETECTION = "Version Detection";
        /** Entities, items, channels, sounds and client hooks on the test platform. */
        public static final String ENTITY_API = "Entity API";
        /** The common, client and server stages of a mod and their order. */
        public static final String MOD_LIFECYCLE = "Mod Lifecycle";
        /** Rotations, entity parts and part interactions. */
        public static final String MULTIPART_API = "Multipart API";

        private Stories() {
        }
    }

    /**
     * Values of {@link Functionality}: the behaviour a test proves. The report tree shows them under the story,
     * for example Version Detection &gt; Version ranges &gt; test.
     */
    public static final class Functionalities {

        // Version API.
        public static final String MINECRAFT_PARSING = "Minecraft version parsing";
        public static final String PLATFORM_PARSING = "Platform version parsing";
        public static final String VERSION_COMPARISON = "Version comparison";

        // Core API.
        public static final String NAMESPACED_IDS = "Namespaced ids";
        public static final String VERSION_RANGES = "Version ranges";
        public static final String VERSIONED_SELECTION = "Versioned implementation selection";
        public static final String BLOCK_DEFINITIONS = "Block definitions";
        public static final String DATA_CODECS = "Data codecs";

        // Platform Detection.
        public static final String MARKER_CLASSES = "Loader marker classes";
        public static final String MARKER_PRECEDENCE = "Marker precedence";
        public static final String SOURCE_ORDERING = "Source ordering";
        public static final String SOURCE_FAILURES = "Source failures";
        public static final String BUILT_IN_SOURCES = "Built-in sources";
        public static final String SOURCE_INSTALLATION = "Source installation";

        // Version Detection.
        public static final String VERSION_PARSING = "Version parsing";
        public static final String VERSION_JSON = "version.json source";
        public static final String REFLECTION = "Reflection source";

        // Entity API.
        public static final String ENTITY_SPAWNING = "Entity spawning";
        public static final String ENTITY_TICKING = "Entity ticking";
        public static final String ENTITY_RIDING = "Entity riding";
        public static final String FLUID_STORAGE = "Fluid storage";
        public static final String NETWORKING = "Networking";
        public static final String CLIENT_HOOKS = "Client hooks";
        public static final String SOUND = "Sound";
        public static final String BLOCK_COLLISIONS = "Block collisions";
        public static final String OBJ_MODELS = "OBJ models";
        public static final String ROTATION = "Yaw rotation";

        // Mod Lifecycle.
        public static final String STARTUP_ORDER = "Startup order";

        // Multipart API.
        public static final String ENTITY_PARTS = "Entity parts";
        public static final String PART_INTERACTIONS = "Part interactions";

        private Functionalities() {
        }
    }

    /**
     * Values of the Allure {@code @Issue} annotation. A value is a pull-request number: the link pattern in
     * {@code allure.properties} turns it into the pull-request URL.
     */
    public static final class Issues {

        public static final String PR_4 = "4";

        private Issues() {
        }
    }

    /** Values of JUnit's {@code @Tag}, which the Allure Jupiter adapter reads as the {@code tag} label. */
    public static final class Tags {

        public static final String PR_4 = "PR-" + Issues.PR_4;

        private Tags() {
        }
    }
}
