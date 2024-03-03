/*
Copyright (C) 2016 Bengt Martensson.

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

import java.io.IOException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;

/**
 * This interface contains the common parts between capturers and receivers.
 */
public interface IIrReader extends IHarcHardware {

    public static final int DEFAULT_BEGIN_TIMEOUT = 10000;
    public static final int DEFAULT_CAPTURE_MAXSIZE = 500;
    public static final int DEFAULT_ENDING_TIMEOUT = 300;

    public void setBeginTimeout(int integer) throws IOException, HarcHardwareException;

    public void setCaptureMaxSize(int integer) throws IOException, HarcHardwareException;

    public void setEndingTimeout(int integer) throws IOException, HarcHardwareException;
}
