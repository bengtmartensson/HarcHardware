/*
Copyright (C) 2013 Bengt Martensson.

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
import javax.sound.sampled.LineUnavailableException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.IHarcHardware;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;

/**
 * This class implements IR sending using the Java audio device.
 * It is essentially a wrapper around the {@link Wave} class.
 */
public class IrAudioDevice implements IHarcHardware, IRawIrSender {

    public static final int DEFAULT_SAMPLE_FREQUENCY = 48000;
    public static final int DEFAULT_SAMPLE_SIZE = 8;
    public static final int DEFAULT_CHANNELS = 1;

    private int sampleFrequency;
    private int sampleSize;
    private int channels;
    private boolean bigEndian;
    private boolean omitTail;
    private boolean square;
    private boolean divide;

    private boolean verbose;

    /**
     * Generic constructor.
     *
     * @param sampleFrequency Frequency for the audio device, do not confuse
     * with the IR modulation frequency.
     * @param sampleSize Size in bits of generated samples. Normally 8.
     * @param channels 1 for mono (normal case), 2 for "stereo" (left and right
     * in anti-phase).
     * @param bigEndian For sampleSize &gt; 8, generate samples in big endian
     * format.
     * @param omitTail Omit the final silence.
     * @param square Generate square vave on output, otherwise sine.
     * @param divide Normally true, assuming the frequency will be effectively
     * doubled by the use of anti-parallel LEDs on the output.
     * @param verbose Verbose operation.
     */
    public IrAudioDevice(int sampleFrequency, int sampleSize, int channels, boolean bigEndian,
            boolean omitTail, boolean square, boolean divide, boolean verbose) {
        this.sampleFrequency = sampleFrequency;
        this.sampleSize = sampleSize;
        this.channels = channels;
        this.bigEndian = bigEndian;
        this.omitTail = omitTail;
        this.square = square;
        this.divide = divide;
        this.verbose = verbose;
    }

    /**
     * Convenience constructor, defaulting some parameters.
     *
     * @param sampleFrequency Frequency for the audio device, do not confuse
     * with the IR modulation frequency.
     * @param sampleSize Size in bits of generated samples. Normally 8.
     * @param channels 1 for mono (normal case), 2 for "stereo" (left and right
     * in anti-phase).
     * @param omitTail Omit the final silence.
     * @param verbose Verbose operation.
     */
    public IrAudioDevice(int sampleFrequency, int sampleSize, int channels, boolean omitTail, boolean verbose) {
        this(sampleFrequency, sampleSize, channels, false, omitTail, true, true, verbose);
    }

    /**
     * Constructor for usage in HarcHardware.Main.
     *
     * @param timeout ignored
     * @param verbose Verbose operation.
     */
    public IrAudioDevice(boolean verbose, Integer timeout) {
        this(DEFAULT_SAMPLE_FREQUENCY, DEFAULT_SAMPLE_SIZE, DEFAULT_CHANNELS, false, false, true, true, verbose);
    }

    /**
     * Convenience constructor, defaulting all parameters.
     */
    public IrAudioDevice() {
        this(DEFAULT_SAMPLE_FREQUENCY, DEFAULT_SAMPLE_SIZE, DEFAULT_CHANNELS, false, false, true, true, false);
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count, Transmitter transmitter) throws NoSuchTransmitterException, IOException, HarcHardwareException, InvalidArgumentException {
        ModulatedIrSequence seq = irSignal.toModulatedIrSequence(count);
        Wave wave = new Wave(seq, sampleFrequency, sampleSize, channels, bigEndian, omitTail, square, divide);
        try {
            wave.play();
            if (verbose)
                System.err.println("Sent IrSignal @ " + sampleFrequency + "Hz, " + sampleSize + "bits, "
                        + (channels == 2 ? "stereo" : "mono") + ", to audio device");
            return true;
        } catch (LineUnavailableException ex) {
            throw new HarcHardwareException(ex);
        }
    }

    @Override
    public String getVersion() throws IOException {
        return null;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void setTimeout(int timeout) {
        // there is no timeout to be set.
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }

    @Override
    public void open() throws IOException {
        // nothing to do
    }

    @Override
    public Transmitter getTransmitter() {
        return null;
    }

    @Override
    public void setDebug(int debug) {
    }

    /**
     * @return the sampleFrequency
     */
    public int getSampleFrequency() {
        return sampleFrequency;
    }

    /**
     * @param sampleFrequency the sampleFrequency to set
     */
    public void setSampleFrequency(int sampleFrequency) {
        this.sampleFrequency = sampleFrequency;
    }

    /**
     * @param sampleSize the sampleSize to set
     */
    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    /**
     * @param channels the channels to set
     */
    public void setChannels(int channels) {
        this.channels = channels;
    }

    /**
     * @param omitTail the omitTail to set
     */
    public void setOmitTail(boolean omitTail) {
        this.omitTail = omitTail;
    }
}
