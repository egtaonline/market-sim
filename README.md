# MarketSim build tutorial

This tutorial explains how to download, build, and run ```MarketSim```.

We assume that you have a version of ```Git``` and ```Eclipse```, and are in a Unix-like environment.

```MarketSim``` requires ```Java 1.6```, and is tested with version ```1.6.0_65-b14-468```.

## 1. Open a Unix-like terminal

First, create a new directory if desired, enter that directory, and clone the MarketSim repository into it. Then check out branch ```marketsim1```.
```bash
mkdir msimDir
cd msimDir
git clone https://github.com/egtaonline/market-sim.git
cd market-sim
git checkout -b marketsim1 origin/marketsim1
```

## 2. Open Eclipse

This tutorial was tested using Eclipse Mars (v 4.5), [still available here](https://eclipse.org/mars/).

First, we create a new Workspace using the ```market-sim``` directory.

```
File > Switch Workspace > Other... >
	Browse > select msimDir/market-sim > OK
```

Next, we set the Workspace to refresh automatically with Git changes.

```
Eclipse > Preferences > General > Workspace >
	Refresh using native hooks or polling > OK
```

Now we import the ```MarketSim``` code base, by building from an Ant file.

```
File > New > Project > Java > Java Project from Existing Ant Buildfile >
	Browse > hft-sim/build.xml > Finish
```

Now we configure the build path. We will do the following:
* Change the Java Runtime Environment (JRE) to Java 1.6
* Add the ```hft-sim/test``` folder as a source folder
* Change the source output folder to the project's default output folder

```
right-click hft-sim > Properties > Java Build Path
	Libraries > remove JRE_LIB
	Libraries > add Library > JRE System Library >
    	Alternate JRE: Java SE 6 [1.6.0_65-b14-468]
	Source > Add folder > test
	Source > output folder > Edit > Project's default output folder
	OK
```

Next we will change the compiler setting to Java 1.6.

```
right-click hft-sim > Properties > Java Compiler >
	Enable project specific settings >
	Compiler compliance level: 1.6 > OK > Yes
```

Now we are ready to run the unit tests on our build.

```
right-click test > Run as > JUnit test
```

All tests should pass.

Next we set up an example run of the simulator.

```
right-click hft-sim > Properties > Run/Debug Settings > 
	New > Java Application > OK
	Name: Run Simulator
	Search > SystemManager - systemmanager > OK
	Arguments tab > Program arguments: 
    	simulations/test 1
    > OK > OK
```

Now we can run the application we created.

```
right-click hft-sim > Run as > Java application
```

To see the result, refresh the output folder.

```
simulations/test > Refresh
```

The result should be located at ```simulations/test/observation1.json```.

## 3. Open a Unix-like terminal, again

Next we can perform local simulations of a role-symmetric mixed-strategy profile. We will use the example stored in ```hft-sim/oneE25A```.

```bash
bash
cd msimDir/hft-sim
```

First, we will generate pure-strategy profiles by sampling from the mixed-strategy profile. Let's generate 10 pure-strategy profiles. (For better accuracy, we would want to generate many more, perhaps 2500.) Note that ```oneE25A/simspec_oneE25.json``` holds parameters of the simulated environment, while ```oneE25A/eq_oneE25A.json``` holds the role-symmetric mixed-strategy profile.

```python
python sample.py -s oneE25A/simspec_oneE25.json -f oneE25A/eq_oneE25A.json -n 10 -d oneE25A/
```

Now there should be 10 directories within ```hft-sim/oneE25A``` that hold 1 pure-strategy profile each.

Next we run the simulator on each profile.

```bash
for f in oneE25A/[0-9]* ; do (./run-hft.sh "$f" 1); done
```

Now each profile directory should have an ```observation1.json``` output file. We finally aggregate the results of these outputs.

```python
python bootstrapNewSums.py oneE25A 1
```

The result gives summary statistics on the payoff for each agent type, and much more.
