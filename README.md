GuitarTuner
===========

This app is composed of 2 main components:

1. MainActivity, the app UI that shows the results of the Pitch calculations

2. PitchDetector (wear/src/PitchDetector.java) performs the transforms on the raw audio data in an attempt to calculate
pitch frequency information.

At present time, the Pitch Detector is accurate up to +/-3 points, but does not have the accuracy necessary
for tuning a guitar.
