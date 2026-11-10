package lv.cebbys.mcmods.modularity.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import lv.cebbys.mcmods.modularity.api.client.ClientContext;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.server.ServerContext;
import lv.cebbys.mcmods.modularity.testing.allure.Functionality;
import lv.cebbys.mcmods.modularity.testkit.TestGame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Functionalities;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The common, client and server stages of a mod and their order. */
@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.MOD_LIFECYCLE)
@Issue(Issues.PR_4)
@Tag(Tags.PR_4)
class LifecycleTest {
    private static final class RecordingMod
            implements ModularityModCommon, ModularityModClient, ModularityModServer {
        final List<String> calls = new ArrayList<>();

        @Override
        public void initialize(CommonContext common) {
            calls.add("common:" + common.modId());
        }

        @Override
        public void initialize(ClientContext client) {
            calls.add("client:" + client.modId());
        }

        @Override
        public void initialize(ServerContext server) {
            calls.add("server:" + server.modId());
        }
    }

    @Test
    @Functionality(Functionalities.STARTUP_ORDER)
    @DisplayName("A client run initialises the common stage, then the client stage")
    void clientRunsCommonThenClient() {
        RecordingMod mod = loadOnClient("example");
        assertCalls(mod, List.of("common:example", "client:example"));
    }

    @Test
    @Functionality(Functionalities.STARTUP_ORDER)
    @DisplayName("A dedicated server initialises the common stage, then the server stage")
    void dedicatedServerRunsCommonThenServer() {
        RecordingMod mod = loadOnServer("example");
        assertCalls(mod, List.of("common:example", "server:example"));
    }

    @Step("Load the mod \"{0}\" on a client")
    private RecordingMod loadOnClient(String modId) {
        RecordingMod mod = new RecordingMod();
        TestGame.create().load(modId, mod);
        return mod;
    }

    @Step("Load the mod \"{0}\" on a dedicated server")
    private RecordingMod loadOnServer(String modId) {
        RecordingMod mod = new RecordingMod();
        TestGame game = TestGame.create();
        game.testPlatform().side(Side.SERVER);
        game.load(modId, mod);
        return mod;
    }

    @Step("Check the call order {1}")
    private void assertCalls(RecordingMod mod, List<String> expected) {
        assertEquals(expected, mod.calls);
    }
}
