package lv.cebbys.mcmods.modularity.api;

import lv.cebbys.mcmods.modularity.api.client.ClientContext;
import lv.cebbys.mcmods.modularity.api.core.Side;
import lv.cebbys.mcmods.modularity.api.server.ServerContext;
import lv.cebbys.mcmods.modularity.testkit.TestGame;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifecycleTest {
    private static final class RecordingMod implements ModularMod {
        final List<String> calls = new ArrayList<>();

        @Override
        public void initCommon(ModContext mod) {
            calls.add("common:" + mod.modId());
        }

        @Override
        public void initClient(ClientContext client) {
            calls.add("client:" + client.modId());
        }

        @Override
        public void initServer(ServerContext server) {
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
