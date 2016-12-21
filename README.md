[![Build Status](https://travis-ci.org/werthdavid/audio-wakeup.svg?branch=master)](https://travis-ci.org/werthdavid/audio-wakeup)

# audio-wakeup

This is a small application that observes the line-in port of your sound card and triggers Hyperion, IFTTT, ...

The intention is that you can plug your mobile phone to your Raspberry Pi with an aux cable and set an alarm for the next morning. 
When the alarm fires, the phone will play the sound through the audio jack which will be detected by the software.
Hyperion will now increase the brightness from 0 to 255 in 2s steps to simulate sunrise like any other wake-up light.
Additionally an IFTTT event (via MakerChannel) can be triggered.

# Download / Build

Grab a release from <a href="https://github.com/werthdavid/audio-wakeup/releases">here</a> or build it on your own:

* Clone the repo
```
git clone https://github.com/werthdavid/audio-wakeup.git
```    
    
* go to checked out directory 
```
cd audio-wakeup
```
    
* Build with Gradle
```
gradlew fatJar
```

* Output will be in directory *audio-wakeup/build/libs/audio-wakeup-1.X.X-SNAPSHOT-all.jar*

# Installation

tbd.

# Run

* JRE >= 1.7 required

```java -jar audio-wakeup-1.X.X-all.jar```

# Run as Service (systemd)

tbd.

# Configuration

At this point it is not (yet) possible to start the application with params. But you can create a configuration file within the same folder:

*audio-wakeup.properties*

**See audio-wakeup.example.properties**

# More to come!

# Pre-Alpha

Currently the application is in pre-alpha stage! Don't expect too much!