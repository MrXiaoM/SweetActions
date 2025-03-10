package top.mrxiaom.sweet.actions.func.entry;

import org.bukkit.Location;

import java.util.Objects;

public class BlockLoc {
    public int x;
    public int y;
    public int z;

    public BlockLoc(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockLoc)) return false;
        BlockLoc blockLoc = (BlockLoc) o;
        return x == blockLoc.x && y == blockLoc.y && z == blockLoc.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    public static BlockLoc of(Location loc) {
        return new BlockLoc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    public static BlockLoc of(Location loc, int yOffset) {
        return new BlockLoc(loc.getBlockX(), loc.getBlockY() + yOffset, loc.getBlockZ());
    }
}
