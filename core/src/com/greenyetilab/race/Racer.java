package com.greenyetilab.race;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Disposable;

/**
 * A racer
 */
public class Racer implements GameObject, Collidable, Disposable {
    private final GameWorld mGameWorld;
    private final Vehicle mVehicle;
    private final VehicleRenderer mVehicleRenderer;
    private final HealthComponent mHealthComponent = new HealthComponent();
    private final GroundCollisionHandlerComponent mGroundCollisionHandlerComponent;
    private Pilot mPilot;
    private int mLapCount = 0;
    private final LapPosition mLapPosition = new LapPosition();
    private boolean mFinished = false;
    private int mScore;

    public Racer(GameWorld gameWorld, Vehicle vehicle) {
        mGameWorld = gameWorld;
        mHealthComponent.setInitialHealth(Constants.PLAYER_HEALTH);

        mVehicle = vehicle;
        mVehicle.setUserData(this);
        mVehicle.setCollisionInfo(CollisionCategories.RACER,
                CollisionCategories.WALL
                | CollisionCategories.RACER | CollisionCategories.RACER_BULLET
                | CollisionCategories.AI_VEHICLE | CollisionCategories.FLAT_AI_VEHICLE
                | CollisionCategories.GIFT);

        mVehicleRenderer = new VehicleRenderer(mVehicle, mHealthComponent);
        mGroundCollisionHandlerComponent = new GroundCollisionHandlerComponent(mVehicle, mHealthComponent);
    }

    public void setPilot(Pilot pilot) {
        mPilot = pilot;
    }

    public Vehicle getVehicle() {
        return mVehicle;
    }

    public int getLapCount() {
        return mLapCount;
    }

    public float getLapDistance() {
        return mLapPosition.getLapDistance();
    }

    public boolean isFinished() {
        return mFinished;
    }

    public void adjustScore(int delta, float x, float y) {
        mScore += Constants.SCORE_GIFT_PICK;
        mGameWorld.showScoreIndicator(Constants.SCORE_GIFT_PICK, x, y);
    }

    @Override
    public void beginContact(Contact contact, Fixture otherFixture) {
        Object other = otherFixture.getBody().getUserData();
        if (other instanceof BonusSpot) {
            BonusSpot spot = (BonusSpot)other;
            spot.pickBonus();
        }
    }

    @Override
    public void endContact(Contact contact, Fixture otherFixture) {
    }

    @Override
    public void preSolve(Contact contact, Fixture otherFixture, Manifold oldManifold) {
    }

    @Override
    public void postSolve(Contact contact, Fixture otherFixture, ContactImpulse impulse) {
    }

    @Override
    public void dispose() {
        mVehicle.dispose();
    }

    @Override
    public boolean act(float delta) {
        if (!mFinished) {
            updatePosition();
        }
        boolean keep = mVehicle.act(delta);
        if (keep) {
            if (mFinished) {
                mVehicle.setAccelerating(false);
            } else {
                keep = mPilot.act(delta);
            }
        }
        if (keep) {
            keep = mGroundCollisionHandlerComponent.act(delta);
        }
        if (keep) {
            keep = mHealthComponent.act(delta);
        }
        if (!keep) {
            dispose();
        }
        return keep;
    }

    private void updatePosition() {
        int oldSectionId = mLapPosition.getSectionId();
        MapInfo mapInfo = mGameWorld.getMapInfo();
        final float PFU = 1 / Constants.UNIT_FOR_PIXEL;
        mLapPosition.copy(mapInfo.getLapPositionTable().get((int)(PFU * mVehicle.getX()), (int)(PFU * mVehicle.getY())));
        if (mLapPosition.getSectionId() == 0 && oldSectionId > 1) {
            ++mLapCount;
            if (mLapCount > mapInfo.getTotalLapCount()) {
                --mLapCount;
                mFinished = true;
            }
        } else if (mLapPosition.getSectionId() > 1 && oldSectionId == 0) {
            --mLapCount;
        }
    }

    @Override
    public void draw(Batch batch, int zIndex) {
        mVehicleRenderer.draw(batch, zIndex);
    }

    @Override
    public float getX() {
        return mVehicle.getX();
    }

    @Override
    public float getY() {
        return mVehicle.getY();
    }

    @Override
    public HealthComponent getHealthComponent() {
        return mHealthComponent;
    }

    public int getScore() {
        return mScore;
    }
}
