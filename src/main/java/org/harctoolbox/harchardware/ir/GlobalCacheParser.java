/*
Copyright (C) 2018 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.harchardware.ir;

import org.harctoolbox.ircore.AbstractIrParser;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.IrSignalParser;
import org.harctoolbox.ircore.MultiParser;

public class GlobalCacheParser extends AbstractIrParser implements IrSignalParser {

    public static MultiParser newParser(String source) {
        MultiParser parser = MultiParser.newIrCoreParser(source);
        parser.addParser(new GlobalCacheParser(source));
        return parser;
    }

    public GlobalCacheParser(String source) {
        super(source);
    }

    public GlobalCacheParser(Iterable<? extends CharSequence> args) {
        super(args);
    }

    @Override
    public IrSignal toIrSignal(Double fallbackFrequency, Double dummyGap) throws InvalidArgumentException {
        return GlobalCache.parse(getSource());
    }

    @Override
    public String getName() {
        return "GlobalCache";
    }
}
