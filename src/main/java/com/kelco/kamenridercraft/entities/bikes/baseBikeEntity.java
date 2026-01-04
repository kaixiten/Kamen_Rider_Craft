package com.kelco.kamenridercraft.entities.bikes;

import javax.annotation.Nullable;

import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.util.GeckoLibUtil;

import com.kelco.kamenridercraft.sounds.ModSounds;

public class baseBikeEntity extends Mob implements GeoEntity {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public String NAME = "skullboilder";
	public String NAME_MODEL = "hardboilder";
	public String NAME_ANIMATIONS = "hardboilder";
	public float MAX_SPEED = 0.01f;
	public static final int MAX_CLIMB_HEIGHT = 2;

	private int engineSoundTimer = 0;
	private float currentPitch = 1.0f;
	private boolean wasMoving = false;
	private static final SoundEvent ENGINE_SOUND = ModSounds.EXCITE2.get();

	public RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.model.idle");
	public RawAnimation DRIVE = RawAnimation.begin().thenLoop("animation.model.walk");
	public RawAnimation DRIVE_BACKWARDS = RawAnimation.begin().thenLoop("animation.model.walk_backwards");

	public Item VEHICLE_DROP = Items.AIR;

	public baseBikeEntity(EntityType<? extends Mob> entityType, Level level, Item drop) {
		super(entityType, level);
		this.setPersistenceRequired();
		this.VEHICLE_DROP = drop;
		this.xxa = 0.0F;
		this.zza = 0.0F;
	}

