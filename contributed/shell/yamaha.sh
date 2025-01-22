#! /bin/bash

# Just a small convenience function for turning my desk audio amplifier on or off
# from the command line.

if [[ $# -eq 0 || "$1" = "on" ]] ; then
    FUNC="29"
else
    FUNC="30"
fi

harchardware --globalcache transmit --protocol nec1 --names D=122,F=$FUNC --transmitter 3
