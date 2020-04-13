"""
FRODO: a FRamework for Open/Distributed Optimization
Copyright (C) 2008-2019  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek

FRODO is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

FRODO is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


How to contact the authors: 
<https://frodo-ai.tech>
"""

# @todo Write documentation (in which format?)

import os
import time
import signal
import sys
import subprocess
import math
import glob
from shutil import copyfile

# Global variables
interrupted = False
terminated = False
java = ""
javaParams = []
generator = ""
algos = []
timeout = -1
output = ""
outFile = None
javaProcess = None

print("""FRODO  Copyright (C) 2008-2019  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek
This program comes with ABSOLUTELY NO WARRANTY.
This is free software, and you are welcome to redistribute it
under certain conditions.\n""");

drawGraphs = False
try: 
    import matplotlib
    import matplotlib.pyplot as plt
    drawGraphs = True
except ImportError: 
    sys.stderr.write("Could not find the matplotlib module; no graphs will be drawn\n")
    
# The array to compute the 95%-confidence interval for the median
# @author Jean-Yves Le Boudec
conf = [ 
    [], # for n = 0 data point, it doesn't make sense
    [1, 1], # n = 1
    ] + [ 
    [int(math.floor(n / 2.0)), int(math.floor(n / 2.0))] for n in range(2, 6) # no confidence interval for n < 6
    ] + [
    [1, 6], # n = 6
    [1, 7], [1, 7], # n = 7..8
    [2, 8], # n = 9
    [2, 9], # n = 10
    [2, 10], # n = 11
    [3, 10], # n = 12
    [3, 11], [3, 11], # n = 13..14
    [4, 12], [4, 12], # n = 15..16
    [5, 13], # n = 17
    [5, 14], # n = 18
    [5, 15], # n = 19
    [6, 15], # n = 20
    [6, 16], [6, 16], # n = 21..22
    [7, 17], [7, 17], # n = 23..24
    [8, 18], # n = 25
    [8, 19], # n = 26
    [8, 20], # n = 27
    [9, 20], # n = 28
    [9, 21], # n = 29
    [10, 21], # n = 30
    [10, 22], [10, 22], # n = 31..32
    [11, 23], [11, 23], # n = 33..34
    [12, 24], [12, 24], # n = 35..36
    [13, 25], # n = 37
    [13, 26], # n = 38
    [13, 27], # n = 39
    [14, 27], # n = 40
    [14, 28], # n = 41
    [15, 28], # n = 42
    [15, 29], # n = 43
    [16, 29], # n = 44
    [16, 30], [16, 30], # n = 45..46
    [17, 31], [17, 31], # n = 47..48
    [18, 32], [18, 32], # n = 49..50
    [19, 33], # n = 51
    [19, 34], # n = 52
    [19, 35], # n = 53
    [20, 35], # n = 54
    [20, 36], # n = 55
    [21, 36], # n = 56
    [21, 37], # n = 57
    [22, 37], # n = 58
    [22, 38], # n = 59
    [23, 39], [23, 39], # n = 60..61
    [24, 40], [24, 40], [24, 40], # n = 62..64
    [25, 41], [25, 41], # n = 65..66
    [26, 42], # n = 67
    [26, 43], # n = 68
    [26, 44], # n = 69
    [27, 44], # n = 70
    ]


def ignoreInterruption ():
    signal.signal(signal.SIGINT, signal.SIG_IGN)


def interruptionHandler (signal, frame):
    global interrupted, terminated, outFile
    if not interrupted: 
        print("""
Interruption signal caught. Waiting for all algorithms to finish solving the current problem instance... 

Why? Because, statistically, you are more likely to interrupt long runs, introducing an experimental bias. 
In other words, the algorithm you just tried to interrupt might take a long time to terminate or time out. 
Interrupting it and discarding this run could make the algorithm appear to perform better than it actually does. 

Hit CTRL+C again if you really want to abruptly interrupt the experiment. 
All experimental results for the current problem instance will be discarded. 
        """)
        interrupted = True
    elif not terminated:
        print("\nAbruptly interrupting the experiment. Killing the Java process... ")
        if not javaProcess is None:
            javaProcess.kill()
            javaProcess.wait()
        terminated = True
    else:
        if not outFile is None: 
            outFile.close()
        sys.exit(0)


