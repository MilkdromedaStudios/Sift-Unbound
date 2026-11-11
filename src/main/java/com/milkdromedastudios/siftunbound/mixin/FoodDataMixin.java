package com.milkdromedastudios.siftunbound.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.milkdromedastudios.siftunbound.world.SiftTides;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The Sift's tides change natural regeneration: none during the Endure Tide, 50% faster during the Thrive Tide. */
@Mixin(FoodData.class)
abstract class FoodDataMixin {
	/** During the Endure Tide in the Sift, natural regeneration is off (and burns no food), as if the game rule were. */
	@WrapOperation(method = "tick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
	private Object siftunbound$endureTide(GameRules rules, GameRule<?> rule, Operation<Object> original, ServerPlayer player) {
		Object value = original.call(rules, rule);
		if (rule == GameRules.NATURAL_HEALTH_REGENERATION && SiftTides.blocksNaturalHealing(player)) {
			return Boolean.FALSE;
		}
		return value;
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
	private void siftunbound$thriveTide(ServerPlayer player, float amount, Operation<Void> original) {
		original.call(player, SiftTides.scaleNaturalHealing(player, amount));
	}
}
