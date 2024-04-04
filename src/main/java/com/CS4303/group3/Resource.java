package com.CS4303.group3;

/**
 * Utility functions that help with singleton entity types.
 */
public class Resource {
    public static <C> void add(Game game, Class<C> componentType) {
        try {
            var composition = game.dom.composition().of(componentType)
                .withValue(componentType.getConstructor().newInstance());
            game.schedule.setup(() -> {
                game.dom.createPreparedEntity(composition);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <C> C get(Game game, Class<C> componentType) {
        var iter = game.dom.findEntitiesWith(componentType).iterator();
        if(iter.hasNext())
            return iter.next().comp();
        else
            return null;
    }
}
