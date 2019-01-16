package me.prunt.restrictedcreative.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Bed.Part;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import me.prunt.restrictedcreative.Main;
import me.prunt.restrictedcreative.storage.DataHandler;
import me.prunt.restrictedcreative.utils.MaterialHandler;

public class BlockPistonListener implements Listener {
    private Main main;

    public BlockPistonListener(Main main) {
	this.main = main;
    }

    private Main getMain() {
	return this.main;
    }

    /*
     * Called when a piston retracts
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPull(BlockPistonRetractEvent e) {
	// No need to control blocks in disabled worlds
	if (getMain().getUtils().isDisabledWorld(e.getBlock().getWorld().getName()))
	    return;

	// Non-sticky piston doesn't pull anything
	if (!e.isSticky())
	    return;

	List<Block> newBlocks = new ArrayList<>();
	List<Block> oldBlocks = new ArrayList<>();
	List<BlockFace> blockfaces = getSurroundingBlockFaces(e.getDirection());

	if (Main.DEBUG)
	    System.out.println("onBlockPullBlocks: " + e.getBlocks().size());

	// Loop through pulled blocks
	for (Block b : e.getBlocks()) {
	    checkSurroundingBlocks(b, blockfaces);

	    if (!DataHandler.isTracked(b))
		break;

	    if (Main.DEBUG)
		System.out.println("onBlockPull: " + b.getType());

	    if (b.getPistonMoveReaction() == PistonMoveReaction.BREAK)
		DataHandler.breakBlock(b, null);

	    // If it's on top of another moving block
	    if (e.getBlocks().contains(b.getRelative(BlockFace.DOWN)) && MaterialHandler.needsBlockBelow(b))
		DataHandler.breakBlock(b, null);

	    // If it's attached to another moving block
	    if (MaterialHandler.getNeededFace(b) != null
		    && e.getBlocks().contains(b.getRelative(MaterialHandler.getNeededFace(b))))
		DataHandler.breakBlock(b, null);

	    oldBlocks.add(b);
	    newBlocks.add(b.getRelative(e.getDirection()));
	}

	// Remove old blocks first
	for (Block b : oldBlocks) {
	    DataHandler.removeTracking(b);
	}
	// Add new blocks afterwards
	for (Block b : newBlocks) {
	    DataHandler.setAsTracked(b);
	}
    }

    /*
     * Called when a piston extends
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPush(BlockPistonExtendEvent e) {
	// No need to control blocks in disabled worlds
	if (getMain().getUtils().isDisabledWorld(e.getBlock().getWorld().getName()))
	    return;

	List<Block> newBlocks = new ArrayList<>();
	List<Block> oldBlocks = new ArrayList<>();
	List<BlockFace> blockfaces = getSurroundingBlockFaces(e.getDirection());

	if (Main.DEBUG)
	    System.out.println("onBlockPushBlocks: " + e.getBlocks().size());

	// Loop through pushed blocks
	for (Block b : e.getBlocks()) {
	    checkSurroundingBlocks(b, blockfaces);

	    if (!DataHandler.isTracked(b))
		break;

	    if (Main.DEBUG)
		System.out.println("onBlockPush: " + b.getType());

	    BlockData bd = b.getBlockData();

	    /* Bed */
	    if (bd instanceof Bed) {
		Bed bed = (Bed) bd;
		Block block = bed.getPart() == Part.FOOT ? b.getRelative(bed.getFacing())
			: b.getRelative(bed.getFacing().getOppositeFace());

		if (Main.DEBUG)
		    System.out.println("bedPush: " + b.getType() + " " + block.getType());

		DataHandler.breakBlock(b, null, false);
		DataHandler.breakBlock(block, null, false);
		break;
	    }

	    if (b.getPistonMoveReaction() == PistonMoveReaction.BREAK)
		DataHandler.breakBlock(b, null);

	    // If it's on top of another moving block
	    if (e.getBlocks().contains(b.getRelative(BlockFace.DOWN)) && MaterialHandler.needsBlockBelow(b))
		DataHandler.breakBlock(b, null);

	    // If it's attached to another moving block
	    if (MaterialHandler.getNeededFace(b) != null
		    && e.getBlocks().contains(b.getRelative(MaterialHandler.getNeededFace(b))))
		DataHandler.breakBlock(b, null);

	    oldBlocks.add(b);
	    newBlocks.add(b.getRelative(e.getDirection()));
	}

	// Remove old blocks first
	for (Block b : oldBlocks) {
	    DataHandler.removeTracking(b);
	}
	// Add new blocks afterwards
	for (Block b : newBlocks) {
	    DataHandler.setAsTracked(b);
	}
    }

    private List<BlockFace> getSurroundingBlockFaces(BlockFace direction) {
	// Gets the non-pushed/-pulled faces
	BlockFace bf1 = null, bf2 = null, bf3 = null, bf4 = null, bf5 = direction.getOppositeFace();
	
	switch (direction.name()) {
	case ("UP"):
	case ("DOWN"):
	    bf1 = BlockFace.WEST;
	    bf2 = BlockFace.EAST;
	    bf3 = BlockFace.NORTH;
	    bf4 = BlockFace.SOUTH;
	    break;
	case ("NORTH"):
	case ("SOUTH"):
	    bf1 = BlockFace.WEST;
	    bf2 = BlockFace.EAST;
	    bf3 = BlockFace.UP;
	    bf4 = BlockFace.DOWN;
	    break;
	case ("EAST"):
	case ("WEST"):
	    bf1 = BlockFace.NORTH;
	    bf2 = BlockFace.SOUTH;
	    bf3 = BlockFace.UP;
	    bf4 = BlockFace.DOWN;
	    break;
	}

	return Arrays.asList(bf1, bf2, bf3, bf4, bf5);
    }

    private void checkSurroundingBlocks(Block b, List<BlockFace> sides) {
	for (BlockFace bf : sides) {
	    Block bl = b.getRelative(bf);

	    // Checks if the surrounding block is placed in creative
	    if (!DataHandler.isTracked(bl))
		continue;

	    // If it's attached to the moving block
	    if (bl.getFace(b) == BlockFace.DOWN && MaterialHandler.needsBlockBelow(bl)
		    || bl.getFace(b) == MaterialHandler.getNeededFace(bl))
		DataHandler.breakBlock(bl, null);
	}
    }
}
