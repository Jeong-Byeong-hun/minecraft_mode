package com.minecraftmode.residence;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Land deed: used in the residence it claims the chunk the player stands in, from the block under their feet up 16 blocks
 * ({@link LandClaims#claim}), and is used up. Used while sneaking (anywhere) it opens the land screen. Bought with merit from the bounty clerk (dearer each time, {@code Bounties.DEED_MERIT}).
 */
public class LandDeedItem extends Item {
	public LandDeedItem(final Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (sp.isShiftKeyDown()) {
			LandOffice.open(sp);
			return InteractionResult.SUCCESS;
		}
		if (!ResidenceDimension.is(server)) {
			LandOffice.tell(sp, "wrong_world", ChatFormatting.RED);
			return InteractionResult.FAIL;
		}
		LandClaims claims = LandClaims.get(server.getServer());
		BlockPos feet = sp.blockPosition();
		LandClaims.Result result = claims.claim(sp.getUUID(), sp.getPlainTextName(), feet, server);
		if (result != LandClaims.Result.CLAIMED) {
			sp.sendSystemMessage(Component.translatable(result.key(), LandClaims.MAX_CLAIMS).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		LandClaims.Claim claim = claims.at(feet.below());
		player.getItemInHand(hand).consume(1, player);
		LandOffice.tell(sp, "claimed", ChatFormatting.GREEN, claim.chunkX() << 4, (claim.chunkX() << 4) + 15, claim.chunkZ() << 4, (claim.chunkZ() << 4) + 15);
		LandOffice.outline(server, claim);
		return InteractionResult.SUCCESS;
	}
}
