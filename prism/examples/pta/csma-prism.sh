
# abstract (time bounded)

prism-explicit csma/abst/{csma.nm,eventually.pctl} -aroptions refine=all,nopre,opt -const bmax=1

prism-explicit csma/abst/{csma.nm,deadline.pctl} -const bmax=1 -const T=1000 -aroptions refine=all,nopre,opt
prism-explicit csma/abst/{csma.nm,deadline.pctl} -const bmax=1 -const T=2000 -aroptions refine=all,nopre,opt
prism-explicit csma/abst/{csma.nm,deadline.pctl} -const bmax=1 -const T=3000 -aroptions refine=all,nopre,opt


# full (collisions)

prism-explicit csma/{csma.nm,collisions.pctl} -const bmax=2,K=4 -aroptions refine=all,nopre,opt
prism-explicit csma/{csma.nm,collisions.pctl} -const bmax=2,K=8 -aroptions refine=all,nopre,opt
prism-explicit csma/{csma.nm,collisions.pctl} -const bmax=4,K=4 -aroptions refine=all,nopre,opt
prism-explicit csma/{csma.nm,collisions.pctl} -const bmax=4,K=8 -aroptions refine=all,nopre,opt

prism-explicit csma/{csma.nm,time.pctl} -const bmax=1,K=0 -aroptions refine=all,nopre,opt
prism-explicit csma/{csma.nm,time.pctl} -const bmax=2,K=0 -aroptions refine=all,nopre,opt
prism-explicit csma/{csma.nm,time.pctl} -const bmax=3,K=0 -aroptions refine=all,nopre,opt
prism-explicit csma/{csma.nm,time.pctl} -const bmax=4,K=0 -aroptions refine=all,nopre,opt


