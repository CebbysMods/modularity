package lv.cebbys.mcmods.modularity.testkit;

import lv.cebbys.mcmods.modularity.api.block.BlockStateRef;
import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Player whose tool effectiveness is configured per block id. */
public final class TestPlayer implements Player {
    private final Map<Id, Float> speeds = new HashMap<>();
    private final Set<Id> correctTool = new HashSet<>();
    private boolean creative;
    private ItemStackRef mainHand = TestItemStack.EMPTY;

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

    public TestPlayer holding(ItemStackRef stack) {
        this.mainHand = stack;
        return this;
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
    public ItemStackRef mainHandItem() {
        return mainHand;
    }

    @Override
    public Object handle() {
        return this;
    }
}
