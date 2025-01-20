# HarcHardware

HarcHardware is a collection of Java routines for accessing hardware,
files, network resources etc.
It is documented in its [API documentation](https://bengtmartensson.github.io/HarcHardware/).

## Main routine

There is also a main routine, which allows for invoking the functionality from the command line, or from a script.
The usage message, which is given at the end of this page, summarizes its use.
The standard installations also installs a command `harchardware` that can be used to invoke the "program".
Also, if the AppImage is called as `harchardware` (for example through a symbolic link),
the HarcHardware main routine will be invoked.

As many complex command line program, it is organized as a program with sub-commands.
These sub-commands have their own options, as described by the help facility.
There are also a number of common options.
Most importantly, they select the particular hardware used for sending or receiving the IR signals.
In general, using the --class argument uses reflections to use a named Java class for invoking.

These are:

*    _capture_ Receive a IR signal using demodulating receiver.
*    _commands_ Get the commands contained in the selected remote.
*    _help_ Describe the syntax of program and commands.
*    _receive_ Receive a IR signal using demodulating receiver.
*    _remotes_ Get the name of the contained remotes.
*    _send_ Transmit an IrSignal using selected hardware.
*    _transmit_ (synonym to send) Transmit an IrSignal using selected hardware.
*    _version_ Report version and license, or, if the --class argument is given, the version of the hardware.

## Examples
### List the common options

    harchardware help --common

### Produce full help text
This is a long text (see below)...

    harchardware help

### Produce version- and license information

    harchardware version

### Send three repeats of Sony TV power toggle signal to a GlobalCache network IR sender
Requires a GlobalCache hardware at the IP address 192.168.1.70. (Actually, this is the default, and could thus have been left out.)

    harchardware --class GlobalCache --ip 192.168.1.70 send --protocol Sony12 --names D=1,F=21 --count 3

### Send a Yamaha power_on to a /dev/lirc device on its transmitter 2
Requires `/dev/lirc` support. Lirc should not be running.

    harchardware ---devslashlirc --device /dev/lirc0 transmit --protocol NEC1 --names D=122,F=29 --transmitter 2

### Capture an IR signal from a connected Arduino Nano (using non-demodulating receiver), decode, and output Proto Hex too.
Assumes for example an Arduino Nano connected at /dev/ttyUSB0.

    harchardware --arduino --device /dev/ttyUSB0  capture  --decode --prontoNow

### Capture an IR signal with an IrWidget, invoke analyzer, and repeatfinder, output as raw too.
Assumes an IrWidget connected at `/dev/ttyUSB0`.

    harchardware --irwidget --device /dev/ttyUSB0  capture  --analyze --raw --repeatfinder

## Copyright and License

Copyright (c) Bengt Martenson 2010-2025. Licensed under the GPL version 3 license or later. Some contained third-party components have different, but compatible, licenses. This is documented elsewhere.

## Usage message:
Note that despite `transmit` and `send` being synonyms, they are both fully documented in the generated text.
```
Usage: HarcHardware [options] [command] [command options]
  Options:
    -a, --absolutetolerance
      Absolute tolerance in microseconds, used when comparing durations.
      Default: 100.0.
    -H, --home, --applicationhome, --apphome
      Set application home (where files are located)
    --arduino, --girs
      Implies --class GirsClient. If --device is not given, the device default
      will be selected.
      Default: false
    --audio
      Implies --class IrAudioDevice.
      Default: false
    -B, --begintimeout
      Set begin timeout (in ms).
    -b, --blacklist
      List of protocols to be removed from the data base
    --capturemaxlength
      Set max capturelength.
    -C, --class
      Class to be instantiated; must implement IHarcHardware and possibly more
      (dependent on requested function).
    --cf, --commandfusion
      Implies --class CommandFusion. If --device is not given, the device
      default will be selected.
      Default: false
    -c, --configfiles
      Pathname(s) of IRP database file(s) in XML format. Default is the one in
      the jar file. Can be given several times.
    --describe
      Print a possibly longer documentation for the present command.
    -d, --device
      Device name, e.g. COM7: or /dev/ttyACM0,
    --devslashlirc
      Implies --class DevSlashLirc. If --device is not given, the device
      default will be selected.
      Default: false
    -E, --endingtimeout
      Set ending timeout (in ms).
    -f, --frequencytolerance
      Frequency tolerance in Hz. Negative disables frequency check. Default:
      2000.0.
    --globalcache
      Implies --class GlobalCache. If --device is not given, the device
      default will be selected.
      Default: false
    -h, -?, --help
      Print help for this command.
    -e, --encoding, --iencoding
      Encoding used to read input.
      Default: UTF-8
    -I, --ip
      IP address or name.
    -i, --irp
      Explicit IRP string to use as protocol definition.
    --irremote
      Implies --class IRrecvDumpV2. If --device is not given, the device
      default will be selected.
      Default: false
    --irtoy, --IrToy
      Implies --class IrToy. If --device is not given, the device default will
      be selected.
      Default: false
    --irtrans, --IrTrans
      Implies --class IrTrans. If --ip is not given, the device default will
      be selected.
      Default: false
    --irwidget, --IrWidget
      Implies --class IrWidget. If --device is not given, the device default
      will be selected.
      Default: false
    --logclasses
      List of (fully qualified) classes and their log levels, in the form
      class1:level1|class2:level2|...
      Default: <empty string>
    -L, --logfile
      Log file. If empty, log to stderr.
    -F, --logformat
      Log format, as in class java.util.logging.SimpleFormatter.
      Default: [%2$s] %4$s: %5$s%n
    -l, --loglevel
      Log level { OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
      }
      Default: WARNING
    --min-leadout
      Threshold for leadout when decoding. Default: 20000.0.
    -g, --minrepeatgap
      Minimum gap required to end a repetition.
      Default: 5000.0
    --oencoding
      Encoding used in generated output.
      Default: UTF-8
    -o, --output
      Name of output file. Default: stdout.
    -O, --override
      Let given command line parameters override the protocol parameters in
      IrpProtoocols.xml
      Default: false
    -p, --port
      Port number (TCP or UDP).
    -q, --quiet
      Quitest possible operation, typically to be used from scripts.
      Default: false
    -r, --relativetolerance
      Relative tolerance as a number < 1. Default: 0.3.
    -T, --timeout
      Timeout in milliseconds.
    --validate
      Validate IRP database files against the schema, abort if not valid.
      Default: false
    -v, --verbose
      Execute commands verbosely.
      Default: false
    --version
      Report version. Deprecated; use the command "version" instead.
      Default: false
    -x, --xmllog
      Write the log in XML format.
      Default: false
  Commands:
    help      Describe the syntax of program and commands.
      Usage: help [options] commands
        Options:
          --describe
            Print a possibly longer documentation for the present command.
          -h, -?, --help
            Print help for this command.
          -l, --logging
            Describe the logging related options only.
            Default: false
          -c, --common, --options
            Describe the common options only.
            Default: false
          -s, --short
            Produce a short usage message.
            Default: false

    transmit      Transmit an IrSignal using selected hardware.
      Usage: transmit [options] remaining arguments
        Options:
          -c, --command
            Name of command (residing in the named remote) to send.
          -#, --count
            Number of times to send sequence
            Default: 1
          --describe
            Print a possibly longer documentation for the present command.
          -F, --file
            File name of file containing arguments; one send per line
          -f, --frequency
            Frequency in Hz, for use with raw signals
          -h, -?, --help
            Print help for this command.
          -n, --names
            Parameter values for the protocol to be rendered, in the syntax of
            a name engine.
          -p, --protocol
            protocol name to be rendered
          -r, --remote
            Name of remote for command
          -t, --transmitter
            Transmitter, semantic device dependent

    send      Transmit an IrSignal using selected hardware.
      Usage: send [options] remaining arguments
        Options:
          -c, --command
            Name of command (residing in the named remote) to send.
          -#, --count
            Number of times to send sequence
            Default: 1
          --describe
            Print a possibly longer documentation for the present command.
          -F, --file
            File name of file containing arguments; one send per line
          -f, --frequency
            Frequency in Hz, for use with raw signals
          -h, -?, --help
            Print help for this command.
          -n, --names
            Parameter values for the protocol to be rendered, in the syntax of
            a name engine.
          -p, --protocol
            protocol name to be rendered
          -r, --remote
            Name of remote for command
          -t, --transmitter
            Transmitter, semantic device dependent

    receive      Receive a IR signal using demodulating receiver.
      Usage: receive [options]
        Options:
          -a, --analyze
            Send the received signal into the analyzer.
            Default: false
          -c, --cleaner
            Clean received signals
            Default: false
          -#, --count
            Capture this many signals before exiting.
          -d, --decode
            Attempt to decode the signal received.
            Default: false
          --describe
            Print a possibly longer documentation for the present command.
          -f, --frequency
            Overriding frequency for received signal.
            Default: 38000.0
          -h, -?, --help
            Print help for this command.
          -p, --pronto
            Output signal as Pronto Hex.
            Default: false
          -r, --raw
            Output signal in raw form.
            Default: false
          -R, --repeatfinder
            Invoke the repeatfinder on the captured signal.
            Default: false

    capture      Receive a IR signal using demodulating receiver.
      Usage: capture [options]
        Options:
          -a, --analyze
            Send the received signal into the analyzer.
            Default: false
          -c, --cleaner
            Clean received signals
            Default: false
          -#, --count
            Capture this many signals before exiting.
          -d, --decode
            Attempt to decode the signal received.
            Default: false
          --describe
            Print a possibly longer documentation for the present command.
          -h, -?, --help
            Print help for this command.
          -p, --pronto
            Output signal as Pronto Hex.
            Default: false
          -r, --raw
            Output signal in raw form.
            Default: false
          -R, --repeatfinder
            Invoke the repeatfinder on the captured signal.
            Default: false

    remotes      Get the name of the contained remotes.
      Usage: remotes [options]
        Options:
          --describe
            Print a possibly longer documentation for the present command.
          -h, -?, --help
            Print help for this command.

    commands      Get the commands contained in the selected remote.
      Usage: commands [options] Remote
        Options:
          --describe
            Print a possibly longer documentation for the present command.
          -h, -?, --help
            Print help for this command.

    version      Report version and license, or, if the --class argument is
            given, the version of the hardware.
      Usage: version [options]
        Options:
          --describe
            Print a possibly longer documentation for the present command.
          -h, -?, --help
            Print help for this command.
          -s, --short
            Issue only the version number of the program proper.
            Default: false
```