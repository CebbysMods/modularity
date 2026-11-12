package lv.cebbys.modularity.gradle.allure;

/**
 * Values of the Allure labels of the L2 build-contract tests (docs/07, 7.11). This is the copy for the included
 * build: {@code modularity-gradle} cannot read the test fixtures of {@code modularity-api}, so the values it uses are
 * declared here. The values must match {@code ModularityAllureConstants}, or the two layers split in the report.
 *
 * <p>The report tree has four levels: epic, feature, story and functionality. The story is the concern. The
 * functionality is the behaviour inside that concern, and a behaviour gets one only when two or more tests prove
 * it; a single test sits directly under its story.
 *
 * <p>Every value is a compile-time constant, so it can stand inside an annotation. The class holds only strings,
 * so it needs no Allure dependency.
 */
public final class BuildAllureLabels {

    private BuildAllureLabels() {
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
     * layer number, so they read as plain names. The copy must match the test fixtures of {@code modularity-api}.
     */
    public static final class Features {

        /** Unit tests that need no Minecraft and no loader, for example the range or the smoke-log checks. */
        public static final String UNIT = "Unit Tests";
        /** Tests of the build contract: layering, table, metadata, universal jar. */
        public static final String INTEGRATION = "Integration Tests";

        private Features() {
        }
    }

    /** Values of the Allure {@code @Story} annotation: one per concern. */
    public static final class Stories {

        /** The layered sources of one platform: order, override and copy rules, and their errors. */
        public static final String LAYER_ASSEMBLY = "Layer Assembly";
        /** The platform table and the loader metadata generated from it. */
        public static final String PLATFORM_TABLE = "Platform Table and Metadata";
        /** The universal jar and its per-loader relocated class sets. */
        public static final String UNIVERSAL_JAR = "Universal Jar";
        /** The task graph of a version parent project. */
        public static final String VERSION_PLUGIN = "Version Plugin Tasks";
        /** The Quilt metadata that wraps nested jars. */
        public static final String QUILT_JAR = "Quilt Jar Metadata";
        /** Version and range values, a plain Java unit. */
        public static final String VERSION_RANGES = "Version Ranges";
        /** The smoke log checks, a plain Java unit. */
        public static final String SMOKE_CHECKS = "Smoke Log Checks";

        private Stories() {
        }
    }

    /**
     * Values of {@link Functionality}: the behaviour a test proves. A behaviour gets a constant only when two or
     * more tests prove it, so the report tree shows them under the story.
     */
    public static final class Functionalities {

        // Layer Assembly.
        public static final String LAYER_PRECEDENCE = "Layer precedence";
        public static final String LAYER_ERRORS = "Layer errors";

        // Platform Table and Metadata.
        public static final String GENERATED_METADATA = "Generated metadata";
        public static final String SOURCE_SCAN = "Source scan";

        // Universal Jar.
        public static final String RELOCATION = "Relocation";
        public static final String LOADER_TREES = "Loader trees in the jar";

        // Version Plugin Tasks.
        public static final String TASK_GRAPH = "Task graph";

        // Quilt Jar Metadata.
        public static final String NESTED_JARS = "Nested jar wrapping";

        private Functionalities() {
        }
    }

    /**
     * Values of the Allure {@code @Issue} annotation. A value is a pull-request number: the link pattern in
     * {@code allure.properties} turns it into the pull-request URL.
     */
    public static final class Issues {

        public static final String PR_1 = "1";
        public static final String PR_6 = "6";
        public static final String PR_7 = "7";
        public static final String PR_8 = "8";
        public static final String PR_10 = "10";
        public static final String PR_11 = "11";

        private Issues() {
        }
    }

    /** Values of JUnit's {@code @Tag}, which the Allure Jupiter adapter reads as the {@code tag} label. */
    public static final class Tags {

        public static final String PR_1 = "PR-" + Issues.PR_1;
        public static final String PR_6 = "PR-" + Issues.PR_6;
        public static final String PR_7 = "PR-" + Issues.PR_7;
        public static final String PR_8 = "PR-" + Issues.PR_8;
        public static final String PR_10 = "PR-" + Issues.PR_10;
        public static final String PR_11 = "PR-" + Issues.PR_11;

        private Tags() {
        }
    }
}
