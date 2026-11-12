package lv.cebbys.mcmods.modularity.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
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
import lv.cebbys.mcmods.modularity.testing.allure.Functionality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Epics;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Features;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Functionalities;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Issues;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Stories;
import static lv.cebbys.mcmods.modularity.testing.allure.ModularityAllureConstants.Tags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rotations, entity parts and part interactions. */
@Epic(Epics.MODULARITY)
@Feature(Features.L1_UNIT)
@Story(Stories.MULTIPART_API)
@Issue(Issues.PR_4)
@Tag(Tags.PR_4)
class MultipartApiTest {

    @Test
    @Functionality(Functionalities.ROTATION)
    @DisplayName("Yaw rotation matches the Minecraft yaw convention")
    void yawRotationMatchesMinecraftYaw() {
        for (float yaw : new float[]{0, 30, 90, -135, 179}) {
            assertYawRotation(yaw);
        }
    }

    @Test
    @Functionality(Functionalities.ROTATION)
    @DisplayName("Rotations compose, invert, slerp and keep their heading")
    void rotationsComposeAndInvert() {
        Rotation rotation = composeYawAndPitch(40, -10);
        assertInverseRotatesBack(rotation);
        assertHeadingAndNose(rotation);
        assertSlerpWithItself(rotation, new Vec3(1, 2, 3));
        assertHalfYaw();
    }

    @Test
    @Functionality(Functionalities.ENTITY_PARTS)
    @DisplayName("Entity parts are validated and the part reach is computed")
    void partsAreValidatedAndReachIsComputed() {
        EntityPart body = bodyPart();
        assertReach(body);
        EntityDefinition definition = cartDefinition(body);
        assertParts(definition);
        assertPartReach(body, definition);
        assertDuplicatePartRejected(body);
        assertFlatPartRejected();
        assertNotMultipart();
    }

    @Test
    @Functionality(Functionalities.PART_INTERACTIONS)
    @DisplayName("A part interaction without an override calls the whole entity")
    void partInteractionsDefaultToTheWholeEntityAndCanBeOverridden() {
        List<String> calls = new ArrayList<>();
        EntityBehaviour behaviour = recordingBehaviour(calls);
        assertPartCallsTheEntity(behaviour, wheelPart());
        assertEquals(List.of("interact", "attacked 2.0"), calls, "the entity handled both calls");
    }

    @Step("Rotate the forward vector by yaw {0} and compare with Rotation.ofYaw")
    private void assertYawRotation(float yaw) {
        Vec3 forward = new Vec3(0, 0, 1);
        Rotation rotation = Rotation.ofYaw(yaw);
        assertVec(rotation.rotate(forward), forward.rotateYaw(yaw));
        assertEquals(yaw, rotation.yaw(), 1e-4);
    }

    @Step("Compose yaw {0} with pitch {1}")
    private Rotation composeYawAndPitch(float yaw, float pitch) {
        Rotation a = Rotation.ofYaw(yaw);
        Rotation tilt = new Rotation(Math.sin(Math.toRadians(pitch)), 0, 0, Math.cos(Math.toRadians(pitch)));
        return a.mul(tilt);
    }

    @Step("Invert the rotation and rotate the vector back")
    private void assertInverseRotatesBack(Rotation rotation) {
        Vec3 vector = new Vec3(1, 2, 3);
        assertVec(vector, rotation.inverse().rotate(rotation.rotate(vector)));
    }

    @Step("Slerp the rotation with itself by half and compare")
    private void assertSlerpWithItself(Rotation rotation, Vec3 vector) {
        assertVec(rotation.rotate(vector), rotation.slerp(rotation, 0.5).rotate(vector));
    }

    @Step("Check that the yaw stays 40 and that the nose points up")
    private void assertHeadingAndNose(Rotation rotation) {
        assertEquals(40, rotation.yaw(), 1e-4, "pitch keeps the heading");
        assertTrue(rotation.rotate(new Vec3(0, 0, 1)).y() > 0.3, "the nose points up");
    }

    @Step("Slerp from the identity to yaw 40 by half and expect yaw 20")
    private void assertHalfYaw() {
        assertEquals(20, Rotation.IDENTITY.slerp(Rotation.ofYaw(40), 0.5).yaw(), 1e-4);
    }

    @Step("Check that the part reach is the length of the part offset")
    private void assertReach(EntityPart body) {
        assertEquals(new Vec3(1, 1.5, 2).length(), body.reach(), 1e-9);
    }

    @Step("Check that the definition is multipart and lists the body and the wheel")
    private void assertParts(EntityDefinition definition) {
        assertTrue(definition.isMultipart(), "two parts make a multipart entity");
        assertEquals(List.of("body", "wheel"), definition.parts().stream().map(EntityPart::name).toList());
    }

    @Step("Check that the part reach is the reach of the body")
    private void assertPartReach(EntityPart body, EntityDefinition definition) {
        assertEquals(body.reach(), definition.partReach(), 1e-9, "the body reaches furthest");
    }

    @Step("Check that an empty definition is not multipart")
    private void assertNotMultipart() {
        assertFalse(EntityDefinition.builder().build().isMultipart());
    }

    @Step("Call the part interact and the part attack without an override")
    private void assertPartCallsTheEntity(EntityBehaviour behaviour, EntityPart wheel) {
        assertEquals(InteractionResult.SUCCESS, behaviour.interactPart(null, null, wheel),
                "the part interact calls the entity interact");
        assertTrue(behaviour.attackedPart(null, Optional.empty(), 2, wheel),
                "the part attack calls the entity attack");
    }

    @Step("Create the body part at (0, 1, 0) with size (1, 0.5, 2)")
    private EntityPart bodyPart() {
        return new EntityPart("body", new Vec3(0, 1, 0), new Vec3(1, 0.5, 2), true);
    }

    @Step("Create the wheel part at (1, 0.4, 1.5)")
    private EntityPart wheelPart() {
        return new EntityPart("wheel", new Vec3(1, 0.4, 1.5), new Vec3(0.2, 0.4, 0.4), false);
    }

    @Step("Build the entity definition with the body and the wheel")
    private EntityDefinition cartDefinition(EntityPart body) {
        return EntityDefinition.builder().part(body).part(wheelPart()).build();
    }

    @Step("Reject a definition with the same part twice")
    private void assertDuplicatePartRejected(EntityPart body) {
        assertThrows(IllegalArgumentException.class, () -> EntityDefinition.builder().part(body).part(body));
    }

    @Step("Reject a part with no height")
    private void assertFlatPartRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EntityPart("flat", Vec3.ZERO, new Vec3(1, 0, 1), true));
    }

    @Step("Create a behaviour that records its calls")
    private EntityBehaviour recordingBehaviour(List<String> calls) {
        return new EntityBehaviour() {
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
    }

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x(), actual.x(), 1e-9, actual.toString());
        assertEquals(expected.y(), actual.y(), 1e-9, actual.toString());
        assertEquals(expected.z(), actual.z(), 1e-9, actual.toString());
    }
}
