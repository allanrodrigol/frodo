# FRODO - Open-Source Framework for DCOP

This project is a fork from [FRODO 2](https://sourceforge.net/projects/frodo2) project, a popular Java-based platform for solving distributed constraint optimization problem (DCOP) [1].

## Distributed Constraint Optimization Problem

The distributed constraint optimization problem (DCOP) provides a powerful formalism for modeling many decentralized coordination tasks in multiagent systems, and is widely used in realistic applications, such as sensor networks, task scheduling, resource allocation, among others.

DCOP is a tuple <<img src="https://render.githubusercontent.com/render/math?math=\mathcal{A, X, D, R, \alpha}">>, where <img src="https://render.githubusercontent.com/render/math?math=\mathcal{R}"> is a set of constraints and <img src="https://render.githubusercontent.com/render/math?math=\mathcal{X}"> is a set of variables distributed among a set <img src="https://render.githubusercontent.com/render/math?math=\mathcal{A}"> of  agents. Each variable <img src="https://render.githubusercontent.com/render/math?math=x_i \in \mathcal{X}"> is held by a single agent in <img src="https://render.githubusercontent.com/render/math?math=\mathcal{A}"> and has a finite and discrete domain <img src="https://render.githubusercontent.com/render/math?math=D_i \in \mathcal{D}">. Such mapping from variables to agents determines the control <img src="https://render.githubusercontent.com/render/math?math=\alpha(x_i)"> of each variable <img src="https://render.githubusercontent.com/render/math?math=x_i \in \mathcal{X}"> to an agent. Thus, each value <img src="https://render.githubusercontent.com/render/math?math=d \in D_i"> represents one of possible states of a given agent. The constraints are defined by a set <img src="https://render.githubusercontent.com/render/math?math=\mathcal{R}"> of cost or reward relations between a pair of variable assignments.

DCOP aims to find a complete set of <img src="https://render.githubusercontent.com/render/math?math=A^*"> assignments, where <img src="https://render.githubusercontent.com/render/math?math=A^* = \{d_1, ...,d_n \mbox{ } | \mbox{ } d_1 \in D_1, ..., d_n \in D_n\}">, such that the global objective function <img src="https://render.githubusercontent.com/render/math?math=F(A)"> is minimized according to: 
<img src="https://render.githubusercontent.com/render/math?math=F(A) = \sum\limits_{x_i,x_j \in \mathcal{X}}f_{ij}(d_i,d_j)">
<img src="https://render.githubusercontent.com/render/math?math=A^* = \underset{A \in \mathcal{S}}{\argmin} \mbox{ } F(A)">
when <img src="https://render.githubusercontent.com/render/math?math=x_i \leftarrow d_i"> and <img src="https://render.githubusercontent.com/render/math?math=x_j \leftarrow d_j"> in <img src="https://render.githubusercontent.com/render/math?math=A">.

In recent years, there has been increasing interest in developing new DCOP algorithms. Complete algorithms are able to find optimal solutions, however they require exponentially increasing computational or communication resources with respect to the number of agents and variables, domain size of the variables, or constraints. On the other hand, incomplete algorithms can provide suboptimal solutions faster than complete algorithms.

## Build

This project was converted to an [Apache Maven](https://maven.apache.org/) project and requires JDK 8 or higher. Run `maven clean install` to build the project. The compiled files will be stored in the `target/` directory.

## Major changes

* Converting the original project to a Maven project.
* Implementing the anytime local search ALS_DCOP [3] framework.
* Adding three new local search algorithms: COOPT [2], DSA-SDP [3] and GDBA [4].
* Adding a distributed breath-first search (BSF) generation.

## TODO

* Importing external libraries `library-2.0.1` and `or-objects-3.0.3` from [Maven Central Repository](https://mvnrepository.com/repos/central).
* Merging remote implementation of distributed breath-first search.

## References

1. Léauté, T., Ottens, B., & Szymanek, R. (2009). Frodo 2.0: An open-source framework for distributed constraint optimization. In Proceedings of the IJCAI'09 Distributed Constraint Reasoning Workshop (DCR'09), pp. 160-164, Pasadena, California, USA. AAAI Press.
2. Leite, A., & Enembreck, F. (2019). COOPT: using collective behavior of coupled oscillators for solving DCOP. Artif. Intell.. 64 (1), 987–1023.
3. Zivan, R., Okamoto, S., & Peled, H. (2014). Explorative anytime local search for distributed constraint optimization. Artif. Intell., 212 (1), 1-26.
4. Okamoto, S., Zivan, R., & Nahon, A. (2016). Distributed breakout: Beyond satisfaction. In Proceedings of the Twenty-Fifth International Joint Conference on Artificial Intelligence, IJCAI'16, pp. 447-453. AAAI Press.