def checkAlgoNameUnicity ():
    global algos
    
    if len(algos) == 0:
        sys.stderr.write("Error: no algorithm specified")
        sys.exit(1)
    
    algoNames = []
    for name in [ algo[0] for algo in algos ]: 
        if name in algoNames: 
            sys.stderr.write("Error: two algorithms have the same name `" + name + "'")
            sys.exit(1)
        algoNames += [name]


def getLowMedHigh (data):
    """ Returns the median and the bounds of the 95%-confidence intervals: [low, med, high]
    @param data     the list of data points; each data point is a tuple [timeout, value]
    """
    
    # First sort the data and look up the median index
    data = sorted(data)
    size = len(data)
    medI = int(math.floor(size / 2.0))
    if size < 71: 
        [lowI, highI] = conf[size]
    else: 
        lowI = int(math.floor(size / 2.0 - 0.980 * math.sqrt(size)))
        highI = int(math.ceil(size / 2.0 + 1 + 0.980 * math.sqrt(size)))
    
    # Check whether the median corresponds to a timeout
    if data[medI-1][0] == 1: # timeout
        return [ float("NaN"), float("NaN"), float("NaN") ]
    else: 
        return [ data[lowI-1][1], data[medI-1][1], data[highI-1][1] ]


def runAtDepth (depth, indent, genParams, saveProblems):
    global interrupted, terminated, java, javaParams, generator, algos, timeout, output, outFile, javaProcess
    
    # Check if all options have been set 
    if depth == len(genParams):
        print(indent + "Generating a problem instance using the following Java arguments:")
        print(indent + str([generator] + genParams))
        subprocess.call([java] + javaParams + [generator] + genParams, stdout = -1)
        
        # First write experimental results for the current problem instance into a temporary file
        tmpFileName = ".current_run.csv"
        if os.path.exists(tmpFileName): 
            os.remove(tmpFileName)
            
        # Run each algorithm
        indent += "\t"
        for algo in algos: 
            
            # Check whether special javaParams have been specified for this algorithm
            myJavaParams = javaParams
            if len(algo) >= 5: 
                myJavaParams = algo[4]
            
            # Run the algorithm
            print(indent + time.strftime("%H:%M:%S", time.localtime()) + " Starting " + algo[0])
            if sys.platform.startswith('win'):
                javaProcess = subprocess.Popen([java] + myJavaParams + [algo[1]] + algo[0:4] + [str(timeout), tmpFileName], creationflags = subprocess.CREATE_NEW_PROCESS_GROUP, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            else: 
                javaProcess = subprocess.Popen([java] + myJavaParams + [algo[1]] + algo[0:4] + [str(timeout), tmpFileName], preexec_fn = ignoreInterruption, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            
            out, err = javaProcess.communicate()
            if err:
                print(err.decode("utf-8"))
    
            javaProcess = None
            
            if terminated: 
                return
        
        # Copy the results to the overall output file (skipping irrelevant timeouts) and delete the tmp file
        needsHeader = not os.path.exists(output)
        outFile = open(output, "a+")
        
        if not os.path.exists(tmpFileName):
            print("ERROR: No solver produced any output file; please address the probable Java error messages above.")
            interrupted = True
            return
        tmpFile = open(tmpFileName, "r")
        
        line = tmpFile.readline()
        if needsHeader: 
            outFile.write(line)
        line = tmpFile.readline()
        while line != "": 
            thisSplit = line.split(';')
                  
            # Save the problem instance file if requested
            if saveProblems: 
                probName = thisSplit[2]
                fileName = thisSplit[3]
                copyfile(fileName, probName + "_" + fileName)
            
            # Compare the algorithms and the problem instances on this line and the next; 
            # if they are the same, skip this line (it is the timeout line)
            nextLine = tmpFile.readline()
            if nextLine == "\n": # EOF
                outFile.write(line)
                break
            
            nextSplit = nextLine.split(';')
            if thisSplit[0] != nextSplit[0] or thisSplit[2] != nextSplit[2]: # the algorithms or problem instances differ
                outFile.write(line)
                line = nextLine
                
            else: # same algorithm on the same problem instance; skip the first timeout line
                outFile.write(nextLine)
                line = tmpFile.readline()
        outFile.close()
        
        # Delete the tmp file
        tmpFile.close()
        os.remove(tmpFileName)
        
        return
    
    # Check whether we need to iterate on the depth-th option
    optList = genParams[depth]
    if not isinstance(optList, list): 
        genParams[depth] = str(optList)
        runAtDepth(depth+1, indent, genParams, saveProblems)
        return

    optBefore = genParams[0:depth]
    optAfter = genParams[depth+1:]
    
    # Iterate on the possible values for this option
    for opt in optList: 
        
        if interrupted:
            return
    
        print(indent + "Picking " + str(opt) + " from " + str(optList))
        runAtDepth(depth+1, indent+"\t", optBefore + [str(opt)] + optAfter, saveProblems)


def runFromRepo (java_i, javaParams_i, repoPath, nbrRuns, algos_i, timeout_i, output_i):
    """Starts the experiment based on an input repository of problem instances
    @param java_i             the command line to call Java
    @param javaParams_i        the list of parameters to be passed to the JVM. Example: ["-Xmx2G", "-classpath", "my/path"]
    @param repoPath         the path to a folder containing input problem files (without any trailing slash)
    @param nbrRuns         the number of times each algorithm is run against each problem instance 
    @param algos_i             the list of algorithms; each algorithm is [display name, solver class name, agent configuration file, javaParams] (with javaParams optional)
    @param timeout_i         the timeout in seconds
    @param output_i         the CSV file to which the statistics should be written
    """
    
    # Read the list of input problem instance files from the repository folder
    filenames = [ filename for filename in glob.glob(repoPath + "/*") if not os.path.isdir(filename) ]
    
    # Set the name of the intermediate file that will be passed to the algorithms
    probFile = ".fromRepo.xcsp"
    algos = [ algo[0:3] + [probFile] + algo[4:4] for algo in algos_i ] # insert the probFile into the list of algos
    
    run(java_i, javaParams_i, "frodo2.benchmarks.FileCopier", [filenames, probFile], nbrRuns, algos, timeout_i, output_i)


def run (java_i, javaParams_i, generator_i, genParams, nbrProblems, algos_i, timeout_i, output_i, saveProblems = False):
    """Starts the experiment
    @param java_i             the command line to call Java
    @param javaParams_i        the list of parameters to be passed to the JVM. Example: ["-Xmx2G", "-classpath", "my/path"]
    @param generator_i         the class name for the random problem generator
    @param genParams_i         the list of options for the random problem generator. Each option is either a value, or a list of values. 
    @param nbrProblems         the number of runs
    @param algos_i             the list of algorithms; each algorithm is [display name, solver class name, agent configuration file, problem file, javaParams] (with javaParams optional)
    @param timeout_i         the timeout in seconds
    @param output_i         the CSV file to which the statistics should be written
    @param saveProblems     whether to save all problem instance files
    """
    
    # @todo Eventually remove the solver from the algorithm description (after standardizing the stats gatherer)
    
    # @todo How to show and update the graphs as the experiment is running?
    
    # @todo It should be possible to run some of the algorithms from a JAR, others from the src folder and plot pairwise difference to compare FRODO versions
    
    # Set the values of the global variables
    global interrupted, java, javaParams, generator, algos, timeout, output
    java = java_i
    javaParams = javaParams_i
    generator = generator_i
    algos = algos_i
    timeout = timeout_i
    output = output_i

    checkAlgoNameUnicity()
    
    # Catch interruptions to let the algorithms finish on the current problem instance
    signal.signal(signal.SIGINT, interruptionHandler)
    
    for run in range(1, nbrProblems+1): 
        
        if interrupted: 
            return;

        print("Run " + str(run) + "/" + str(nbrProblems))
        runAtDepth(0, "\t", genParams, saveProblems)
        

def plotScatter (resultsFile, xAlgo, yAlgo, metricsCol, timeouts = True, block = True, loglog = True):
    """ Plots one algorithm against another in a log-log scatter plot
    @param resultsFile   the CSV file containing the experimental results
    @param xAlgo         the algorithm to be put on the x axis
    @param yAlgo         the algorithm to be put on the y axis
    @param metricsCol    the index of he column in the CSV file containing the comparison metrics
    @param timeouts      if True, displays the timeouts; otherwise they are ignored (which might produce more readable graphs)
    @param block         whether to block for the matplotlib window to be closed to continue (default = True)
    @parma loglog        whether to plot the results using log/log scales (default = True)
    """
    
    global drawGraphs
    
    if not os.path.isfile(resultsFile): 
        return
    file = open(resultsFile)
    
    # Read the column names
    headers = file.readline().split(';')
    metricsName = headers[metricsCol]
    
    # results = { instance1 : [xAlgo, yAlgo], ..., instanceN : [xAlgo, yAlgo] }
    results = dict()
    
    # Read the file line by line
    while True:
        line = file.readline()
        if line == "":
            break
           
        # Skip timeouts if required
        lineSplit = line.split(';')
        if not timeouts:
            timeout = int(lineSplit[1]) # 0 = no timeout; 1 = timeout
            if timeout == 1:
                continue
        
        # Parse the algorithm name
        algoName = lineSplit[0]
        if algoName == xAlgo:
            algoIndex = 0
        elif algoName == yAlgo:
            algoIndex = 1
        else:
            continue    # skip algorithms that are not xAlgo nor yAlgo
        
        # Parse the instance name and the metrics value
        instanceName = lineSplit[2]
        metricsStr = lineSplit[metricsCol]
        try: 
            metricsVal = float(metricsStr)
        except: 
            print("Invalid metrics value on column " + str(metricsCol) + " for algorithm '" + algoName + "' on instance '" + instanceName + "': " + metricsStr)
            metricsVal = float("NaN")
        
        # Record the result
        if instanceName in results:
            coord = results[instanceName]
        else:
            coord = [float("NaN"), float("NaN")]
            results[instanceName] = coord
        coord[algoIndex] = metricsVal
    
    if drawGraphs:
        plotDataScatter(results, xAlgo, yAlgo, metricsName, block, loglog)
    else:
        saveDataScatter(resultsFile, results, xAlgo, yAlgo, metricsName)
    
    
def plot (resultsFile, xCol, yCol, block = True, ylog = True):
    """ Plots the results
    @param resultsFile     the CSV file containing the experimental results
    @param xCol         the index of the column in the CSV file to be used for the x axis (the first column has index 0)
    @param yCol         the index of the column in the CSV file to be used for the y axis (the first column has index 0)
    @param block         whether to block for the matplotlib window to be closed to continue (default = True)
    @param ylog         whether to use a log scale for the y axis
    """
    
    # @todo Allow to input column names rather than column indexes, and allow to input lists to get multiple graphs
    # @todo Make it possible to set figure size, dpi, rcParams, xticks, colors fmts, ylim, ncol, the order of the algorithms in the legend
    
    global drawGraphs
    
    if not os.path.isfile(resultsFile): 
        return
    file = open(resultsFile)
    
    # Read the column names
    headers = file.readline().split(';')
    if len(headers) <= 1:
        print("ERROR: No headers found in the results file `" + resultsFile + "'")
        return
    xIndex = xCol
    yIndex = yCol
    xName = headers[xIndex]
    yName = "median " + headers[yIndex]
    
    # if drawGraphs:     results = { algoName : { xValue : [[timeout1, yValue1], ..., [timeoutN, yValueN]] } }
    # else:             results = { xValue : { algoName : [[timeout1, yValue1], ..., [timeoutN, yValueN]] } }
    results = dict()
    
    # Read the file line by line
    xMin = float("infinity")
    xMax = float("-infinity")
    while True:
        line = file.readline()
        if line == "": 
            break
        
        # Parse the algorithm name and the (x, y) values
        lineSplit = line.split(';')
        algoName = lineSplit[0]
        xValue = lineSplit[xIndex]
        yValue = float(lineSplit[yIndex])
        timeout = int(lineSplit[1]) # 0 = no timeout; 1 = timeout
        
        x = float(xValue)
        xMin = min(xMin, x)
        xMax = max(xMax, x)
        
        if drawGraphs:
            # Get the data for this algorithm, or initialize it if necessary
            # data = { xValue : [[timeout1, yValue1], ..., [timeoutN, yValueN]] }
            if algoName in results:
                data = results[algoName]
            else:
                data = dict()
                results[algoName] = data
            
            # Get the data for this xValue, or initialize it if necessary
            # yValues = [[timeout1, yValue1], ..., [timeoutN, yValueN]]
            if xValue in data:
                yValues = data[xValue]
            else:
                yValues = []
                data[xValue] = yValues

        else:
            # Get the data for this xValue, or initialize it if necessary
            # data = { algoName : [[timeout1, yValue1], ..., [timeoutN, yValueN]] }
            if xValue in results:
                data = results[xValue]
            else:
                data = dict()
                results[xValue] = data
            
            # Get the data for this algorithm, or initialize it if necessary
            # yValues = [[timeout1, yValue1], ..., [timeoutN, yValueN]]
            if algoName in data:
                yValues = data[algoName]
            else:
                yValues = []
                data[algoName] = yValues
        
        # Record the value
        yValues += [[timeout, yValue]]
    
    if drawGraphs: 
        plotData(results, xMin, xMax, xName, yName, block, ylog)
    else:
        saveData(resultsFile, results, xName, yName)
    
    
def plotDataScatter (results, xAlgo, yAlgo, metricsName, block, loglog):
    """
    @param results         { instance1 : [xAlgo, yAlgo], ..., instanceN : [xAlgo, yAlgo] }
    """
    
    fig = plt.figure()
    
    if loglog: 
        axes = fig.add_subplot(111, xscale = "log", yscale = "log")
    else:
        axes = fig.add_subplot(111)
    
    # Collect the x and y values
    xValues = []
    yValues = []
    xyMin = float("infinity")
    xyMax = float("-infinity")
    for xValue, yValue in results.values():
        if not math.isnan(xValue) and not math.isnan(yValue) and (not loglog or (xValue > 0 and yValue > 0)):
            xValues += [xValue]
            yValues += [yValue]
            xyMin = min(xyMin, xValue, yValue)
            xyMax = max(xyMax, xValue, yValue)

    if not len(xValues) == 0: 
        plt.scatter(xValues, yValues, marker = "+")
    
        # Set the limits of the axes to make sure everything is visible and the axes are square
        if loglog:
            xyMin = math.pow(10, math.floor(math.log10(xyMin)))
            xyMax = math.pow(10, math.ceil(math.log10(xyMax)))
        else: 
            margin = abs(xyMax - xyMin) * 0.1
            xyMin = xyMin - margin;
            xyMax = xyMax + margin
        axes.set_xlim(xyMin, xyMax)
        axes.set_ylim(xyMin, xyMax)
    
        # Plot the y = x line
        plt.plot([xyMin, xyMax], [xyMin, xyMax], "--")
    
    plt.grid(which="major")
    plt.xlabel(xAlgo)
    plt.ylabel(yAlgo)
    plt.title(metricsName)

    plt.show(block = block)


def plotData (results, xMin, xMax, xName, yName, block, ylog = True):
    """
    @param results         { algoName : { xValue : [[timeout1, yValue1], ..., [timeoutN, yValueN]] } }
    """
    
#     fig = plt.figure(figsize=(4, 2.5), dpi=150) # @todo Make this configurable
    fig = plt.figure()
    
#     # @todo Make the following optional and configurable
#     plt.rcParams["font.size"] = 9
#     plt.rcParams["font.family"] = "Times New Roman"
#     plt.rcParams["text.usetex"] = True
    
    if ylog:
        axes = fig.add_subplot(111, yscale = "log")
    else: 
        axes = fig.add_subplot(111)

    # Compute the margins on the x-axis to make the confidence intervals visible
    margin = .025 * (xMax - xMin)
    xMin -= margin
    xMax += margin
    axes.set_xlim(xMin, xMax)
    
    # Compute and plot the median and confidence intervals
    for algoName, data in results.items():
        
        # plotData = [ [xValue1, yMin1, yMed1, yPlus1], [xValue2,... ] ]
        plotData = []
        
        for xValue, yValues in data.items():
            [yLow, yMed, yHigh] = getLowMedHigh(yValues)
            plotData += [ [float(xValue), yMed - yLow, yMed, yHigh - yMed] ]
        
        plotData = sorted(plotData)
        xValues = [ xValue for [xValue, yMin, yMed, yPlus] in plotData ]
        yMeds = [ yMed for [xValue, yMin, yMed, yPlus] in plotData ]
        yMins = [ yMin for [xValue, yMin, yMed, yPlus] in plotData ]
        yPluses = [ yPlus for [xValue, yMin, yMed, yPlus] in plotData ]
        
        plt.errorbar(xValues, yMeds, yerr = [yMins, yPluses], label = algoName)
    
    # @todo Move the legend outside of the graph
    leg = plt.legend(loc='best', numpoints=1, columnspacing=1, labelspacing=.5, handletextpad=0.5)
    
    # @todo Make the following configurable
#     for t in leg.get_texts():
#         t.set_fontsize(7)
    
    plt.grid(which="major")
    axes.xaxis.grid(False) 
    plt.xlabel(xName)
    plt.ylabel(yName)

    plt.show(block = block)
    
    
def saveDataScatter (resultsFile, results, xAlgo, yAlgo, metricsName):
    """
    @param results         { instance1 : [xAlgo, yAlgo], ..., instanceN : [xAlgo, yAlgo] }
    """
    
    # Open the output file
    outFilePath = "figure_data_scatter" + resultsFile;
    outFile = open(outFilePath, 'w')
    
    # Write the metrics name and the algorithm names
    outFile.write(metricsName + "\n")
    outFile.write(xAlgo + ";" + yAlgo + "\n")
    
    # Write the data
    for x, y in results.values():
        outFile.write(str(x) + ";" + str(y) + "\n")
    
    print("(Over)wrote " + outFilePath)
    outFile.close()


def saveData (resultsFile, results, xName, yName):
    """
    @param results         { xValue : { algoName : [[timeout1, yValue1], ..., [timeoutN, yValueN]] } }
    """
    
    # Converts the results to the format [ [ xValue1, { algoName1 : [[timeout1, yValue1], ..., [timeoutN, yValueN]] } ], [ xValue2,... 
    resultsList = sorted([ [float(xValue), data] for xValue, data in results.items() ])
    print(resultsList)
    
    # Open the output file
    outFilePath = "figure_data_" + resultsFile;
    outFile = open(outFilePath, 'w')
    
    # Write the y-axis label
    outFile.write("y axis label:;" + yName + "\n")
    
    # Get the list of all algorithms
    allAlgos = []
    for [xValue, data] in resultsList:
        for algoName in data:
            if algoName not in allAlgos: 
                allAlgos += [algoName]
    allAlgos = sorted(allAlgos)
    
    # Write the header
    outFile.write(xName)
    yNegSuff = " length of below confidence half-interval"
    yPosSuff = " length of above confidence half-interval"
    for algoName in allAlgos:
        outFile.write(";" + algoName + ";" + algoName + yNegSuff + ";" + algoName + yPosSuff)
    outFile.write("\n")

    # Write the median an confidence intervals for each x value
    for [xValue, data] in resultsList:
        outFile.write(str(xValue))
        
        for algoName in allAlgos:
            if algoName not in data: 
                outFile.write(";;;")
            else:
                [yLow, yMed, yHigh] = getLowMedHigh(data[algoName])
                outFile.write(";" + str(yMed) + ";" + str(yMed - yLow) + ";" + str(yHigh - yMed))
    
        outFile.write("\n")
    
    print("(Over)wrote " + outFilePath)
    outFile.close()
    