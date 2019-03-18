package com.lordsofmidnight.objects.powerUps;

import java.util.HashMap;
import java.util.UUID;

import com.lordsofmidnight.gamestate.points.PointMap;
import com.lordsofmidnight.objects.Entity;
import com.lordsofmidnight.objects.Pellet;
import java.util.concurrent.ConcurrentHashMap;

public class Invincible extends PowerUp {

  public Invincible() {
    super(200, "invincible");
    this.type = com.lordsofmidnight.utils.enums.PowerUp.INVINCIBLE;
  }

  @Override
  public void use(
      Entity user,
      ConcurrentHashMap<UUID, PowerUp> activePowerUps,
      PointMap<Pellet> pellets,
      Entity[] agents) {
    this.user = user;
    activePowerUps.put(id, this);
    this.effected = user;
    user.setInvincible(true);
    counter = 0;
  }

  @Override
  public boolean incrementTime() {
    super.incrementTime();
    if (counter == EFFECTTIME) {
      effected.setInvincible(false);
      return true;
    }
    return false;
  }
}
