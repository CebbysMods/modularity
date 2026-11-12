package lv.cebbys.mcmods.modularity.testing.allure;

/**
 * Values of the Allure labels the test pyramid uses (docs/07, 7.11). One child class per label, so a report
 * filter names one constant and a renamed label is edited once.
 *
 * <p>The report tree has four levels: epic, feature, story and functionality. The epic is always Modularity.
 * The feature is the layer of the test pyramid. The story is the concern, for example Version Detection. The
 * functionality is the behaviour inside that concern, for example Source ordering. A behaviour gets a label
 * only when two or more tests prove it; a single test sits directly under its story, so no group holds one
 * test.
 *
 * <p>A {@code @Step} method takes only text, numbers and readable values as parameters, because Allure shows
 * every parameter in the report. The objects a test creates live in fields of the test instance.
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

    /**
     * Values of the Allure {@code @Feature} annotation: one per layer of the test pyramid. The values carry no
     * layer number, so they read as plain names.
     */
    public static final class Features {

        /** Unit tests that need no Minecraft and no loader. */
        public static final String UNIT = "Unit Tests";
        /** Tests of the build contract: layering, table, metadata, universal jar. */
        public static final String INTEGRATION = "Integration Tests";
        /** One test per smoke run, with the log, the summary and the screenshot attached. */
        public static final String END_TO_END = "End to End Tests";
        /** The shipped jar on every loader (docs/07, PR-13). */
        public static final String PRODUCTION_JAR = "Production Jar";

        private Features() {
        }
    }

    /** Values of the Allure {@code @Story} annotation: one per concern. */
    public static final class Stories {

        /** Minecraft and platform versions, ranges and versioned code. */
        public static final String VERSION_MODEL = "Version Model API";
        /** Namespaced ids, block definitions and data codecs. */
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
     * Values of {@link Functionality}: the behaviour a test proves. A behaviour gets a constant only when two or
     * more tests prove it, so the report tree shows them under the story, for example Platform Detection &gt;
     * Source ordering &gt; test.
     */
    public static final class Functionalities {

        // Version Model API.
        public static final String MINECRAFT_PARSING = "Minecraft version parsing";
        public static final String PLATFORM_PARSING = "Platform version parsing";
        public static final String VERSION_COMPARISON = "Version comparison";

        // Platform Detection.
        public static final String MARKER_CLASSES = "Loader marker classes";
        public static final String MARKER_PRECEDENCE = "Marker precedence";
        public static final String SOURCE_ORDERING = "Source ordering";
        public static final String SOURCE_FAILURES = "Source failures";

        // Version Detection.
        public static final String VERSION_JSON = "version.json source";

        // Mod Lifecycle.
        public static final String STARTUP_ORDER = "Startup order";

        // Multipart API.
        public static final String ROTATION = "Yaw rotation";

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
