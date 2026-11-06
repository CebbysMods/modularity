package lv.cebbys.mcmods.modularity.platform.item;

import lv.cebbys.mcmods.modularity.api.core.Id;
import lv.cebbys.mcmods.modularity.api.item.ItemDefinition;
import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.item.ItemUseContext;
import lv.cebbys.mcmods.modularity.api.world.Direction;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.api.world.Vec3;
import lv.cebbys.mcmods.modularity.platform.Wrap;
import lv.cebbys.mcmods.modularity.platform.compat.GameCompat;
import lv.cebbys.mcmods.modularity.platform.compat.RegistryCompat;
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
        var result = definition.behaviour().useOnBlock(portable);
        return result == lv.cebbys.mcmods.modularity.api.core.InteractionResult.PASS ? super.useOn(context)
                : GameCompat.result(result, context.getLevel().isClientSide());
    }
}
