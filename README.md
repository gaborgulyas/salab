# SALabs
Framework for the analysis of structural de-anonymization algorithms.

## Main Commands

### Evaluation of de-anonymization attacks
1. create_data
2. simulate
3. analyze

### Analysis of social network data
4. export
5. measure
6. summarize

Examples below should be executed as:
salabs.bat COMMAND PARAM1 PARAM2 ...
salabs.sh COMMAND PARAM1 PARAM2 ...

Warning: always use '/' separator in PATHs (even on Windows).

## Parameters

If you put a @ before the main command, you can enable debug mode. Most importantly this will open a progressbar window for you on some slower operations (e.g., betweenness-centrality calculation).
The debug mode gives you one more additional benefit: in this case all mapping in each propagation round will be saved to a binary file.

**Note**: the first triple (NETWORK_NAME, SIZE, "EXPERIMENT_IDENTIFIER") identifies each experiment, as it denotes a unique directory on the file system level.
**Note 2**: you can split EXPERIMENT_IDENTIFIER into two parts as "ID1/ID2". The first part will be included in the experiment name as expected, and the second part would be included in the de-anonymization attack name only. After generating a single dataset, you can run different settings against it for comparison, e.g., different seeding parameters.

### 1. create_data

#### Syntax
```
create_data NETWORK_NAME SIZE "EXPERIMENT_IDENTIFIER" NUMBER_OF_EXPORTS PERTURBATION_ALGO NUMBER_OF_PERTURBATIONS PERTURBATION_PARAMETERS
```
Creates a dataset with the given parameters for experimenting. Perturbation provides a realistic way (depends on the algo, though) to create plausible syntatic anonymized datasets.

#### Explanation
- `NETWORK_NAME`: network name, e.g.:  epinions, karate, etc. You can also download new graphs in TGF format (note: include edgelist only!). Include postfix "_directed" for handling directed graphs.
- `SIZE`: this parameter specifies if a subnetwork should be exported from the original graph. -1 denotes if no subgraph should be exported, otherwise the size should be in number of nodes.
- `EXPERIMENT_IDENTIFIER`: custom experiment identified added to output directory name.
- `NUMBER_OF_EXPORTS`: self explanatory parameter name. :)
- `PERTURBATION_ALGO`: the algorithm that could be used for perturbation. Possible choices:
	- `ns09`, `dns09`: Detailed in [1], non-directed and directed versions. Most realistic, thus recommended to use.
    - `sng`: Detailed in [2], it solely exists in the framework for scientific purposes. Not realistic, not recommended to use in experiments.
    - `sample`: samples edges with probability `s`, leaves nodes intact. Used in multiple publications, e.g., [6-8]
    - `copyfirst`: this could be used for making copies if there is already one perturbation existing for an experiment.
    - `clone`: clones the source graph.
- `NUMBER_OF_PERTURBATIONS`: number of perturbations.
- `PERTURBATION_PARAMETERS`: depends on the chosen algorithm. ns09, dns09 takes two arguments for setting alpha_v, alpha_e. For sng, it sets alpha_v (node that overlap), number of nodes beside the overlap, and the proportion of extra edges added to the target network (3 params overall).

#### Example
```
create_data epinions -1 "first_experiment" 1 ns09 2 0.5 0.75
```
Here, the program uses the original Epinions network for perturbation (-1 means there should be no exports). Two perturbations are created with the perturbation algorithm Ns09, having parameters alpha_v=0.5, and aplha_e=0.75.

### 2. simulate

#### Syntax
```
simulate NETWORK_NAME SIZE "EXPERIMENT_IDENTIFIER" DEANON_ALGO NUMBER_OF_ROUNDS SEED_TYPE SEED_SIZE DEANON_PARAMETERS
```
Runs a re-identification attack on an existing dataset.

#### Explanation
- `NETWORK_NAME`, `SIZE`, `EXPERIMENT_IDENTIFIER`: same as before.
- `DEANON_ALGO`:
    - `ns09`: De-anonymization algorithm as described in [1], but implemented in an undirected fashion.
    - `dns09`: De-anonymization algorithm as described in [1].
    - `grh`: Grasshopper de-anonymizaton algorithm as described in [3].
    - `sng`: Seed-and-grow algorithm as described in [2].
- `NUMBER_OF_ROUNDS`: number of re-identification rounds could be set. Helpful, if results vary for the given perturbation setting.
- `SEED_TYPE`: artifical seeding based on different characteristics of nodes. To understand differences better, you can read [4].
    - `Kcliques.R`: selects k-clique member nodes for seeding. K sets the clique size, and R controls the degree of possible nodes, e.g., R=1 means that seed nodes should be in the top 10% by degree. R is optional.
    - `Kbfs.R`: selects neighboring nodes by using breadth-first search. K and R takes a similar roles as before.
    - `top`: highest degree nodes are used as seeds.
    - `lta.R`: use nodes with low anonymity score for seeding. LTA refers to local topological anonymity. R can also be used to select high degree nodes only. See [5] for more details on LTA.
    - `betwc.R`: use nodes with high betweenness-centrality values for seeding. R can also be used to select high degree nodes only.
    - `closec.R`: use nodes with high closeness-centrality values for seeding. R can also be used to select high degree nodes only.
    - `lcc.R`: use nodes that have high local clustering coefficient values for seeding. R can also be used to select high degree nodes only.
    - `lcch.R`: same as before. The only difference is that this restricts LCC to top 20% before filtering with degree.
    - `random.R`: randomly selected nodes, that could be filtered by degree (have to set R).
