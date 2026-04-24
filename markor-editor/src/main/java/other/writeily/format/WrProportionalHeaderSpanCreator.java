/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2025 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
 ###########################################################*/
package other.writeily.format;

import android.text.style.RelativeSizeSpan;

public class WrProportionalHeaderSpanCreator {
    private final int _color;

    public WrProportionalHeaderSpanCreator(int color) {
        _color = color;
    }

    public Object createHeaderSpan(float proportion) {
        return new RelativeSizeSpan(proportion);
    }
}
