package lv.cebbys.mcmods.modularity.api.block;

import lv.cebbys.mcmods.modularity.api.item.ItemStackRef;
import lv.cebbys.mcmods.modularity.api.world.Direction;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.api.world.Vec3;

import java.util.Optional;

/**
 * Someone tries to place a block. Also used for non-player placement checks (falling blocks,
 * dispensers), in which case {@link #player()} is empty and {@link #stack()} may be empty.
 */
public interface PlacementContext {
    Level level();

    /** Position where the block would end up. */
    Pos pos();

    Direction clickedFace();

    Vec3 clickLocation();

    ItemStackRef stack();

    Optional<Player> player();

    /** Block state currently at {@link #pos()}. */
    default BlockStateRef existingState() {
        return level().blockState(pos());
    }
}
