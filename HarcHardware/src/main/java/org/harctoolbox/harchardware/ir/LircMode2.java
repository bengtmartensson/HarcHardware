/*
Copyright (C) 2013, 2014, 2017 Bengt Martensson.

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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.ModulatedIrSequence;

/**
 * This class runs an external program, for example mode2 of LIRC, in a separate process,
 * and evaluates its output, which is assumed to be in the LIRC mode2 format.
 */
public final class LircMode2 implements IHarcHardware, ICapture, IReceive  {
    private final Mode2Parser parser;

    public LircMode2(Reader reader, boolean verbose, int endingTimeout) {
        parser = new Mode2Parser(reader, verbose, (int) IrCoreUtils.milliseconds2microseconds(endingTimeout));
    }

    public LircMode2(InputStream inputStream, boolean verbose, int endingTimeout) {
        this(new InputStreamReader(inputStream, Charset.forName("US-ASCII")), verbose, endingTimeout);
    }

    public LircMode2(boolean verbose, int endingTimeout) {
        this(System.in, verbose, endingTimeout);
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public void setVerbose(boolean verbose) {
        parser.setVerbose(verbose);
    }

    @Override
    public void setDebug(int debug) {
    }

    @Override
    public boolean isValid() {
        return parser.isValid();
    }

    @Override
    public void close() throws IOException {
        parser.close();
    }


    @Override
    public void open() throws IOException {
    }

    @Override
    public ModulatedIrSequence capture() throws HarcHardwareException, IOException {
        IrSequence irSequence = receive();
        return irSequence != null
                ? new ModulatedIrSequence(irSequence, ModulatedIrSequence.DEFAULT_FREQUENCY, null)
                : null;
    }

    @Override
    public boolean stopCapture() {
        try {
            close();
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

    @Override
    public void setEndingTimeout(int timeout) {
        parser.setThreshold((int)IrCoreUtils.milliseconds2microseconds(timeout));
    }

    @Override
    public IrSequence receive() throws IOException, HarcHardwareException {
        try {
            return parser.readIrSequence();
        } catch (ParseException ex) {
            throw new HarcHardwareException(ex);
        }
    }

    @Override
    public boolean stopReceive() {
        try {
            close();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(LircMode2.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    @Override
    public void setTimeout(int timeout) throws IOException {
        setEndingTimeout(timeout);
    }

    @Override
    public void setBeginTimeout(int integer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setCaptureMaxSize(int integer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
