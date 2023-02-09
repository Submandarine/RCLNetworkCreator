# RCLNetworkCreator
Crates random RCL networks for educational purposes.

This program will create an image of a random RCL network and two textfiles, one with the component values and the voltage of the voltage source,
and one with all voltages and currents that have to be calculated. The resulting files get numbered to prevent overwriting earlier results, a simple config file is crated to keep track of the numbering\
The following assumptions are made:\
-The voltage source is ideal\
-Capacitors have no resistance at t0 (immeadeately after voltage is applied) and infinite resistance at t->inf\
-Inductors have infinite resistance at t0 and no resistance at t->inf

# Parameters
The characteristics of the created circuits can be modified with the following parameters:\
-nRes: number of resistors def 7\
-nCap: number of capacitors def 1\
-nInd: number of inductors def 1\
-minPart: minimum resistance value def 5\
-maxPart: maximum resistance value def 20\
-minV: minimum voltage def 1\
-maxV: maximum voltage def 10\
-maxComp: maximum components per circuit (prevent huge simple parallel or serial circuits) def components / 3\
-maxUseless: maximum shorted resistors def 3\
-time: "t0"=evaluate at t0, "tInf": evaluate at settled state, default do both\
visual parameters:\
-cWidth: width of components def 25\
-cHeight: height of components def 50\
-lines: length of vertical connections def 15\
