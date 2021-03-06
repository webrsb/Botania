/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 * 
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 * 
 * File Created @ [May 15, 2015, 6:55:34 PM (GMT)]
 */
package vazkii.botania.common.item.equipment.tool.terrasteel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.item.ISequentialBreaker;
import vazkii.botania.client.core.helper.IconHelper;
import vazkii.botania.common.item.ItemTemperanceStone;
import vazkii.botania.common.item.equipment.tool.ToolCommons;
import vazkii.botania.common.item.equipment.tool.manasteel.ItemManasteelAxe;
import vazkii.botania.common.item.relic.ItemLokiRing;
import vazkii.botania.common.lib.LibItemNames;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemTerraAxe extends ItemManasteelAxe implements ISequentialBreaker {

	private static final int MANA_PER_DAMAGE = 100;
	private static Map<Integer, List<BlockSwapper>> blockSwappers = new HashMap();

	IIcon iconOn, iconOff;

	public ItemTerraAxe() {
		super(BotaniaAPI.terrasteelToolMaterial, LibItemNames.TERRA_AXE);
		FMLCommonHandler.instance().bus().register(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister par1IconRegister) {
		iconOn = IconHelper.forItem(par1IconRegister, this, 0);
		iconOff = IconHelper.forItem(par1IconRegister, this, 1);
	}

	@Override
	public IIcon getIconFromDamage(int p_77617_1_) {
		return iconOn;
	}

	@Override
	public IIcon getIcon(ItemStack stack, int renderPass, EntityPlayer player, ItemStack usingItem, int useRemaining) {
		return shouldBreak(player) ? iconOn : iconOff;
	}

	public boolean shouldBreak(EntityPlayer player) {
		return !player.isSneaking() && !ItemTemperanceStone.hasTemperanceActive(player);
	}

	@Override
	public boolean onBlockStartBreak(ItemStack stack, int x, int y, int z, EntityPlayer player) {
		MovingObjectPosition raycast = ToolCommons.raytraceFromEntity(player.worldObj, player, true, 10);
		if(raycast != null) {
			breakOtherBlock(player, stack, x, y, z, x, y, z, raycast.sideHit);
			ItemLokiRing.breakOnAllCursors(player, this, stack, x, y, z, raycast.sideHit);
		}

		return false;
	}

	@Override
	public int getManaPerDamage() {
		return MANA_PER_DAMAGE;
	}

	@Override
	public void breakOtherBlock(EntityPlayer player, ItemStack stack, int x, int y, int z, int originX, int originY, int originZ, int side) {
		if(shouldBreak(player)) {
			ChunkCoordinates coords = new ChunkCoordinates(x, y, z);
			addBlockSwapper(player.worldObj, player, stack, coords, coords, 32, false, true, new ArrayList());
		}
	}

	@Override
	public boolean disposeOfTrashBlocks(ItemStack stack) {
		return false;
	}

	@SubscribeEvent
	public void onTickEnd(TickEvent.WorldTickEvent event) {
		if(event.phase == Phase.END) {
			int dim = event.world.provider.dimensionId;
			if(blockSwappers.containsKey(dim)) {
				List<BlockSwapper> swappers = blockSwappers.get(dim);
				List<BlockSwapper> swappersSafe = new ArrayList(swappers);
				swappers.clear();

				for(BlockSwapper s : swappersSafe)
					if(s != null)
						s.tick();
			}
		}
	}

	private static BlockSwapper addBlockSwapper(World world, EntityPlayer player, ItemStack stack, ChunkCoordinates origCoords, ChunkCoordinates coords, int steps, boolean leaves, boolean force, List<String> posChecked) {
		BlockSwapper swapper = new BlockSwapper(world, player, stack, origCoords, coords, steps, leaves, force, posChecked);

		int dim = world.provider.dimensionId;
		if(!blockSwappers.containsKey(dim))
			blockSwappers.put(dim, new ArrayList());
		blockSwappers.get(dim).add(swapper);

		return swapper;
	}

	private static class BlockSwapper {

		final World world;
		final EntityPlayer player;
		final ItemStack stack;
		final ChunkCoordinates origCoords;
		final int steps;
		final ChunkCoordinates coords;
		final boolean leaves;
		final boolean force;
		final List<String> posChecked;
		BlockSwapper(World world, EntityPlayer player, ItemStack stack, ChunkCoordinates origCoords, ChunkCoordinates coords, int steps, boolean leaves, boolean force, List<String> posChecked) {
			this.world = world;
			this.player = player;
			this.stack = stack;
			this.origCoords = origCoords;
			this.coords = coords;
			this.steps = steps;
			this.leaves = leaves;
			this.force = force;
			this.posChecked = posChecked;
		}

		void tick() {
			Block blockat = world.getBlock(coords.posX, coords.posY, coords.posZ);
			if(!force && blockat.isAir(world, coords.posX, coords.posY, coords.posZ))
				return;

			ToolCommons.removeBlockWithDrops(player, stack, world, coords.posX, coords.posY, coords.posZ, origCoords.posX, origCoords.posY, origCoords.posZ, null, ToolCommons.materialsAxe, EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, stack) > 0, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, stack), 0F, false, !leaves);

			if(steps == 0)
				return;

			for(int i = 0; i < 3; i++)
				for(int j = 0; j < 3; j++)
					for(int k = 0; k < 3; k++) {
						int x = coords.posX + i - 1;
						int y = coords.posY + j - 1;
						int z = coords.posZ + k - 1;
						String pstr = posStr(x, y, z);
						if(posChecked.contains(pstr))
							continue;

						Block block = world.getBlock(x, y, z);
						boolean log = block.isWood(world, x, y, z);
						boolean leaf = block.isLeaves(world, x, y, z);
						if(log || leaf) {
							int steps = this.steps - 1;
							steps = leaf ? leaves ? steps : 3 : steps;
							addBlockSwapper(world, player, stack, origCoords, new ChunkCoordinates(x, y, z), steps, leaf, false, posChecked);
							posChecked.add(pstr);
						}
					}
		}

		String posStr(int x, int y, int z) {
			return x + ":" + y + ":" + z;
		}
	}

}
