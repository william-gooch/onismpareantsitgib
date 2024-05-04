package com.CS4303.group3.plugin;

import com.CS4303.group3.Game;
import com.CS4303.group3.Resource;
import com.CS4303.group3.plugin.Button_Plugin.Button;
import com.CS4303.group3.plugin.Force_Plugin.Gravity;
import com.CS4303.group3.plugin.Object_Plugin.Position;
import com.CS4303.group3.utils.Changeable.Changeable_Boolean;

import dev.dominion.ecs.api.Dominion;
import processing.core.PVector;

public class Door_Plugin implements Plugin_Interface {

    Dominion dom;
    

    @Override
    public void build(Game game) {
        dom = game.dom;

        // TO DO: discuss how the dimension flipping works because the shrinking and
        // expanding of the door is dependent on the gravity and its happening in the
        // wrong direction right now

        // TO DO: need to deal with collider for the door

        // game.schedule.draw(draw -> {
        //     dom.findEntitiesWith(Position.class, Door.class)
        //             .stream().forEach(res -> {
        //                 var pos = res.comp1().position;
        //                 var height = res.comp2().height;
        //                 var width = res.comp2().width;
        //                 draw.call(drawing -> {

        //                     if (res.comp2().open) {
        //                         drawing.fill(40, 175, 176);
        //                     } else {
        //                         drawing.fill(151, 204, 4);
        //                     }

        //                     drawing.rect(pos.x, pos.y, width, height);

        //                 });
        //             });
        // });

    }

    static class Door {
        public int height, width, maxHeight, maxWidth;
        public float timeElapsed;
        public Changeable_Boolean open;
        public final float UPDATE_DELAY = 0.01f;
        public final float LOWERING_INCREMENT = 1f;

        public Door(int height, int width) {
            this.height = height;
            this.maxHeight = height;
            this.maxWidth = width;
            this.timeElapsed = 0;
            this.width = width;
            this.open = new Changeable_Boolean(false);
        }

        public boolean isOpen() {
            return open.get();
        }

        public void moveDoor(Game game, PVector pos) {

            timeElapsed += game.schedule.dt();

            if (timeElapsed >= UPDATE_DELAY) {
                if (isOpen()) {
                    lower(game, pos);
                } else {
                    raise(pos, game);
                }
                timeElapsed = 0;
            }

        }

        public void lower(Game game, PVector pos) {
            if (height > 0 && width > 0) {
                Gravity gravity = Resource.get(game, Gravity.class);

                if (gravity.gravity().y > 0) {

                    height -= LOWERING_INCREMENT;
                    pos.y += LOWERING_INCREMENT;

                } else if (gravity.gravity().y < 0) {

                    height -= LOWERING_INCREMENT;

                } else if (gravity.gravity().x > 0) {

                    width -= LOWERING_INCREMENT;

                    pos.x += LOWERING_INCREMENT;

                } else if (gravity.gravity().x < 0) {

                    width -= LOWERING_INCREMENT;

                }

                if (height < 0) {
                    height = 0;
                }

                if (width < 0) {
                    width = 0;
                }

            }
        }

        public void raise(PVector pos, Game game) {

            Gravity gravity = Resource.get(game, Gravity.class);
            if (height < maxHeight || width < maxWidth) {
                if (gravity.gravity().y > 0) {

                    height += LOWERING_INCREMENT;
                    pos.y -= LOWERING_INCREMENT;

                } else if (gravity.gravity().y < 0) {

                    height += LOWERING_INCREMENT;

                } else if (gravity.gravity().x > 0) {

                    width += LOWERING_INCREMENT;

                    pos.x -= LOWERING_INCREMENT;

                } else if (gravity.gravity().x < 0) {

                    width += LOWERING_INCREMENT;

                }
            }

            if (height > maxHeight) {
                height = maxHeight;
            }
            if (width > maxWidth) {
                width = maxWidth;
            }
        }

    }
}