- `SEED_SIZE`: number of seed nodes.
- `DEANON_PARAMETERS`: depends on the algo, but for ns09 and grh you can the theta parameter here. Algo sng does not take such extra parameters.

#### Example
    simulate epinions -1 "first_experiment" ns09 3 random.25 1000 0.01
Now we run the ns09 algorithm on the dataset created in the previous example. 1000 seed nodes are selected from the 25% of the highest degree nodes, and parameter theta is set to 0.01.

Theta controls the greedyness of the algorithms. You can get a better understanding on the parameter theta for ns09 after checking out Fig. 2. in [4].

### 3. analyze

#### Syntax
    analyze NETWORK_NAME SIZE "EXPERIMENT_IDENTIFIER" DEANON_ALGO no_lta
Collects and summarizes the results of re-identification. EXPERIMENT_IDENTIFIER can be split, so even sub-experiments can be evaluated separately.
This procedure will do the following:
- Measure accuracy, i.e., calculate the average TP, FP rates
- Measure runtime averages
- Calculate LTA variants for the network (if it is not eplicitly told not to, and related caches do not exist)

#### Explanation
- `NETWORK_NAME`, `SIZE`, `EXPERIMENT_IDENTIFIER`, `DEANON_ALGO`: same as before.
- no_lta: as calculation of local topological anonymity values (LTA) tend to be a slower operation the framework can be commanded to omit this step. Otherwise, it would calculate LTA values (and cache it), and calculate the Spearman rank correlation between LTA values and observed empirical re-identification frequencies.

#### Example
    analyze epinions -1 "first_experiment" ns09 no_lta

### 4. export

    export SRC_NET TAR_NET EXP_SIZE
Exports a subnet ans saves the result.

### 5. measure

#### Syntax
    measure SRC_NET MEASURE TOP_PERCENT
Measures different properties of a network and saves the values to a binary dictionary cache.

#### Explanation
- `MEASURE`: could be closeness-centrality (closec), betweenness centrality (betwc), local topological anonymity (LTA), node degree (deg), and local clustering coefficient (lcc).

#### Example
    measure epinions closec

### 6. summarize

#### Syntax
    summarize NET
Gives a brief overview on the given network including sizes and degree distribution.

#### Example
	summarize epinions				// this network should be in the data directory
	summarize ./output/karate.tgf	// arbitrary network path

## Directory structure

The directory structure of experiments are tend to be quite self-explanatory, but might deserve a little attention.

**TODO: TBW.**

## References
[1] Arvind Narayanan, Vitaly Shmatikov: De-anonymizing Social Networks. IEEE Security and Privacy 2009. [[pdf]](http://33bits.org/2009/03/19/de-anonymizing-social-networks/)

[2] Wei Peng, Feng Li, Xukai Zou, Jie Wu: Seed and Grow: An attack against anonymized social networks. SECON 2012.

[3] Benedek Simon, Gabor Gyorgy Gulyas, Sandor Imre: Analysis of Grasshopper, a Novel Social Network De-anonymization Algorithm. Journal of Periodica Polytechnica Electrical Engineering and Computer Science, Volume 58, Number 4, 2014. [[pdf]](www.pp.bme.hu/eecs/article/download/7878/6648)

[4] Gabor Gyorgy Gulyas, Sandor Imre: Measuring Importance of Seeding for Structural De-anonymization Attacks in Social Networks. SESOC 2014. [[pdf]](http://gulyas.info/upload/GulyasG_SESOC14.pdf)

[5] Gabor Gyorgy Gulyas, Sandor Imre: Using Identity Separation Against De-anonymization of Social Networks. ransactions on Data Privacy 8:2 (2015), pp. 113 - 140 [[pdf]](http://www.tdp.cat/issues11/tdp.a180a14.pdf)

[6] Shouling Ji, Weiqing Li, Prateek Mittal, Xin Hu, Raheem Beyah: SecGraph: A Uniform and Open-source Evaluation System for Graph Data Anonymization and De-anonymization. Usenix Security 2015.

[7] Yartseva, Lyudmila, Matthias Grossglauser: On the performance of percolation graph matching. Proceedings of the first ACM conference on Online social networks. ACM, 2013.

[8] Pedram Pedarsani, Daniel R. Figueiredo, Matthias Grossglauser: A Bayesian method for matching two similar graphs without seeds. 51st Annual Allerton Conference on Communication, Control, and Computing, Monticello, IL, 2013.


