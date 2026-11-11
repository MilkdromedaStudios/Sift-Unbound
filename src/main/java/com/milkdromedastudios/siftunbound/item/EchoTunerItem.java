package com.milkdromedastudios.siftunbound.item;

import com.milkdromedastudios.siftunbound.portal.NoteBlockMachine;
import com.milkdromedastudios.siftunbound.portal.WardenPortal;
import com.milkdromedastudios.siftunbound.registry.ModCriteria;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The Echo Tuner hums the melody that wakes the Warden Portal (in Dungeons II a note block machine plays it).
 * Use it on (or inside) a reinforced-deepslate frame such as the one at the heart of every ancient city to light it
 * (costs durability). Used while sneaking, it just hums the melody and shows its notes, so it can be played on a
 * {@link com.milkdromedastudios.siftunbound.portal.NoteBlockMachine note block machine} instead.
 */
public class EchoTunerItem extends Item {
	public EchoTunerItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			NoteBlockMachine.hum(server, player.position());
			player.sendOverlayMessage(Component.translatable("message.sift_unbound.melody", NoteBlockMachine.melodyText()));
			player.getCooldowns().addCooldown(player.getItemInHand(hand), 60);
			if (player instanceof ServerPlayer serverPlayer) {
				ModCriteria.award(serverPlayer, ModCriteria.HUMMED_MELODY);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player sneaker = context.getPlayer();
		if (sneaker != null && sneaker.isSecondaryUseActive()) {
			return use(context.getLevel(), sneaker, context.getHand());
		}
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		Player player = context.getPlayer();
		BlockPos start = WardenPortal.isFrame(level.getBlockState(clicked)) ? clicked.relative(context.getClickedFace()) : clicked;
		if (level.isClientSide()) {
			return WardenPortal.find(level, start).isPresent() || WardenPortal.find(level, clicked).isPresent()
				? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		WardenPortal.Result result = WardenPortal.tryLight(level, start);
		if (result != WardenPortal.Result.LIT) {
			result = WardenPortal.tryLight(level, clicked);
		}
		if (result == WardenPortal.Result.LIT) {
			level.playSound(null, clicked, ModSounds.SIFT_PORTAL_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.playSound(null, clicked, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, 1.0F);
			if (level instanceof ServerLevel server) {
				server.sendParticles(ParticleTypes.SCULK_SOUL, clicked.getX() + 0.5, clicked.getY() + 1.0, clicked.getZ() + 0.5, 30, 1.0, 1.0, 1.0, 0.05);
			}
			level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, clicked);
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.sift_unbound.portal.lit"));
				context.getItemInHand().hurtAndBreak(1, player, context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
					? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
			}
			return InteractionResult.SUCCESS_SERVER;
		}
		level.playSound(null, clicked, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.6F, 0.5F);
		if (player != null) {
			player.sendOverlayMessage(Component.translatable("message.sift_unbound.portal.no_frame"));
		}
		return InteractionResult.FAIL;
	}
}
