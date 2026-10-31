package lv.cebbys.mcmods.modularity.api.item;

import lv.cebbys.mcmods.modularity.api.world.Direction;
import lv.cebbys.mcmods.modularity.api.world.Level;
import lv.cebbys.mcmods.modularity.api.world.Player;
import lv.cebbys.mcmods.modularity.api.world.Pos;
import lv.cebbys.mcmods.modularity.api.world.Vec3;

import java.util.Optional;

/** An item is used on a block. */
public interface ItemUseContext {
    Level level();

    Optional<Player> player();

    ItemStackRef stack();

    Pos clickedPos();

    Direction clickedFace();

    Vec3 clickLocation();
}