	public static AttributeSupplier.Builder setAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.3F)
				.add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.ATTACK_DAMAGE, 2.0D)
				.add(Attributes.FOLLOW_RANGE, 32.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
	}

	@Override
	protected AABB makeBoundingBox() {
		Vec3 pos = this.position();
		float halfWidth = 0.2f;
		float halfLength = 1.0f;
		float height = 1.2f;

		float yaw = this.getYRot() * Mth.DEG_TO_RAD;
		double cos = Math.abs(Mth.cos(yaw));
		double sin = Math.abs(Mth.sin(yaw));

		double expandedX = halfWidth * cos + halfLength * sin;
		double expandedZ = halfWidth * sin + halfLength * cos;

		return new AABB(
				pos.x - expandedX, pos.y, pos.z - expandedZ,
				pos.x + expandedX, pos.y + height, pos.z + expandedZ
		);
	}

	private void playEngineSound() {
		if (!this.level().isClientSide()) return;

		LivingEntity passenger = this.getControllingPassenger();
		float speed = (float) this.getDeltaMovement().horizontalDistance();
		float passengerInput = passenger != null ? Math.abs(passenger.zza) : 0f;

		boolean hasPassenger = passenger != null;
		boolean isMoving = speed > 0.01f || passengerInput > 0.1f;

		if (hasPassenger && !isMoving) {
			if (this.engineSoundTimer % 8 == 0) {
				this.level().playLocalSound(
						this.getX(), this.getY(), this.getZ(),
						ENGINE_SOUND,
						SoundSource.NEUTRAL,
						0.35f, 0.65f, false
				);
			}
			this.engineSoundTimer++;
			wasMoving = false;
			return;
		}

		if (hasPassenger || speed > 0.01f) {
			float targetPitch = calculateEnginePitch(speed, passengerInput);
			this.currentPitch = Mth.lerp(0.1f, this.currentPitch, targetPitch);

			if (this.engineSoundTimer % 5 == 0) {
				this.level().playLocalSound(
						this.getX(), this.getY(), this.getZ(),
						ENGINE_SOUND,
						SoundSource.NEUTRAL,
						0.5f,
						this.currentPitch,
						false
				);
			}
			this.engineSoundTimer++;
			wasMoving = true;
			return;
		}

		wasMoving = false;
	}

	private float calculateEnginePitch(float speed, float passengerSpeed) {
		float targetPitch;
		if (passengerSpeed > 0) {
			targetPitch = 0.8f + speed * 3.0f;
		} else if (speed > 0.01f) {
			targetPitch = 0.8f + speed * 2.0f;
		} else {
			targetPitch = 0.7f;
		}
		return Mth.clamp(targetPitch, 0.7f, 2.0f);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!this.isVehicle() && !player.isShiftKeyDown()) {
			player.startRiding(this);
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return canVehicleCollide(this, entity);
	}

	public static boolean canVehicleCollide(Entity vehicle, Entity entity) {
		return (entity.canBeCollidedWith() || entity.isPushable()) && !vehicle.isPassengerOfSameVehicle(entity);
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portal) {
		return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, portal));
	}

	@Override
	public void push(Entity entity) {
		if (entity instanceof Boat) {
			if (entity.getBoundingBox().minY < this.getBoundingBox().maxY) {
				super.push(entity);
			}
		} else if (entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
			super.push(entity);
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.is(DamageTypes.PLAYER_ATTACK) && !this.hasControllingPassenger()) {
			return super.hurt(source, this.getMaxHealth());
		}
		return super.hurt(source, amount);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
	}
	
	private float getTurnAngleDegrees() {
		Vec3 motion = this.getDeltaMovement();
		double speed = motion.horizontalDistance();

		if (speed < 0.01) {
			return 0.0f;
		}

		Vec3 motionDir = motion.multiply(1.0 / speed, 0.0, 1.0 / speed);

		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		Vec3 headDir = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));

		double dot = motionDir.dot(headDir);
		dot = Mth.clamp(dot, -1.0, 1.0);

		double angleRad = Math.acos(dot);
		return (float) Math.toDegrees(angleRad);
	}

	@Override
	public void travel(Vec3 pos) {
		if (this.isAlive()) {
			float zInput = 0;
			this.fallDistance = 0;

			if (this.isVehicle()) {
				LivingEntity passenger = getControllingPassenger();

				if (passenger != null) {
					this.yRotO = getYRot();
					this.xRotO = getXRot();

					zInput = passenger.zza;

					if (zInput <= 0) zInput *= 0.25f;

					if (zInput > 0) {
						if (this.getSpeed() < 0.8) this.setSpeed(this.getSpeed() + MAX_SPEED);
						setYRot(yRotO - passenger.xxa * 10F);
						setXRot(passenger.getXRot() * 3f);

						setRot(getYRot(), getXRot());
						this.yBodyRot = this.getYRot();
						this.yHeadRot = this.yBodyRot;

					} else if (zInput < 0) {
						if (this.getSpeed() < 1) this.setSpeed(this.getSpeed() + MAX_SPEED);
						setYRot(yRotO + passenger.xxa * 10F);
						setXRot(-passenger.getXRot() * 3f);

						setRot(getYRot(), getXRot());
						this.yBodyRot = this.getYRot();
						this.yHeadRot = this.yBodyRot;
					} else {
						if (this.getSpeed() != 0) {
							this.setSpeed(0f);
						}
					}
				}
			}

			Vec3 currentMovement = this.getDeltaMovement();

			super.travel(new Vec3(0, pos.y, zInput));

			if (this.isVehicle()) {
				LivingEntity passenger = this.getControllingPassenger();
				if (passenger != null) {
					Vec3 motion = this.getDeltaMovement();
					double currentSpeed = motion.horizontalDistance();

					if (currentSpeed > 0.01 && Math.abs(passenger.xxa) > 0.05f) {
						float turnAngle = this.getTurnAngleDegrees();

						double decelFactor = 0.0;

						if (turnAngle >= 45.0f) {
							decelFactor = 0.15;
						} else if (turnAngle >= 30.0f) {
							decelFactor = 0.07;
						} else if (turnAngle >= 20.0f) {
							decelFactor = 0.02;
						}

						if (decelFactor > 0) {
							double newSpeed = currentSpeed * (1.0 - decelFactor);
							if (newSpeed >= 0) {
								Vec3 newMotion = motion.multiply(newSpeed / currentSpeed, 1.0, newSpeed / currentSpeed);
								this.setDeltaMovement(newMotion);
							}
						}
					}
				}
			}

			if (this.isVehicle() && this.getControllingPassenger() != null) {
				AABB collisionBox = this.getBoundingBox();
				AABB detectionBox = collisionBox.inflate(
						collisionBox.getXsize() * 0.1,
						0.0,
						collisionBox.getZsize() * 0.1
				);

				int highestGroundY = Integer.MIN_VALUE;
				double[] xCoords = {
						detectionBox.minX, detectionBox.minX,
						detectionBox.maxX, detectionBox.maxX,
						detectionBox.minX + detectionBox.getXsize() / 2,
						detectionBox.maxX - detectionBox.getXsize() / 2
				};
				double[] zCoords = {
						detectionBox.minZ, detectionBox.maxZ,
						detectionBox.minZ, detectionBox.maxZ,
						detectionBox.maxZ,
						detectionBox.minZ
				};

				double bottomY = detectionBox.minY + 0.2;

				for (int i = 0; i < xCoords.length; i++) {
					int groundY = getGroundHeightAt(xCoords[i], zCoords[i], bottomY);
					if (groundY > highestGroundY) {
						highestGroundY = groundY;
					}
				}

				LivingEntity passenger = this.getControllingPassenger();
				if (passenger != null && Math.abs(passenger.zza) > 0.1f) {
					float yaw = this.getYRot() * Mth.DEG_TO_RAD;
					double forwardX = -Math.sin(yaw);
					double forwardZ = Math.cos(yaw);
					double frontOffset = passenger.zza > 0 ? 1.5 : -1.5;
					double frontX = this.getX() + forwardX * frontOffset;
					double frontZ = this.getZ() + forwardZ * frontOffset;

					int frontGroundY = getGroundHeightAt(frontX, frontZ, bottomY);
					if (frontGroundY > highestGroundY) {
						highestGroundY = frontGroundY;
					}
				}

				if (highestGroundY != Integer.MIN_VALUE) {
					double currentY = this.getY();
					double targetY = highestGroundY + 0.1;
					double deltaY = targetY - currentY;

					if (deltaY > 0.01 && deltaY <= MAX_CLIMB_HEIGHT) {
						this.setPos(this.getX(), targetY, this.getZ());

						Vec3 horizontalVel = new Vec3(currentMovement.x, 0, currentMovement.z);
						if (horizontalVel.length() > 0) {
							this.setDeltaMovement(horizontalVel.normalize().scale(currentMovement.horizontalDistance()));
						}

						if (passenger != null && passenger.zza > 0.1f) {
							if (this.level() instanceof ServerLevel serverLevel) {
								for (int i = 0; i < 3; i++) {
									double particleX = this.getX() + (this.random.nextDouble() - 0.5) * 0.6;
									double particleZ = this.getZ() + (this.random.nextDouble() - 0.5) * 0.6;
									serverLevel.sendParticles(
											ParticleTypes.CAMPFIRE_COSY_SMOKE,
											particleX, this.getY(), particleZ,
											1, 0, 0.02, 0, 0.005
									);
								}
							}
						}
					}
				}
			}
		}

		playEngineSound();

		LivingEntity rider = this.getControllingPassenger();
		if (rider != null && rider.zza > 0.1f) {
			if (this.tickCount % 3 == 0) {
				spawnExhaustParticles();
			}
		}
	}

	private int getGroundHeightAt(double worldX, double worldZ, double fromY) {
		int currentY = Mth.floor(fromY);
		int highestGroundY = Integer.MIN_VALUE;

		for (int yCheck = currentY; yCheck <= currentY + MAX_CLIMB_HEIGHT; yCheck++) {
			BlockPos checkPos = new BlockPos(Mth.floor(worldX), yCheck, Mth.floor(worldZ));
			BlockState state = this.level().getBlockState(checkPos);

			if (!state.isAir() && state.isSolid()) {
				VoxelShape shape = state.getCollisionShape(this.level(), checkPos);
				if (!shape.isEmpty()) {
					double topY = checkPos.getY() + shape.max(Direction.Axis.Y);
					if (topY > highestGroundY && topY <= fromY + MAX_CLIMB_HEIGHT) {
						highestGroundY = (int) Math.ceil(topY);
					}
				}
			}
		}

		if (highestGroundY == Integer.MIN_VALUE) {
			BlockPos surfacePos = new BlockPos(Mth.floor(worldX), 0, Mth.floor(worldZ));
			highestGroundY = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, surfacePos).getY();
		}

		return highestGroundY;
	}

	private void spawnExhaustParticles() {
		LivingEntity rider = this.getControllingPassenger();
		if (rider == null || rider.zza <= 0.1f) return;

		if (this.level() instanceof ServerLevel serverLevel && this.onGround()) {
			float yaw = this.getYRot() * Mth.DEG_TO_RAD;
			double offsetX = -0.8 * Math.sin(yaw);
			double offsetZ = 0.8 * Math.cos(yaw);

			double particleX = this.getX() + offsetX;
			double particleY = this.getY() + 0.3;
			double particleZ = this.getZ() + offsetZ;

			float speed = (float) this.getDeltaMovement().horizontalDistance();

			if (speed > 0.01f) {
				int particleCount = Math.max(1, (int) (speed * 15));
				serverLevel.sendParticles(
						ParticleTypes.CAMPFIRE_COSY_SMOKE,
						particleX, particleY, particleZ,
						particleCount,
						0.1, 0.1, 0.1,
						0.02
				);
			}
		}
	}

	@Nullable
	@Override
	public LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof LivingEntity entity ? entity : null;
	}

	@Override
	protected Entity.MovementEmission getMovementEmission() {
		return MovementEmission.EVENTS;
	}

	@Override
	public boolean isControlledByLocalInstance() {
		return true;
	}

	@Override
	public void positionRider(Entity entity, MoveFunction moveFunction) {
		if (entity instanceof LivingEntity passenger) {
			moveFunction.accept(entity, getX(), getY() + 0.3f, getZ());
			this.xRotO = passenger.xRotO;
		}
	}

	@Override
	public void die(DamageSource damageSource) {
		super.die(damageSource);
		if (!this.level().isClientSide) {
			this.spawnAtLocation(this.VEHICLE_DROP);
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource) {
		return SoundEvents.METAL_BREAK;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.METAL_BREAK;
	}

	protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
		return 0.5F;
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "controller", 2, state -> {
			EntityModelData entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
			float front_fork = 0;
			float wheel = 0;

			if (this.getControllingPassenger() != null) {
				if (this.getControllingPassenger().xxa < 0) front_fork = -0.25f;
				if (this.getControllingPassenger().xxa > 0) front_fork = 0.25f;
				if (this.getControllingPassenger().zza > 0) wheel = -0.1f;
				if (this.getControllingPassenger().zza < 0) wheel = 0.05f;
			}

			EntityModelData newEntityData = new EntityModelData(false, false, entityData.netHeadYaw() + wheel, front_fork);
			state.setData(DataTickets.ENTITY_MODEL_DATA, newEntityData);

			if (getControllingPassenger() != null) {
				if (getControllingPassenger().zza != 0) {
					return getControllingPassenger().zza > 0 ? state.setAndContinue(DRIVE) : state.setAndContinue(DRIVE_BACKWARDS);
				}
			}
			return state.setAndContinue(IDLE);
		}).setSoundKeyframeHandler(event -> {}));
	}
}
