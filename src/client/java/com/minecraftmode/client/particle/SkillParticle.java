package com.minecraftmode.client.particle;

import com.minecraftmode.job.skill.Fx;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * One class for every skill sprite; {@link Fx.Kind} decides how it moves, grows and fades. Sprites
 * are white/grey and tinted with the option's color. Most kinds glow (full brightness).
 */
public class SkillParticle extends SingleQuadParticle {
	private final Fx.Kind kind;
	private final float baseSize;
	private final float spin;
	private final float phase;

	protected SkillParticle(
		final ClientLevel level, final double x, final double y, final double z, final double xd, final double yd, final double zd, final SpriteSet sprites,
		final Fx.Kind kind
	) {
		super(level, x, y, z, sprites.get(level.getRandom()));
		this.kind = kind;
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.hasPhysics = false;
		this.friction = 0.9F;
		this.phase = this.random.nextFloat() * Mth.TWO_PI;
		float spin = (this.random.nextFloat() - 0.5F) * 0.3F;
		switch (kind) {
			case SPARK -> {
				this.lifetime = 10 + this.random.nextInt(8);
				this.quadSize = 0.10F + this.random.nextFloat() * 0.06F;
				this.friction = 0.82F;
			}
			case SLASH -> {
				// short: a sword wave's trail of slashes looked many blocks longer than the wave that hits
				this.lifetime = 4 + this.random.nextInt(2);
				this.quadSize = 0.55F + this.random.nextFloat() * 0.2F;
				this.roll = this.random.nextFloat() * Mth.TWO_PI;
				spin = 0.0F;
			}
			case ORB -> {
				this.lifetime = 12 + this.random.nextInt(10);
				this.quadSize = 0.16F + this.random.nextFloat() * 0.08F;
				this.friction = 0.88F;
			}
			case RING -> {
				this.lifetime = 14;
				this.quadSize = 0.3F;
				spin = 0.0F;
			}
			case RUNE -> {
				this.lifetime = 30;
				this.quadSize = 1.1F;
				spin = 0.06F;
			}
			case SHARD -> {
				this.lifetime = 20 + this.random.nextInt(12);
				this.quadSize = 0.12F + this.random.nextFloat() * 0.06F;
				this.gravity = 0.7F;
				this.hasPhysics = true;
				this.friction = 0.96F;
			}
			case SMOKE -> {
				this.lifetime = 24 + this.random.nextInt(16);
				this.quadSize = 0.25F + this.random.nextFloat() * 0.1F;
				this.gravity = -0.03F;
				this.friction = 0.92F;
				spin *= 0.3F;
			}
			case PETAL, FEATHER -> {
				this.lifetime = 40 + this.random.nextInt(20);
				this.quadSize = 0.10F + this.random.nextFloat() * 0.05F;
				this.gravity = 0.06F;
				this.friction = 0.95F;
				this.hasPhysics = true;
			}
			case COIN -> {
				this.lifetime = 25 + this.random.nextInt(10);
				this.quadSize = 0.12F;
				this.gravity = 0.5F;
				this.hasPhysics = true;
			}
			case BOLT -> {
				this.lifetime = 4 + this.random.nextInt(3);
				this.quadSize = 0.3F + this.random.nextFloat() * 0.15F;
				this.roll = this.random.nextFloat() * Mth.TWO_PI;
				spin = 0.0F;
			}
			case BUBBLE -> {
				this.lifetime = 16 + this.random.nextInt(10);
				this.quadSize = 0.08F + this.random.nextFloat() * 0.06F;
				this.gravity = -0.12F;
				this.friction = 0.9F;
			}
		}
		this.spin = spin;
		this.baseSize = this.quadSize;
	}

	@Override
	public void tick() {
		super.tick();
		this.oRoll = this.roll;
		this.roll += this.spin;
		float t = (float)this.age / this.lifetime;
		switch (this.kind) {
			case SPARK, ORB, BUBBLE -> this.quadSize = this.baseSize * (1.0F - t);
			case SLASH, BOLT -> this.alpha = 1.0F - t * t;
			case RING -> {
				this.quadSize = this.baseSize + t * 2.6F;
				this.alpha = 1.0F - t;
			}
			case RUNE -> this.alpha = t < 0.2F ? t * 5.0F : 1.0F - Math.max(0.0F, (t - 0.6F) / 0.4F);
			case SMOKE -> {
				this.quadSize = this.baseSize * (1.0F + t);
				this.alpha = 0.8F * (1.0F - t);
			}
			case PETAL, FEATHER -> {
				if (!this.onGround) {
					this.xd += Math.sin(this.age * 0.25F + this.phase) * 0.004;
					this.zd += Math.cos(this.age * 0.2F + this.phase) * 0.004;
				}
				this.alpha = 1.0F - Math.max(0.0F, (t - 0.7F) / 0.3F);
			}
			case SHARD, COIN -> this.alpha = 1.0F - Math.max(0.0F, (t - 0.7F) / 0.3F);
		}
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	@Override
	protected int getLightCoords(final float a) {
		return switch (this.kind) {
			case SMOKE, PETAL, FEATHER, SHARD, COIN -> super.getLightCoords(a);
			default -> 0xF000F0;
		};
	}

	public static class Provider implements ParticleProvider<ColorParticleOption> {
		private final SpriteSet sprites;
		private final Fx.Kind kind;

		public Provider(final SpriteSet sprites, final Fx.Kind kind) {
			this.sprites = sprites;
			this.kind = kind;
		}

		@Override
		public @Nullable Particle createParticle(
			final ColorParticleOption options, final ClientLevel level, final double x, final double y, final double z, final double xAux, final double yAux,
			final double zAux, final RandomSource random
		) {
			SkillParticle particle = new SkillParticle(level, x, y, z, xAux, yAux, zAux, this.sprites, this.kind);
			particle.setColor(options.getRed(), options.getGreen(), options.getBlue());
			return particle;
		}
	}
}
