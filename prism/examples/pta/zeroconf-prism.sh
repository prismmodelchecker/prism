
# full (multi-variable)

prism-explicit zeroconf/{zeroconf.nm,incorrect.pctl} -aroptions refine=all,nopre,opt

prism-explicit zeroconf/{zeroconf.nm,deadline.pctl} -const T=100 -aroptions refine=all,nopre,opt
prism-explicit zeroconf/{zeroconf.nm,deadline.pctl} -const T=150 -aroptions refine=all,nopre,opt
prism-explicit zeroconf/{zeroconf.nm,deadline.pctl} -const T=200 -aroptions refine=all,nopre,opt

prism-explicit zeroconf/{zeroconf.nm,time.pctl} -aroptions refine=all,nopre,opt


# simple (single variable)

prism-explicit zeroconf/{zeroconf-simple.nm,incorrect.pctl} -aroptions refine=all,nopre,opt

prism-explicit zeroconf/{zeroconf-simple.nm,deadline.pctl} -const T=100 -aroptions refine=all,nopre,opt
prism-explicit zeroconf/{zeroconf-simple.nm,deadline.pctl} -const T=150 -aroptions refine=all,nopre,opt
prism-explicit zeroconf/{zeroconf-simple.nm,deadline.pctl} -const T=200 -aroptions refine=all,nopre,opt

