package lv.cebbys.mcmods.modularity.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import lv.cebbys.mcmods.modularity.api.client.ClientContext;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.server.ServerContext;
import lv.cebbys.mcmods.modularity.testkit.TestGame;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.TESTKIT_LIFECYCLE)
@Issue(Issues.PR_9)
@Tag(Tags.PR_9)
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
    void clientRunsCommonThenClient() {
        RecordingMod mod = new RecordingMod();
        TestGame.create().load("example", mod);
        assertEquals(List.of("common:example", "client:example"), mod.calls);
    }

    @Test
    void dedicatedServerRunsCommonThenServer() {
        RecordingMod mod = new RecordingMod();
        TestGame game = TestGame.create();
        game.testPlatform().side(Side.SERVER);
        game.load("example", mod);
        assertEquals(List.of("common:example", "server:example"), mod.calls);
    }
}
