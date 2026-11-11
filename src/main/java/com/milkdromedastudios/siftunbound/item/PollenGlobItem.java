package com.milkdromedastudios.siftunbound.item;

import com.milkdromedastudios.siftunbound.entity.projectile.PollenGlob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A Pollinator's pollen glob: thrown, it sticks where it lands and bursts into slowing yellow goo three seconds later. */
public class PollenGlobItem extends Item {
	public PollenGlobItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.6F);
		if (level instanceof ServerLevel server) {
			PollenGlob glob = new PollenGlob(server, player);
			glob.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.0F, 1.0F);
			server.addFreshEntity(glob);
		}
		player.getCooldowns().addCooldown(stack, 20);
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
