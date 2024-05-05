package com.CS4303.group3;

import com.CS4303.group3.Game.*;

import dev.dominion.ecs.api.*;

public class GameSchedule {
    @FunctionalInterface
    public interface DrawWrapper {
        void call(DrawOperation op);
    }

    @FunctionalInterface
    public interface DrawSystem {
        void run(DrawWrapper draw);
    }

    private Game _game;

    Scheduler _setup;
    Scheduler _update;
    Scheduler _draw;

    Scheduler _keydown;
    Scheduler _keyup;

    public GameSchedule(Game game) {
        this._game = game;
        var dom = game.dom;

        _setup = dom.createScheduler();
        _update = dom.createScheduler();
        _draw = dom.createScheduler();
        _keydown = dom.createScheduler();
        _keyup = dom.createScheduler();
    }

    public float dt() {
        return Math.min((float)this._update.deltaTime(), 0.1f);
    }

    public GameSchedule setup(Runnable system) {
        this._setup.schedule(system);
        return this;
    }

    public GameSchedule update(Runnable system) {
        this._update.schedule(system);
        return this;
    }

    /**
     * Add a system to draw with a certain priority. Systems with higher priority will be executed later, hence being drawn over other objects.
     * @param priority
     * @param system
     * @return
     */
    public GameSchedule draw(long priority, DrawSystem system) {
        this._draw.schedule(() -> {
            system.run(op -> {
                this._game.drawQueue.add(new PriorityDrawOperation<>(op, priority));
            });
        });
        return this;
    }

    public GameSchedule draw(DrawSystem system) {
        return this.draw(0, system);
    }

    public GameSchedule keyDown(Runnable system) {
        this._keydown.schedule(system);
        return this;
    }

    public GameSchedule keyUp(Runnable system) {
        this._keyup.schedule(system);
        return this;
    }

}
