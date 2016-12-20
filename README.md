[![Build Status](https://travis-ci.org/werthdavid/audio-wakeup.svg?branch=master)](https://travis-ci.org/werthdavid/audio-wakeup)

# audio-wakeup

This is a small application that observes the line-in port of your sound card and triggers Hyperion, IFTTT, ...

The intention is that you can plug your mobile phone to your Raspberry Pi with an aux cable and set an alarm for the next morning. 
When the alarm fires, the phone will play the sound through the audio jack which will be detected by the software.
Hyperion will now increase the brightness from 0 to 255 in 2s steps to simulate sunrise like any other wake-up light.
Additionally an IFTTT event (via MakerChannel) can be triggered.

# More to come!

# Pre-Alpha

Currently the application is in pre-alpha stage! Don't expect too much!