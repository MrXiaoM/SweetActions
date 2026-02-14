package top.mrxiaom.sweet.actions.func.block;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Objects;

public class BlockLoc {
    private int x;
    private int y;
    private int z;

    public BlockLoc(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int x() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int y() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int z() {
        return z;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void x(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void y(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void z(int z) {
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

    public static BlockLoc of(Block block) {
        return of(block.getLocation());
    }
    public static BlockLoc of(Block block, boolean underfoot) {
        return of(block.getLocation(), underfoot);
    }
    public static BlockLoc of(Location loc) {
        return new BlockLoc(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    public static BlockLoc of(Location loc, boolean underfoot) {
        if (underfoot) {
            double y = loc.getY();
            double blockY = Math.floor(y);
            if (y == blockY) {
                return new BlockLoc(loc.getBlockX(),  (int) blockY - 1, loc.getBlockZ());
            }
            return new BlockLoc(loc.getBlockX(), (int) blockY, loc.getBlockZ());
        }
        return of(loc);
    }
}
