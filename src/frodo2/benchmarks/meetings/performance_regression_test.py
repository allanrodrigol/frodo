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

# For each algorithm, this script compares the performance of its current version (in the src/ folder) against its old version (from a JAR)

# Add the FRODO benchmarks folder to the Python path and import the frodo2 module
import sys
sys.path.append("..")
import frodo2

# The command to call java and the JVM parameters
root = "../../../../"
java = "java"
javaParamsOld = [
            "-Xmx2G", 
            "-classpath", root + "frodo2.17.1.jar", # includes a (presumably older) version of FRODO from a JAR
            ]
javaParamsNew = [
            "-Xmx2G", 
            "-classpath", root + "bin:" + root + "lib/jacop-4.4.0.jar:" + root + "lib/jdom-2.0.6.jar", # includes the current version of FRODO
            ]

# Partly define the problem generator (the input parameters will depend on the algorithm)
generator = "frodo2.benchmarks.meetings.MeetingScheduling"
problemFileEAV = "meetingScheduling_EAV.xml"
problemFilePEAV = "meetingScheduling_PEAV.xml"

# Define the experiments to run, and on which problem sizes
# Each experiment is [algoName, genParams, [algo_version_1, algo_version_2...]]
# where algo_version_i is [algoName, solverClassName, agentConfigFilePath, inputProblemFilePath, javaParams]
experiments = [
    ["ADOPT", ["-i", "-PEAV", "-maxCost", 10, 3, list(range(1, 5)), 2, 8], [
        ["previous ADOPT", "frodo2.algorithms.adopt.ADOPTsolver", root + "agents/ADOPT/ADOPTagentJaCoP.xml", problemFilePEAV, javaParamsOld], 
        ["new ADOPT", "frodo2.algorithms.adopt.ADOPTsolver", root + "agents/ADOPT/ADOPTagentJaCoP.xml", problemFilePEAV, javaParamsNew], 
            ]], 
    ["AFB", ["-i", "-PEAV", "-maxCost", 10, 3, list(range(1, 7)), 2, 8], [
        ["previous AFB", "frodo2.algorithms.afb.AFBsolver", root + "agents/AFB/AFBagentJaCoP.xml", problemFilePEAV, javaParamsOld], 
        ["new AFB", "frodo2.algorithms.afb.AFBsolver", root + "agents/AFB/AFBagentJaCoP.xml", problemFilePEAV, javaParamsNew], 
            ]],
    ["DPOP", ["-i", "-PEAV", "-maxCost", 10, 3, list(range(1, 7)), 2, 8], [
        ["previous DPOP", "frodo2.algorithms.dpop.DPOPsolver", root + "agents/DPOP/DPOPagentJaCoP.xml", problemFilePEAV, javaParamsOld], 
        ["new DPOP", "frodo2.algorithms.dpop.DPOPsolver", root + "agents/DPOP/DPOPagentJaCoP.xml", problemFilePEAV, javaParamsNew], 
            ]], 
    ["ASO-DPOP", ["-i", "-PEAV", "-maxCost", 10, 3, list(range(1, 7)), 2, 8], [
        ["previous ASO-DPOP", "frodo2.algorithms.asodpop.ASODPOPsolver", root + "agents/DPOP/ASO-DPOP/ASO-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsOld], 
        ["new ASO-DPOP", "frodo2.algorithms.asodpop.ASODPOPsolver", root + "agents/DPOP/ASO-DPOP/ASO-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsNew], 
            ]], 
    ["MB-DPOP", ["-i", "-PEAV", "-maxCost", 10, 3, list(range(1, 7)), 2, 8], [
        ["previous MB-DPOP", "frodo2.algorithms.dpop.DPOPsolver", root + "agents/DPOP/MB-DPOP/MB-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsOld], 
        ["new MB-DPOP", "frodo2.algorithms.dpop.DPOPsolver", root + "agents/DPOP/MB-DPOP/MB-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsNew], 
            ]], 
    ["O-DPOP", ["-i", "-PEAV", "-maxCost", 10, 3, list(range(1, 7)), 2, 8], [
        ["previous O-DPOP", "frodo2.algorithms.odpop.ODPOPsolver", root + "agents/DPOP/O-DPOP/O-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsOld], 
        ["new O-DPOP", "frodo2.algorithms.odpop.ODPOPsolver", root + "agents/DPOP/O-DPOP/O-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsNew], 
            ]], 
    ["P-DPOP", ["-i", "-PEAV", "-maxCost", 10, 3, list(range(1, 7)), 2, 8], [
        ["previous P-DPOP", "frodo2.algorithms.dpop.privacy.P_DPOPsolver", root + "agents/DPOP/P-DPOP/P-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsOld], 
        ["new P-DPOP", "frodo2.algorithms.dpop.privacy.P_DPOPsolver", root + "agents/DPOP/P-DPOP/P-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsNew], 
            ]],
    ["P1.5-DPOP", ["-i", "-PEAV", "-maxCost", 10, 3, list(range(1, 4)), 2, 8], [
        ["previous P3/2-DPOP", "frodo2.algorithms.dpop.privacy.P3halves_DPOPsolver", root + "agents/DPOP/P-DPOP/P1.5-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsOld], 
        ["new P3/2-DPOP", "frodo2.algorithms.dpop.privacy.P3halves_DPOPsolver", root + "agents/DPOP/P-DPOP/P1.5-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsNew], 
            ]],
    ["P2-DPOP", ["-i", "-PEAV", "-maxCost", 10, 3, list(range(1, 3)), 2, 4], [
        ["previous P2-DPOP", "frodo2.algorithms.dpop.privacy.P2_DPOPsolver", root + "agents/DPOP/P-DPOP/P2-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsOld],
        ["new P2-DPOP", "frodo2.algorithms.dpop.privacy.P2_DPOPsolver", root + "agents/DPOP/P-DPOP/P2-DPOPagentJaCoP.xml", problemFilePEAV, javaParamsNew],
            ]],
    ["DSA", ["-PEAV", "-infinity", 1000, "-maxCost", 10, 3, list(range(1, 7)), 2, 8], [
        ["previous DSA", "frodo2.algorithms.localSearch.dsa.DSAsolver", root + "agents/DSA/DSAagent.xml", problemFilePEAV, javaParamsOld], 
        ["new DSA", "frodo2.algorithms.localSearch.dsa.DSAsolver", root + "agents/DSA/DSAagent.xml", problemFilePEAV, javaParamsNew], 
            ]],
    ["MaxSum", ["-PEAV", "-infinity", 1000, "-maxCost", 10, 3, list(range(1, 7)), 2, 8], [
        ["previous MaxSum", "frodo2.algorithms.maxsum.MaxSumSolver", root + "agents/MaxSum/MaxSumAgentPerturbed.xml", problemFilePEAV, javaParamsOld], 
        ["new MaxSum", "frodo2.algorithms.maxsum.MaxSumSolver", root + "agents/MaxSum/MaxSumAgentPerturbed.xml", problemFilePEAV, javaParamsNew], 
            ]],
    ["MGM", ["-PEAV", "-infinity", 1000, "-maxCost", 10, 3, list(range(1, 7)), 2, 8], [
        ["previous MGM", "frodo2.algorithms.localSearch.mgm.MGMsolver", root + "agents/MGM/MGMagent.xml", problemFilePEAV, javaParamsOld], 
        ["new MGM", "frodo2.algorithms.localSearch.mgm.MGMsolver", root + "agents/MGM/MGMagent.xml", problemFilePEAV, javaParamsNew], 
            ]],
    ["MGM2", ["-PEAV", "-infinity", 1000, "-maxCost", 10, 3, list(range(1, 5)), 2, 8], [
        ["previous MGM2", "frodo2.algorithms.localSearch.mgm.mgm2.MGM2solver", root + "agents/MGM/MGM2agent.xml", problemFilePEAV, javaParamsOld], 
        ["new MGM2", "frodo2.algorithms.localSearch.mgm.mgm2.MGM2solver", root + "agents/MGM/MGM2agent.xml", problemFilePEAV, javaParamsNew], 
            ]],
    ["MPC-DisCSP4", ["-i", "-EAV", 3, list(range(1, 5)), 2, 8], [
        ["previous MPC-DisCSP4", "frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver", root + "agents/MPC/MPC-DisCSP4_JaCoP.xml", problemFileEAV, javaParamsOld],
        ["new MPC-DisCSP4", "frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver", root + "agents/MPC/MPC-DisCSP4_JaCoP.xml", problemFileEAV, javaParamsNew],
            ]],
    ["MPC-DisWCSP4", ["-i", "-EAV", "-maxCost", 10, 3, list(range(1, 3)), 2, 8], [
        ["previous MPC-DisWCSP4", "frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver", root + "agents/MPC/MPC-DisWCSP4_JaCoP.xml", problemFileEAV, javaParamsOld],
        ["new MPC-DisWCSP4", "frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver", root + "agents/MPC/MPC-DisWCSP4_JaCoP.xml", problemFileEAV, javaParamsNew],
            ]],    
    ["SynchBB", ["-i", "-PEAV", "-maxCost", 10, 3, list(range(1, 7)), 2, 8], [
        ["previous SynchBB", "frodo2.algorithms.synchbb.SynchBBsolver", root + "agents/SynchBB/SynchBBagentJaCoP.xml", problemFilePEAV, javaParamsOld], 
        ["new SynchBB", "frodo2.algorithms.synchbb.SynchBBsolver", root + "agents/SynchBB/SynchBBagentJaCoP.xml", problemFilePEAV, javaParamsNew], 
            ]],
]
timeout = 30 # in seconds
nbrProblems = 101

# Run each experiment nbrProblems times
outputs = [] # the names of the CSV output files
for i in range(0, nbrProblems):
    print("\n" + str(i + 1) + "/" + str(nbrProblems))
    
    # Run each experiment
    for exp in experiments: 
        print("\n" + exp[0]) # prints the algorithm
        
        # The CSV file to which the statistics should be written
        output = exp[0] + ".csv"
        outputs += [output]
        
        # Run the experiment
        frodo2.run(java, javaParamsNew, generator, exp[1], 1, exp[2], timeout, output, saveProblems = False)

# Plot the results for each experiment
for j in range(0, len(experiments)): 
    exp = experiments[j]

    # The CSV file to which the statistics should have been written
    output = outputs[j]

    # Plot curves with x = problem size and y = performance of each algorithm
#     frodo2.plot(output, xCol = 10, yCol = 16, block = False) # 16 = message size
#     frodo2.plot(output, xCol = 10, yCol = 14, block = (j == len(experiments)-1)) # 14 = runtime

    # Scatter plot with one data point per instance, x = old algorithm and y = new algorithm
    xAlgo = exp[2][0][0]
    yAlgo = exp[2][1][0]
    frodo2.plotScatter(output, xAlgo, yAlgo, metricsCol = 20, timeouts = False, block = False, loglog = False) # 20 = solution quality 
    frodo2.plotScatter(output, xAlgo, yAlgo, metricsCol = 16, timeouts = True, block = False) # 16 = message size
    frodo2.plotScatter(output, xAlgo, yAlgo, metricsCol = 16, timeouts = False, block = False) # 16 = message size
    frodo2.plotScatter(output, xAlgo, yAlgo, metricsCol = 14, timeouts = True, block = False) # 14 = runtime
    frodo2.plotScatter(output, xAlgo, yAlgo, metricsCol = 14, timeouts = False, block = (j == len(experiments)-1)) # 14 = runtime
