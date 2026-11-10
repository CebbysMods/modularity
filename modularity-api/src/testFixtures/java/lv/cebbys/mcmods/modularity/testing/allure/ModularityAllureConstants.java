package lv.cebbys.mcmods.modularity.testing.allure;

/**
 * Values of the Allure labels the test pyramid uses (docs/07, 7.11). One child class per annotation, so a report
 * filter names one constant and a renamed label is edited once.
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
        public static final String L1_UNIT = "L1 unit tests";
        /** Tests of the build contract: layering, table, metadata, universal jar. */
        public static final String L2_BUILD_CONTRACT = "L2 build contract";
        /** One test per smoke run, with the log, the summary and the screenshot attached. */
        public static final String L3_SMOKE = "L3 in-game smoke";
        /** The shipped jar on every loader (docs/07, PR-13). */
        public static final String L4_PRODUCTION_JAR = "L4 production jar";

        private Features() {
        }
    }

    /** Values of the Allure {@code @Story} annotation: one per concern inside a layer. */
    public static final class Stories {

        public static final String API_VERSION = "API version";
        public static final String RUNTIME_DETECTION = "Runtime detection";
        public static final String TESTKIT_CORE = "Test-kit core";
        public static final String TESTKIT_ENTITIES = "Test-kit entities";
        public static final String TESTKIT_LIFECYCLE = "Test-kit lifecycle";
        public static final String TESTKIT_MULTIPART = "Test-kit multipart";

        private Stories() {
        }
    }

    /** Values of the Allure {@code @Issue} annotation: one requirement id per value (docs/00). */
    public static final class Issues {

        public static final String PR_9 = "PR-9";

        private Issues() {
        }
    }

    /** Values of JUnit's {@code @Tag}, which the Allure Jupiter adapter reads as the {@code tag} label. */
    public static final class Tags {

        public static final String PR_9 = Issues.PR_9;

        private Tags() {
        }
    }
}
