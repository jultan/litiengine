package de.gurkenlabs.litiengine.input;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.gurkenlabs.litiengine.IGameLoop;
import de.gurkenlabs.litiengine.IUpdateable;
import de.gurkenlabs.litiengine.entities.IMovableEntity;
import de.gurkenlabs.litiengine.physics.Force;
import de.gurkenlabs.litiengine.physics.IPhysicsEngine;
import de.gurkenlabs.util.geom.GeometricUtilities;

public class EntityMovementController implements IUpdateable, IEntityMovementController {
  private final List<Force> activeForces;
  private final IMovableEntity movableEntity;
  private final IPhysicsEngine engine;

  public EntityMovementController(final IGameLoop gameLoop, IPhysicsEngine engine, final IMovableEntity movableEntity) {
    this.activeForces = new CopyOnWriteArrayList<>();
    this.movableEntity = movableEntity;
    this.engine = engine;
    gameLoop.registerForUpdate(this);
  }

  @Override
  public IMovableEntity getControlledEntity() {
    return this.movableEntity;
  }

  @Override
  public void apply(Force force) {
    if (!this.activeForces.contains(force)) {
      this.activeForces.add(force);
    }
  }

  @Override
  public void update(final IGameLoop gameLoop) {
    this.handleForces(gameLoop);
  }
  
  @Override
  public List<Force> getActiceForces() {
    return this.activeForces;
  }

  protected IPhysicsEngine getPhysicsEngine() {
    return this.engine;
  }

  private void handleForces(IGameLoop gameLoop) {
    // clean up forces
    this.activeForces.forEach(x -> {
      if (x.hasEnded()) {
        this.activeForces.remove(x);
      }
    });

    // apply all forces
    // TODO: calculate the diff of all forces cobined and only move the entity once
    for (final Force force : this.activeForces) {
      if (force.cancelOnReached() && force.hasReached(this.getControlledEntity())) {
        force.end();
        continue;
      }

      final double angle = GeometricUtilities.calcRotationAngleInDegrees(new Point2D.Double(this.getControlledEntity().getCollisionBox().getCenterX(), this.getControlledEntity().getCollisionBox().getCenterY()), force.getLocation());
      
      final boolean success = this.getPhysicsEngine().move(this.getControlledEntity(), angle, (gameLoop.getDeltaTime() / 1000.0F) * force.getStrength());
      if (force.cancelOnCollision() && !success) {
        force.end();
      }
    }
  }
}
