# nas_sim1 readme

This code performs a discrete-tiime event simulation of flights in the national airspace (NAS) for a single day. It does not use threads, but determines the priority of events by using a Java PriorityQueue. There are 50K flights per day and Java threads are not lightweight enough. This code requires other custom code to run, which is not in the repository yet. It also requires input data files, also not in the repository. 
2018/07/30
