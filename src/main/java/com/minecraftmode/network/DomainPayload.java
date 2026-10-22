package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.domain.Domains;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.joml.Vector3fc;

/**
 * Server -> client: domain {@code id} ({@code com.minecraftmode.domain.Domains}) is open at {@code center} with {@code radius} and
 * {@code style} (a {@code DomainStyle} ordinal) for {@code ticks} more; 0 ticks = it closed.
 */
public record DomainPayload(int id, Vector3fc center, float radius, int style, int ticks) implements CustomPacketPayload {
	public static final Type<DomainPayload> TYPE = new Type<>(MinecraftMode.id("domain"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DomainPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, DomainPayload::id,
		ByteBufCodecs.VECTOR3F, DomainPayload::center,
		ByteBufCodecs.FLOAT, DomainPayload::radius,
		ByteBufCodecs.VAR_INT, DomainPayload::style,
		ByteBufCodecs.VAR_INT, DomainPayload::ticks,
		DomainPayload::new
	);

	public static DomainPayload of(final Domains.Domain d, final int ticks) {
		return new DomainPayload(d.id(), d.center().toVector3f(), d.radius(), d.style().ordinal(), ticks);
	}

	@Override
	public Type<DomainPayload> type() {
		return TYPE;
	}
}
