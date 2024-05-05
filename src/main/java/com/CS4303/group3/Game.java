package com.CS4303.group3;

import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;

import com.CS4303.group3.plugin.*;

import dev.dominion.ecs.api.Dominion;
import processing.core.*;
import processing.opengl.PGraphicsOpenGL;

public class Game extends PApplet {
    //Global Variables
    public Dominion dom;
    public GameSchedule schedule;
    public float scale;
    public boolean paused = false;
    public float playerWidth, playerHeight;

    PriorityBlockingQueue<PriorityDrawOperation<?>> drawQueue = new PriorityBlockingQueue<PriorityDrawOperation<?>>();

    public static String level_name = "./src/main/java/com/CS4303/group3/levels/default_level.json";

    static {
        System.setProperty("dominion.show-banner", "false");
    }

    //Main method
    public static void main(String[] args) {
        if(args.length > 0) level_name = "./src/main/java/com/CS4303/group3/levels/" + args[0] + ".json";
        main(Game.class.getName());
    }

    //Settings --Does not run this for some reason -- ask how william got this bit to run
    public void settings() {
//        fullScreen();
        // size((int)Math.floor(displayWidth * 0.5), (int)Math.floor(displayHeight * 0.5));

        if(displayHeight > displayWidth) {
            scale = displayWidth;
        } else scale = displayHeight * 0.5f;

        noSmooth();
//        size((int)scale, (int)scale, P2D);
        size((int)scale, (int)scale);
        playerWidth = scale/25;
        playerHeight = scale/16;
    }

    //Setup
    public void setup() {
        // size((int)Math.floor(displayWidth * 0.75), (int)Math.floor(displayHeight * 0.75));
//        ((PGraphicsOpenGL) getGraphics()).textureSampling(2);
//        frameRate(1);

        //start Dominion
        dom = Dominion.create();

        //start scheduler
        schedule = new GameSchedule(this);

        //
        addPlugin(new Map_Plugin());
        addPlugin(new Input_Plugin());
        addPlugin(new Assets_Plugin());
        addPlugin(new Object_Plugin());
        addPlugin(new Player_Plugin());
        addPlugin(new Force_Plugin());
        addPlugin(new Box_Plugin());
        addPlugin(new Button_Plugin());
        addPlugin(new Enemy_Plugin());
        addPlugin(new Door_Plugin());
        addPlugin(new Sprite_Plugin());
        addPlugin(new Game_Plugin());
        addPlugin(new Docking_Plugin());
        addPlugin(new Spike_Plugin());


        schedule._setup.tick();
    }

    private void addPlugin(Plugin_Interface plugin) {
        plugin.build(this);
    }

    //Game Loop
    public void draw() {
        background(0);

        if(!paused) schedule._update.tick();

        schedule._draw.tick();
        ArrayList<PriorityDrawOperation<?>> ops = new ArrayList<>(drawQueue.size());
        drawQueue.drainTo(ops);

        //rotate view with the gravity
        Force_Plugin.Gravity gravity_entity = Resource.get(this, Force_Plugin.Gravity.class);
        if(gravity_entity != null) {
            PVector gravity = gravity_entity.gravity();
            if (gravity.y < 0) {
                rotate(PI);
                translate(-scale, -scale);
            }
            if (gravity.x > 0) {
                rotate(HALF_PI);
                translate(0, -scale);
            }
            if (gravity.x < 0) {
                rotate(-HALF_PI);
                translate(-scale, 0);
            }
        }
        ops.stream()
            .forEach(op -> {
                op.operation.perform(this);
            });
    }

    //Renderings
    public void render() {
    }

    //Inputs
    public void keyPressed() {
        schedule._keydown.tick();
    }

    public void keyReleased() {
        schedule._keyup.tick();
    }

    //Queue stuff
    @FunctionalInterface
    public interface DrawOperation {
        void perform(Game game);
    }

    static class PriorityDrawOperation<T extends DrawOperation> implements Comparable<PriorityDrawOperation<?>> {
        T operation;
        long priority;

        public PriorityDrawOperation(T operation, long priority) {
            this.operation = operation;
            this.priority = priority;
        }

        @Override
        public int compareTo(PriorityDrawOperation<?> o) {
            return Long.valueOf(this.priority).compareTo(Long.valueOf(o.priority));
        }
    }
}
