package lv.cebbys.mcmods.modularity.fabric.item;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemDefinition;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.item.ItemUseContext;
import lv.cebbys.mcmods.modularity.api.world.Direction;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import lv.cebbys.mcmods.modularity.fabric.Wrap;
import lv.cebbys.mcmods.modularity.fabric.compat.GameCompat;
import lv.cebbys.mcmods.modularity.fabric.compat.RegistryCompat;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import java.util.Optional;

/** Native item created from a portable {@link ItemDefinition}. */
public class ModularItem extends Item {
    private final ItemDefinition definition;

    public ModularItem(Id id, ItemDefinition definition) {
        super(RegistryCompat.itemProperties(id).stacksTo(definition.stackSize()));
        this.definition = definition;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemUseContext portable = new ItemUseContext() {
            @Override
            public Level level() {
                return Wrap.level(context.getLevel());
            }

            @Override
            public Optional<Player> player() {
                return Optional.ofNullable(context.getPlayer()).map(Wrap::player);
            }

            @Override
            public ItemStackRef stack() {
                return Wrap.stack(context.getItemInHand());
            }

            @Override
            public Pos clickedPos() {
                return Wrap.pos(context.getClickedPos());
            }

            @Override
            public Direction clickedFace() {
                return Wrap.direction(context.getClickedFace());
            }

            @Override
            public Vec3 clickLocation() {
                return Wrap.vec(context.getClickLocation());
            }
        };
        return switch (definition.behaviour().useOnBlock(portable)) {
            case PASS -> super.useOn(context);
            case SUCCESS -> GameCompat.success(context.getLevel().isClientSide());
            case CONSUME -> InteractionResult.CONSUME;
            case FAIL -> InteractionResult.FAIL;
        };
    }
}
