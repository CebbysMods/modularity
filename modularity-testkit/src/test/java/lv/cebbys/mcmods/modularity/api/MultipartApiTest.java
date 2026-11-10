package lv.cebbys.mcmods.modularity.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import lv.cebbys.mcmods.modularity.api.core.InteractionResult;
import lv.cebbys.mcmods.modularity.api.entity.EntityBehaviour;
import lv.cebbys.mcmods.modularity.api.entity.EntityContext;
import lv.cebbys.mcmods.modularity.api.entity.EntityDefinition;
import lv.cebbys.mcmods.modularity.api.entity.EntityPart;
import lv.cebbys.mcmods.modularity.api.entity.EntityRef;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.api.world.Rotation;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Orientation and multipart entity API. */
@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.TESTKIT_MULTIPART)
@Issue(Issues.PR_9)
@Tag(Tags.PR_9)
class MultipartApiTest {
    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x(), actual.x(), 1e-9, actual.toString());
        assertEquals(expected.y(), actual.y(), 1e-9, actual.toString());
        assertEquals(expected.z(), actual.z(), 1e-9, actual.toString());
    }

    @Test
    void yawRotationMatchesMinecraftYaw() {
        Vec3 forward = new Vec3(0, 0, 1);
        for (float yaw : new float[]{0, 30, 90, -135, 179}) {
            assertVec(forward.rotateYaw(yaw), Rotation.ofYaw(yaw).rotate(forward));
            assertEquals(yaw, Rotation.ofYaw(yaw).yaw(), 1e-4);
        }
    }

    @Test
    void rotationsComposeAndInvert() {
        Rotation a = Rotation.ofYaw(40);
        Rotation pitch = new Rotation(Math.sin(Math.toRadians(-10)), 0, 0, Math.cos(Math.toRadians(-10)));
        Rotation r = a.mul(pitch);
        Vec3 v = new Vec3(1, 2, 3);
        assertVec(v, r.inverse().rotate(r.rotate(v)));
        assertEquals(40, r.yaw(), 1e-4, "pitch keeps the heading");
        assertTrue(r.rotate(new Vec3(0, 0, 1)).y() > 0.3, "nose up");
        assertVec(r.rotate(v), r.slerp(r, 0.5).rotate(v));
        assertEquals(20, Rotation.IDENTITY.slerp(Rotation.ofYaw(40), 0.5).yaw(), 1e-4);
    }

    @Test
    void partsAreValidatedAndReachIsComputed() {
        EntityPart body = new EntityPart("body", new Vec3(0, 1, 0), new Vec3(1, 0.5, 2), true);
        assertEquals(new Vec3(1, 1.5, 2).length(), body.reach(), 1e-9);
        var def = EntityDefinition.builder().part(body)
                .part(new EntityPart("wheel", new Vec3(1, 0.4, 1.5), new Vec3(0.2, 0.4, 0.4), false)).build();
        assertTrue(def.isMultipart());
        assertEquals(List.of("body", "wheel"), def.parts().stream().map(EntityPart::name).toList());
        assertEquals(body.reach(), def.partReach(), 1e-9, "the body reaches furthest");
        assertThrows(IllegalArgumentException.class, () -> EntityDefinition.builder().part(body).part(body));
        assertThrows(IllegalArgumentException.class, () -> new EntityPart("flat", Vec3.ZERO, new Vec3(1, 0, 1), true));
        assertTrue(!EntityDefinition.builder().build().isMultipart());
    }

    @Test
    void partInteractionsDefaultToTheWholeEntityAndCanBeOverridden() {
        List<String> calls = new ArrayList<>();
        EntityBehaviour plain = new EntityBehaviour() {
            @Override
            public InteractionResult interact(EntityContext ctx, Player player) {
                calls.add("interact");
                return InteractionResult.SUCCESS;
            }

            @Override
            public boolean attacked(EntityContext ctx, Optional<EntityRef> attacker, float amount) {
                calls.add("attacked " + amount);
                return true;
            }
        };
        EntityPart wheel = new EntityPart("wheel", Vec3.ZERO, new Vec3(0.5, 0.5, 0.5), false);
        assertEquals(InteractionResult.SUCCESS, plain.interactPart(null, null, wheel));
        assertTrue(plain.attackedPart(null, Optional.empty(), 2, wheel));
        assertEquals(List.of("interact", "attacked 2.0"), calls);
    }
}
