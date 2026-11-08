package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.transfer.FluidTank;
import lv.cebbys.mcmods.modularity.api.world.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Player whose tool effectiveness is configured per block id. */
public final class TestPlayer extends TestEntity implements Player {
    private static final Map<Id, Id> FILLED_BUCKETS = Map.of(
            Id.minecraft("lava_bucket"), Id.minecraft("lava"),
            Id.minecraft("water_bucket"), Id.minecraft("water"));

    private final Map<Id, Float> speeds = new HashMap<>();
    private final Set<Id> correctTool = new HashSet<>();
    private final List<String> messages = new ArrayList<>();
    private boolean creative;
    private boolean sneaking;
    private ItemStackRef mainHand = TestItemStack.EMPTY;

    public TestPlayer() {
        super(UUID.randomUUID(), Id.minecraft("player"), null);
    }

    public TestPlayer speed(Id block, float speed) {
        speeds.put(block, speed);
        return this;
    }

    public TestPlayer correctToolFor(Id... blocks) {
        correctTool.addAll(Set.of(blocks));
        return this;
    }

    public TestPlayer creative(boolean creative) {
        this.creative = creative;
        return this;
    }

    public TestPlayer sneaking(boolean sneaking) {
        this.sneaking = sneaking;
        return this;
    }

    public TestPlayer holding(ItemStackRef stack) {
        this.mainHand = stack;
        return this;
    }

    /** Messages shown with {@link #displayMessage}. */
    public List<String> messages() {
        return messages;
    }

    @Override
    public String name() {
        return "TestPlayer";
    }

    @Override
    public float destroySpeed(BlockStateRef state) {
        return speeds.getOrDefault(state.block().id(), 1.0F);
    }

    @Override
    public boolean hasCorrectToolForDrops(BlockStateRef state) {
        return correctTool.contains(state.block().id());
    }

    @Override
    public boolean isCreative() {
        return creative;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isShiftKeyDown() {
        return sneaking;
    }

    @Override
    public ItemStackRef mainHandItem() {
        return mainHand;
    }

    /** Vanilla buckets: a filled bucket gives 1000 mB and becomes an empty bucket. */
    @Override
    public long fillFromHeldItem(FluidTank tank) {
        Id fluid = FILLED_BUCKETS.get(mainHand.itemId());
        if (fluid == null || tank.fill(fluid, 1000, true) != 1000) {
            return 0;
        }
        tank.fill(fluid, 1000, false);
        if (!creative) {
            mainHand = TestItemStack.item(Id.minecraft("bucket"));
        }
        return 1000;
    }

    @Override
    public void displayMessage(String message, boolean actionBar) {
        messages.add(message);
    }
}
