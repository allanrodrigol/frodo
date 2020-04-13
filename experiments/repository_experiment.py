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

""" This script runs experiments based on an repository of problem instances """

# Add the FRODO benchmarks folder to the Python path and import the frodo2 module
import sys
sys.path.append("../frodo2.17.1.jar/frodo2/benchmarks")
import frodo2

# The command to call java and the JVM parameters
java = "java"
javaParams = [
			"-Xmx2G", # sets the Java heap space to 2 GB
			"-classpath", "../frodo2.17.1.jar", # sets the Java classpath to include FRODO
			]

# Define the path to the folder containing the problem instances
repoPath = "repository"

# Define the algorithms to run
# Each algorithm is a list [algoName, solverClassName, agentConfigFilePath, inputProblemFilePath]
algos = [
		["ADOPT", "frodo2.algorithms.adopt.ADOPTsolver", "../agents/ADOPT/ADOPTagent.xml"], 
 
		["AFB", "frodo2.algorithms.afb.AFBsolver", "../agents/AFB/AFBagent.xml"], 
 
		["DPOP", "frodo2.algorithms.dpop.DPOPsolver", "../agents/DPOP/DPOPagent.xml"], 
		["ASO-DPOP", "frodo2.algorithms.asodpop.ASODPOPsolver", "../agents/DPOP/ASO-DPOP/ASO-DPOPagent.xml"], 
		["MB-DPOP", "frodo2.algorithms.dpop.DPOPsolver", "../agents/DPOP/MB-DPOP/MB-DPOPagent.xml"], 
 		["O-DPOP", "frodo2.algorithms.odpop.ODPOPsolver", "../agents/DPOP/O-DPOP/O-DPOPagent.xml"], 
		["P-DPOP", "frodo2.algorithms.dpop.privacy.P_DPOPsolver", "../agents/DPOP/P-DPOP/P-DPOPagent.xml"], 
		["P3/2-DPOP", "frodo2.algorithms.dpop.privacy.P3halves_DPOPsolver", "../agents/DPOP/P-DPOP/P1.5-DPOPagent.xml"], 
		["P2-DPOP", "frodo2.algorithms.dpop.privacy.P2_DPOPsolver", "../agents/DPOP/P-DPOP/P2-DPOPagent_DisCSP.xml"], 
# 		["P2-DPOP", "frodo2.algorithms.dpop.privacy.P2_DPOPsolver", "../agents/DPOP/P-DPOP/P2-DPOPagent.xml"],
 
		["DSA", "frodo2.algorithms.localSearch.dsa.DSAsolver", "../agents/DSA/DSAagent.xml"], 
 
		["MaxSum", "frodo2.algorithms.maxsum.MaxSumSolver", "../agents/MaxSum/MaxSumAgentPerturbed.xml"], 
 
		["MGM", "frodo2.algorithms.localSearch.mgm.MGMsolver", "../agents/MGM/MGMagent.xml"], 
		["MGM2", "frodo2.algorithms.localSearch.mgm.mgm2.MGM2solver", "../agents/MGM/MGM2agent.xml"], 
 
		["MPC-DisCSP4", "frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver", "../agents/MPC/MPC-DisCSP4.xml"], 
# 		["MPC-DisWCSP4", "frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver", "../agents/MPC/MPC-DisWCSP4.xml"],
 
		["SynchBB", "frodo2.algorithms.synchbb.SynchBBsolver", "../agents/SynchBB/SynchBBagent.xml"], 
		]
timeout = 600 # in seconds

# The number of times each algorithm should be run against each problem instance 
nbrRuns = 1

# The CSV file to which the statistics should be written
output = "outputRepository.csv"

# Run the experiment
frodo2.runFromRepo(java, javaParams, repoPath, nbrRuns, algos, timeout, output)

# Tip: if some of the algorithms tend to time out most of the time on some problem files, 
# you can run 2 experiments: one for all algorithms on the smaller problem sizes, 
# and one with only the faster algorithms on the larger problem sizes

