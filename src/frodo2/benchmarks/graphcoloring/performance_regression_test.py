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
generator = "frodo2.benchmarks.graphcoloring.GraphColoring"
problemFile = "graphColoring.xml"

# Define the experiments to run, and on which problem sizes
# Each experiment is [algoName, genParams, [algo_version_1, algo_version_2...]]
# where algo_version_i is [algoName, solverClassName, agentConfigFilePath, inputProblemFilePath, javaParams]
experiments = [
	["ADOPT", ["-i", "-soft", list(range(10, 15)), .4, .0, 3], [
		["previous ADOPT", "frodo2.algorithms.adopt.ADOPTsolver", root + "agents/ADOPT/ADOPTagentJaCoP.xml", problemFile, javaParamsOld], 
		["new ADOPT", "frodo2.algorithms.adopt.ADOPTsolver", root + "agents/ADOPT/ADOPTagentJaCoP.xml", problemFile, javaParamsNew], 
			]], 
	["AFB", ["-i", "-soft", list(range(15, 21)), .4, .0, 3], [
		["previous AFB", "frodo2.algorithms.afb.AFBsolver", root + "agents/AFB/AFBagentJaCoP.xml", problemFile, javaParamsOld], 
		["new AFB", "frodo2.algorithms.afb.AFBsolver", root + "agents/AFB/AFBagentJaCoP.xml", problemFile, javaParamsNew], 
			]],
	["DPOP", ["-i", "-soft", list(range(14, 19)), .4, .0, 3], [
		["previous DPOP", "frodo2.algorithms.dpop.DPOPsolver", root + "agents/DPOP/DPOPagentJaCoP.xml", problemFile, javaParamsOld], 
		["new DPOP", "frodo2.algorithms.dpop.DPOPsolver", root + "agents/DPOP/DPOPagentJaCoP.xml", problemFile, javaParamsNew], 
			]], 
	["ASO-DPOP", ["-i", "-soft", list(range(14, 19)), .4, .0, 3], [
		["previous ASO-DPOP", "frodo2.algorithms.asodpop.ASODPOPsolver", root + "agents/DPOP/ASO-DPOP/ASO-DPOPagentJaCoP.xml", problemFile, javaParamsOld], 
		["new ASO-DPOP", "frodo2.algorithms.asodpop.ASODPOPsolver", root + "agents/DPOP/ASO-DPOP/ASO-DPOPagentJaCoP.xml", problemFile, javaParamsNew], 
			]], 
	["MB-DPOP", ["-i", "-soft", list(range(15, 21)), .4, .0, 3], [
		["previous MB-DPOP", "frodo2.algorithms.dpop.DPOPsolver", root + "agents/DPOP/MB-DPOP/MB-DPOPagentJaCoP.xml", problemFile, javaParamsOld], 
		["new MB-DPOP", "frodo2.algorithms.dpop.DPOPsolver", root + "agents/DPOP/MB-DPOP/MB-DPOPagentJaCoP.xml", problemFile, javaParamsNew], 
			]], 
	["O-DPOP", ["-i", "-soft", list(range(14, 19)), .4, .0, 3], [
		["previous O-DPOP", "frodo2.algorithms.odpop.ODPOPsolver", root + "agents/DPOP/O-DPOP/O-DPOPagentJaCoP.xml", problemFile, javaParamsOld], 
		["new O-DPOP", "frodo2.algorithms.odpop.ODPOPsolver", root + "agents/DPOP/O-DPOP/O-DPOPagentJaCoP.xml", problemFile, javaParamsNew], 
			]], 
	["P-DPOP", ["-i", "-soft", list(range(9, 14)), .4, .0, 3], [
		["previous P-DPOP", "frodo2.algorithms.dpop.privacy.P_DPOPsolver", root + "agents/DPOP/P-DPOP/P-DPOPagentJaCoP.xml", problemFile, javaParamsOld], 
		["new P-DPOP", "frodo2.algorithms.dpop.privacy.P_DPOPsolver", root + "agents/DPOP/P-DPOP/P-DPOPagentJaCoP.xml", problemFile, javaParamsNew], 
			]],
	["P1.5-DPOP", ["-i", "-soft", list(range(5, 10)), .4, .0, 3], [
		["previous P3/2-DPOP", "frodo2.algorithms.dpop.privacy.P3halves_DPOPsolver", root + "agents/DPOP/P-DPOP/P1.5-DPOPagentJaCoP.xml", problemFile, javaParamsOld], 
		["new P3/2-DPOP", "frodo2.algorithms.dpop.privacy.P3halves_DPOPsolver", root + "agents/DPOP/P-DPOP/P1.5-DPOPagentJaCoP.xml", problemFile, javaParamsNew], 
			]],
	["P2-DPOP", ["-i", "-soft", list(range(3, 6)), .4, .0, 3], [
		["previous P2-DPOP", "frodo2.algorithms.dpop.privacy.P2_DPOPsolver", root + "agents/DPOP/P-DPOP/P2-DPOPagentJaCoP.xml", problemFile, javaParamsOld],
		["new P2-DPOP", "frodo2.algorithms.dpop.privacy.P2_DPOPsolver", root + "agents/DPOP/P-DPOP/P2-DPOPagentJaCoP.xml", problemFile, javaParamsNew],
			]],
	["Complete-E-DPOP", ["-soft", list(range(22, 27)), .4, .0, 3, .3], [
		["previous Complete-E-DPOP", "frodo2.algorithms.dpop.stochastic.Complete_E_DPOPsolver", root + "agents/DPOP/StochDCOP/Complete-E-DPOP.xml", problemFile, javaParamsOld],
		["new Complete-E-DPOP", "frodo2.algorithms.dpop.stochastic.Complete_E_DPOPsolver", root + "agents/DPOP/StochDCOP/Complete-E-DPOP.xml", problemFile, javaParamsNew],
			]],
	["E-DPOP", ["-soft", list(range(26, 31)), .4, .0, 3, .3], [
		["previous E-DPOP", "frodo2.algorithms.dpop.stochastic.E_DPOPsolver", root + "agents/DPOP/StochDCOP/E-DPOP.xml", problemFile, javaParamsOld],
		["new E-DPOP", "frodo2.algorithms.dpop.stochastic.E_DPOPsolver", root + "agents/DPOP/StochDCOP/E-DPOP.xml", problemFile, javaParamsNew],
			]],
	["Robust-E-DPOP", ["-soft", list(range(27, 32)), .4, .0, 3, .3], [
		["previous Robust-E-DPOP", "frodo2.algorithms.dpop.stochastic.E_DPOPsolver", root + "agents/DPOP/StochDCOP/Robust-E-DPOP.xml", problemFile, javaParamsOld],
		["new Robust-E-DPOP", "frodo2.algorithms.dpop.stochastic.E_DPOPsolver", root + "agents/DPOP/StochDCOP/Robust-E-DPOP.xml", problemFile, javaParamsNew],
			]],
	["DSA", ["-i", "-soft", list(range(15, 20)), .4, .0, 3], [
		["previous DSA", "frodo2.algorithms.localSearch.dsa.DSAsolver", root + "agents/DSA/DSAagentJaCoP.xml", problemFile, javaParamsOld], 
		["new DSA", "frodo2.algorithms.localSearch.dsa.DSAsolver", root + "agents/DSA/DSAagentJaCoP.xml", problemFile, javaParamsNew], 
			]],
	["MaxSum", ["-soft", list(range(15, 20)), .4, .0, 3], [
		["previous MaxSum", "frodo2.algorithms.maxsum.MaxSumSolver", root + "agents/MaxSum/MaxSumAgentPerturbed.xml", problemFile, javaParamsOld], 
		["new MaxSum", "frodo2.algorithms.maxsum.MaxSumSolver", root + "agents/MaxSum/MaxSumAgentPerturbed.xml", problemFile, javaParamsNew], 
			]],
	["MGM", ["-i", "-soft", list(range(15, 20)), .4, .0, 3], [
		["previous MGM", "frodo2.algorithms.localSearch.mgm.MGMsolver", root + "agents/MGM/MGMagentJaCoP.xml", problemFile, javaParamsOld], 
		["new MGM", "frodo2.algorithms.localSearch.mgm.MGMsolver", root + "agents/MGM/MGMagentJaCoP.xml", problemFile, javaParamsNew], 
			]],
	["MGM2", ["-i", "-soft", list(range(15, 20)), .4, .0, 3], [
		["previous MGM2", "frodo2.algorithms.localSearch.mgm.mgm2.MGM2solver", root + "agents/MGM/MGM2agentJaCoP.xml", problemFile, javaParamsOld], 
		["new MGM2", "frodo2.algorithms.localSearch.mgm.mgm2.MGM2solver", root + "agents/MGM/MGM2agentJaCoP.xml", problemFile, javaParamsNew], 
			]],
	["MPC-DisCSP4", ["-i", list(range(2, 7)), .4, .0, 3], [
		["previous MPC-DisCSP4", "frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver", root + "agents/MPC/MPC-DisCSP4_JaCoP.xml", problemFile, javaParamsOld],
		["new MPC-DisCSP4", "frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver", root + "agents/MPC/MPC-DisCSP4_JaCoP.xml", problemFile, javaParamsNew],
			]],
	["MPC-DisWCSP4", ["-i", "-soft", list(range(2, 5)), .4, .0, 3], [
		["previous MPC-DisWCSP4", "frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver", root + "agents/MPC/MPC-DisWCSP4_JaCoP.xml", problemFile, javaParamsOld],
		["new MPC-DisWCSP4", "frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver", root + "agents/MPC/MPC-DisWCSP4_JaCoP.xml", problemFile, javaParamsNew],
			]],	
	["SynchBB", ["-i", "-soft", list(range(16, 21)), .4, .0, 3], [
		["previous SynchBB", "frodo2.algorithms.synchbb.SynchBBsolver", root + "agents/SynchBB/SynchBBagentJaCoP.xml", problemFile, javaParamsOld], 
		["new SynchBB", "frodo2.algorithms.synchbb.SynchBBsolver", root + "agents/SynchBB/SynchBBagentJaCoP.xml", problemFile, javaParamsNew], 
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
#     frodo2.plot(output, xCol = 7, yCol = 16, block = False) # 16 = message size
#     frodo2.plot(output, xCol = 7, yCol = 14, block = (j == len(experiments)-1)) # 14 = runtime

    # Scatter plot with one data point per instance, x = old algorithm and y = new algorithm
	xAlgo = exp[2][0][0]
	yAlgo = exp[2][1][0]
	frodo2.plotScatter(output, xAlgo, yAlgo, metricsCol = 20, timeouts = False, block = False, loglog = False) # 20 = solution quality 
	frodo2.plotScatter(output, xAlgo, yAlgo, metricsCol = 16, timeouts = True, block = False) # 16 = message size
	frodo2.plotScatter(output, xAlgo, yAlgo, metricsCol = 16, timeouts = False, block = False) # 16 = message size
	frodo2.plotScatter(output, xAlgo, yAlgo, metricsCol = 14, timeouts = True, block = False) # 14 = runtime
	frodo2.plotScatter(output, xAlgo, yAlgo, metricsCol = 14, timeouts = False, block = (j == len(experiments)-1)) # 14 = runtime
