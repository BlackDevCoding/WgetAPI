package xyz.blackdev.wgetapi.winget;

import de.craftsblock.craftsnet.api.transformers.Transformable;
import de.craftsblock.craftsnet.api.transformers.builtin.IntTransformer;

/**
 * @author Philipp Maywald
 */
public class WingetLimitSanitizer implements Transformable<Integer, Integer> {

    @Override
    public Integer transform(Integer parameter) {
        return Math.clamp(parameter, 1, 5000);
    }

    @Override
    public Class<? extends Transformable<Integer, ?>> getParent() {
        return IntTransformer.class;
    }

}
