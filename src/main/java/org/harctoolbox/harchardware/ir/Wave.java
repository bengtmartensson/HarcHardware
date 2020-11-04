/*
Copyright (C) 2009-2013 Bengt Martensson.

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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.DefaultConsole;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.harctoolbox.harchardware.Version;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.irp.IrpUtils;

/**
 * This class generates (or analyzes) a wave audio file that can be played
 * on standard audio equipment and fed to a pair of anti-parallel double IR sending diodes,
 * which can thus control IR equipment.
 *
 *
 * @see <a href="http://www.lirc.org/html/audio.html">http://www.lirc.org/html/audio.html</a>
 */

public class Wave {

    static final Logger logger = Logger.getLogger(Wave.class.getName());

    private static int epsilon8Bit = 2;
    private static int epsilon16Bit = 257;
    private static JCommander argumentParser;
    private static CommandLineArgs commandLineArgs = new CommandLineArgs();

    /**
     * Returns a line to the audio mixer on the local machine, suitable for sound with
     * the parameter values given. When not needed, the user should close the line with its close()-function.
     *
     * @param audioFormat
     * @return open audio line
     * @throws LineUnavailableException
     */
    public static SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine line = AudioSystem.getSourceDataLine(audioFormat);
        line.open(audioFormat);
        return line;
    }

    private static void usage(int exitcode) {
        PrintStream printStream = exitcode == IrpUtils.EXIT_SUCCESS ? System.out : System.err;
        argumentParser.setConsole(new DefaultConsole(printStream));
        argumentParser.usage();

        System.exit(exitcode);
    }

    /**
     * Provides a command line interface to the export/import functions.
     * @param args
     */
    public static void main(String[] args) {
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName("Wave");

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            usage(IrpUtils.EXIT_USAGE_ERROR);
        }

        if (commandLineArgs.helpRequensted)
            usage(IrpUtils.EXIT_SUCCESS);

        if (commandLineArgs.versionRequested) {
            System.out.println(Version.versionString);
            System.out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
            System.out.println();
            System.out.println(Version.licenseString);
            System.exit(IrpUtils.EXIT_SUCCESS);
        }

        try {
            if (commandLineArgs.parameters.size() == 1) {
                // Exactly one argument left -> input wave file
                String inputfile = commandLineArgs.parameters.get(0);
                Wave wave = new Wave(new File(inputfile));
                ModulatedIrSequence seq = wave.analyze(System.out, !commandLineArgs.dontDivide);
                System.out.println(seq.toString(true));
                wave.dump(new File(inputfile + ".tsv"));
                if (commandLineArgs.play)
                    wave.play();
            }
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex) {
            System.err.println(ex.getMessage());
            System.exit(IrpUtils.EXIT_FATAL_PROGRAM_FAILURE);
        }
    }

    private int noFrames = -1;
    private AudioFormat audioFormat;
    private byte[] buf;

    private Wave() {
    }

    /**
     * Reads a wave file into a Wave object.
     *
     * @param file Wave file as input.
     * @throws UnsupportedAudioFileException
     * @throws IOException
     */
    public Wave(File file) throws UnsupportedAudioFileException, IOException {
        AudioInputStream af = AudioSystem.getAudioInputStream(file);
        audioFormat = af.getFormat();
        noFrames = (int) af.getFrameLength();
        buf = new byte[noFrames*audioFormat.getFrameSize()];
        int n = af.read(buf, 0, buf.length);
        if (n != buf.length)
            logger.log(Level.SEVERE, "Too few bytes read: {0} < {1}", new Object[]{n, buf.length});
    }

    /**
     * Generates a wave audio file from its arguments.
     *
     * @param freq Carrier frequency in Hz.
     * @param data double array of durations in micro seconds.
     * @param sampleFrequency Sample frequency of the generated wave file.
     * @param sampleSize Sample size (8 or 16) in bits of the samples.
     * @param channels If == 2, generates two channels in perfect anti-phase.
     * @param bigEndian if true, use bigendian byte order for 16 bit samples.
     * @param omitTail If true, the last trailing gap will be omitted.
     * @param square if true, use a square wave for modulation, otherwise a sine.
     * @param divide If true, divides the carrier frequency by 2, to be used with full-wave rectifiers, e.g. a pair of IR LEDs in anti-parallel.
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public Wave(double freq, double[] data,
            int sampleFrequency, int sampleSize, int channels, boolean bigEndian,
            boolean omitTail, boolean square, boolean divide) throws InvalidArgumentException {
        if (data == null || data.length == 0)
            throw new InvalidArgumentException("Cannot create wave file from zero array.");
        double sf = sampleFrequency/1000000.0;

        int[] durationsInSamplePeriods = new int[omitTail ? data.length-1 : data.length];
        int length = 0;
        for (int i = 0; i < durationsInSamplePeriods.length; i++) {
            durationsInSamplePeriods[i] = (int) Math.round(Math.abs(sf*data[i]));
            length += durationsInSamplePeriods[i];
        }

        double c = sampleFrequency/freq;
        buf = new byte[length*sampleSize/8*channels];
        int index = 0;
        for (int i = 0; i < data.length-1; i += 2) {
            // Handle pulse, even index
            for (int j = 0; j < durationsInSamplePeriods[i]; j++) {
                double t = j/(divide ? 2*c : c);
                double fraq = t - (int)t;
                double s = square
                        ? (fraq < 0.5 ? -1.0 : 1.0)
                        : Math.sin(2*Math.PI*(fraq));
                if (sampleSize == 8) {
                    int val = (int) Math.round(Byte.MAX_VALUE*s);
                    buf[index++] = (byte) val;
                    if (channels == 2)
                        buf[index++] = (byte)-val;
                } else {
                    int val = (int) Math.round(Short.MAX_VALUE*s);
                    byte low = (byte) (val & 0xFF);
                    byte high = (byte) (val >> 8);
                    buf[index++] = bigEndian ? high : low;
                    buf[index++] = bigEndian ? low : high;
                    if (channels == 2) {
                        val = -val;
                        low = (byte) (val & 0xFF);
                        high = (byte) (val >> 8);
                        buf[index++] = bigEndian ? high : low;
                        buf[index++] = bigEndian ? low : high;
                    }
                }
            }

            // Gap, odd index
            if (!omitTail || i < data.length - 2) {
                for (int j = 0; j < durationsInSamplePeriods[i + 1]; j++) {
                    for (int ch = 0; ch < channels; ch++) {
                        buf[index++] = 0;
                        if (sampleSize == 16)
                            buf[index++] = 0;
                    }
                }
            }
        }
        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleFrequency, sampleSize, channels, sampleSize/8*channels, sampleFrequency, bigEndian);
    }

    /**
     * Generates a wave audio file from its arguments.
     *
     * @param irSequence ModulatedIrSequence to be used.
     * @param sampleFrequency Sample frequency of the generated wave file.
     * @param sampleSize Sample size (8 or 16) in bits of the samples.
     * @param channels If == 2, generates two channels in perfect anti-phase.
     * @param bigEndian if true, use bigendian byte order for 16 bit samples.
     * @param omitTail If true, the last trailing gap will be omitted.
     * @param square if true, use a square wave for modulation, otherwise a sine.
     * @param divide If true, divides the carrier frequency by 2, to be used with full-wave rectifiers, e.g. a pair of IR LEDs in anti-parallel.
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    public Wave(ModulatedIrSequence irSequence,
                int sampleFrequency, int sampleSize, int channels, boolean bigEndian,
                boolean omitTail, boolean square, boolean divide) throws InvalidArgumentException {
        this(irSequence.getFrequency(), irSequence.toDoubles(),
                sampleFrequency, sampleSize, channels, bigEndian,
                omitTail, square, divide);
    }

  /**
     * Generates a wave audio file from its arguments.
     *
     * @param irSequence ModulatedIrSequence to be used.
     * @param audioFormat AudioFormat bundling sampleFrequency, sample size, channels, and bigEndian together.
     * @param omitTail If true, the last trailing gap will be omitted.
     * @param square if true, use a square wave for modulation, otherwise a sine.
     * @param divide If true, divides the carrier frequency by 2, to be used with full-wave rectifiers, e.g. a pair of IR LEDs in anti-parallel.
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     */
    public Wave(ModulatedIrSequence irSequence,
                AudioFormat audioFormat,
                boolean omitTail,
                boolean square, boolean divide) throws InvalidArgumentException {
        this(irSequence,
                (int) audioFormat.getSampleRate(), audioFormat.getSampleSizeInBits(),
                audioFormat.getChannels(),
                audioFormat.isBigEndian(),
                omitTail, square, divide);
    }

    // set up integer data (left and right channel) from the byte array.
    private int[][] computeData() {
        int channels = audioFormat.getChannels();
        int sampleSize = audioFormat.getSampleSizeInBits();
        AudioFormat.Encoding encoding = audioFormat.getEncoding();
        boolean bigEndian = audioFormat.isBigEndian();
        int[][] data = new int[noFrames][channels];
        if (encoding == AudioFormat.Encoding.PCM_UNSIGNED && sampleSize != 8) {
            logger.severe("Case not yet implemented");
            return null;
        }

        for (int frame = 0; frame < noFrames; frame++) {
            if (sampleSize == 8) {
                for (int ch = 0; ch < channels; ch++) {
                    int val = buf[channels*frame + ch];
                    if (encoding == AudioFormat.Encoding.PCM_UNSIGNED)
                        val += (val < 0) ? 128 : -128;
                    data[frame][ch] = val;
                }
            } else {
                // sampleSize == 16
                for (int ch = 0; ch < channels; ch++) {
                    int baseIndex = 2*(channels*frame + ch);
                    int high = buf[bigEndian ? baseIndex : baseIndex+1]; // may be negative
                    int low  = buf[bigEndian ? baseIndex+1 : baseIndex]; // consider as unsigned
                    if (low < 0)
                        low += 256;
                    int value = 256*high + low;
                    data[frame][ch] = value;
                }
            }
        }
        return data;
    }

    /**
     * Analyzes the data and computes a ModulatedIrSequence. Generates some messages on stderr.
     *
     * @param stream
     * @param divide consider the carrier as having its frequency halved or not?
     * @return ModulatedIrSequence computed from the data.
     */
    public ModulatedIrSequence analyze(PrintStream stream, boolean divide) {
        double sampleFrequency = audioFormat.getSampleRate();
        int channels = audioFormat.getChannels();
        stream.println("Format is: " + audioFormat.toString() + ".");
        stream.println(String.format("%d frames = %7.6f seconds.", noFrames, noFrames/sampleFrequency));
        int[][] data = computeData();

        if (channels == 2) {
            int noDiffPhase = 0;
            int noDiffAntiphase = 0;
            int noNonNulls = 0;

            for (int i = 0; i < noFrames; i++) {
                if (data[i][0] != 0 || data[i][1] != 0) { // do not count nulls
                    noNonNulls++;
                    if (data[i][0] != data[i][1])
                        noDiffPhase++;
                    if (data[i][0] != -data[i][1])
                        noDiffAntiphase++;
                }
            }
            stream.println("This is a 2-channel file. Left and right channel are "
                    + (noDiffPhase == 0 ? "perfectly in phase."
                    : noDiffAntiphase == 0 ? "perfectly in antiphase."
                    : "neither completely in nor out of phase. Pairs in-phase:"
                    + (noNonNulls - noDiffPhase) + ", pairs anti-phase: " + (noNonNulls - noDiffAntiphase)
                    + " (out of " + noNonNulls + ")."));
            stream.println("Subsequent analysis will be base on the left channel exclusively.");
        }

        // Search the largest block of oscillations
        ArrayList<Integer> durations = new ArrayList<>(noFrames);
        int bestLength = -1; // length of longest block this far
        int bestStart = -1;
        boolean isInInterestingBlock = true;
        int last = -1111111;
        int epsilon = audioFormat.getSampleSizeInBits() == 8 ? epsilon8Bit : epsilon16Bit;

        int firstNonNullIndex = 0; // Ignore leading silence, it is silly.
        while (data[firstNonNullIndex][0] == 0)
            firstNonNullIndex++;
        if (firstNonNullIndex > 0)
            stream.println("The first " + firstNonNullIndex + " sample(s) are 0, ignored.");

        int beg = firstNonNullIndex; // start of current block
        for (int i = firstNonNullIndex; i < noFrames; i++) {
            int value = data[i][0];
            // two consecutive zeros -> interesting block ends
            if (((Math.abs(value) <= epsilon && Math.abs(last) <= epsilon) || (i == noFrames - 1)) && isInInterestingBlock) {
                isInInterestingBlock = false;
                // evaluate just ended block
                int currentLength = i - 1 - beg;
                if (currentLength > bestLength) {
                    // longest this far
                    bestLength = currentLength;
                    bestStart = beg;
                }
                durations.add((int)Math.round(currentLength/sampleFrequency*1000000.0));
                beg = i;
            } else if (Math.abs(value) > epsilon && !isInInterestingBlock) {
                // Interesting  block starts
                isInInterestingBlock = true;
                int currentLength = i - 1 - beg;
                durations.add((int) Math.round(currentLength/sampleFrequency*1000000.0));
                beg = i;
            }

            last = value;
        }
        if (!isInInterestingBlock && noFrames - beg > 1)
            durations.add((int)Math.round((noFrames - beg)/sampleFrequency*1000000.0));
        if (durations.size() % 2 == 1)
            durations.add(0);

        // Found the longest interesting block, now evaluate frequency
        int signchanges = 0;
        last = 0;
        for (int i = 0; i < bestLength; i++) {
            int indx = i + bestStart;
            int value = data[indx][0];
            if (value != 0) {
                if (value*last < 0)
                    signchanges++;
                last = value;
            }
        }
        double carrierFrequency = (divide ? 2 : 1)*sampleFrequency *  signchanges/(2*bestLength);
        stream.println("Carrier frequency estimated to " + Math.round(carrierFrequency) + " Hz.");

        int arr[] = new int[durations.size()];
        int ind = 0;

        for (Integer val : durations) {
            arr[ind] = val;
            ind++;
            logger.log(Level.FINEST, "{0}", val);
        }
        try {
            //return new IrSignal(arr, arr.length/2, 0, (int) Math.round(carrierFrequency));
            return new ModulatedIrSequence(arr, carrierFrequency);
        } catch (OddSequenceLengthException ex) {
            // cannot happen, we have insured that the data has even size.
            return null;
        }
    }

    /**
     * Print the channels to a tab separated text file, for example for debugging purposes.
     * This file can be imported in a spreadsheet.
     *
     * @param dumpfile Output file.
     * @throws FileNotFoundException
     */
    public void dump(File dumpfile) throws FileNotFoundException {
        int data[][] = computeData();
        double sampleRate = audioFormat.getSampleRate();
        int channels = audioFormat.getChannels();
        try (PrintStream stream = new PrintStream(dumpfile, "US-ASCII")) {
            for (int i = 0; i < noFrames; i++) {
                stream.print(String.format("%d\t%8.6f\t", i, i / sampleRate));
                for (int ch = 0; ch < channels; ch++)
                    stream.print(data[i][ch] + (ch < channels - 1 ? "\t" : "\n"));
            }
        } catch (UnsupportedEncodingException ex) {
            throw new InternalError();
        }
    }

    /**
     * Write the signal to the file given as argument.
     * @param file Output File.
     */
    public void export(File file) {
        ByteArrayInputStream bs = new ByteArrayInputStream(buf);
        bs.reset();

        AudioInputStream ais = new AudioInputStream(bs, audioFormat, (long) buf.length/audioFormat.getFrameSize());
        try {
            int result = AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
            if (result <= buf.length)
                logger.log(Level.SEVERE, "Wrong number of bytes written: {0} < {1}", new Object[]{result, buf.length});
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

   /**
    * Sends the generated wave to the line in argument, if possible.
    * @param line Line to used. Should be open, and remains open. User must make sure AudioFormat is compatible.
    * @throws LineUnavailableException
    * @throws IOException
    */
    public void play(SourceDataLine line) throws LineUnavailableException, IOException {
        line.start();
        int bytesWritten = line.write(buf, 0, buf.length);
        if (bytesWritten != buf.length)
            throw new IOException("Not all bytes written");
        line.drain();
    }

    /**
     * Sends the generated wave to the local machine's audio system, if possible.
     * @throws LineUnavailableException
     * @throws IOException
     */
    public void play() throws LineUnavailableException, IOException {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(audioFormat)) {
            line.open(audioFormat);
            play(line);
        }
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-1", "--nodivide"}, description = "Do not divide modulation frequency")
        boolean dontDivide = false;

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message")
        boolean helpRequensted = false;

        @Parameter(names = {"-p", "--play"}, description = "Send the generated wave to the audio device of the local machine")
        boolean play = false;

        @Parameter(names = {"-v", "--version"}, description = "Display version information")
        boolean versionRequested;

//        @Parameter(description = "[parameters]")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private ArrayList<String> parameters = new ArrayList<>(64);
    }
}
