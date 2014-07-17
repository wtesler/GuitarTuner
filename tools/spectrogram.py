import matplotlib.pyplot as plt
import random

with open("C:\\Users\\Will\\Documents\\log.txt") as f:
	content = f.readlines()

prefix = "D/PitchDetector("
time = 0
pitch = 0
pitches = []
times = []
freqs = []
amps = []
pixels = []
for line in content:
    if line.startswith(prefix):
        line = line[(len(prefix)+8):]
        line = line.strip("\n")
        splitLine = line.split(" ")
        if len(splitLine) > 0:
            if 'Pitch:' in splitLine[0]:
                maxAmp = 0
                for amp in amps:
                    if amp > maxAmp:
                        maxAmp = amp
                for i in range(len(freqs)):
                    R = amps[i]/maxAmp
                    B = (1 - amps[i]/maxAmp)
                    degrees = range(0, 360)
                    r = random.choice(degrees)
                    plt.plot(time, freqs[i],'.', color=[R,0,B],
                             alpha = R, markersize=25)
                    pixels.append([R,0,B, amps[i]/maxAmp])
                freqs = []
                amps = []
                pitch = float(splitLine[1])
                pitches.append(pitch)
            elif splitLine[0] == 'Time:':
                time = float(splitLine[1])
                times.append(time)
            else:
                #print(splitLine[0])
                freqs.append(float(splitLine[0]))
                amps.append(float(splitLine[1]))
plt.plot(times, pitches, '.r-', )
strings = [82.41, 110.00, 146.83, 196.00, 246.94, 329.63]
for string in strings:      
    plt.axhline(string, color='g')
mng = plt.get_current_fig_manager()
mng.resize(*mng.window.maxsize())
#plt.figure(facecolor="black")
plt.show()
