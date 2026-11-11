package com.milkdromedastudios.siftunbound.mixin;

import com.milkdromedastudios.siftunbound.portal.NoteBlockMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets note blocks near a Warden Portal frame play the Sift's melody (see {@link NoteBlockMachine}). */
@Mixin(NoteBlock.class)
abstract class NoteBlockMixin {
	/** Runs only when the note actually sounds (right where vanilla sends the block event). */
	@Inject(method = "playNote", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;blockEvent(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;II)V"))
	private void siftunbound$noteBlockMachine(@Nullable Entity source, BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
		if (level instanceof ServerLevel server) {
			NoteBlockMachine.onNotePlayed(server, pos, state.getValue(NoteBlock.NOTE));
		}
	}
}
