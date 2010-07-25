# FORMATS'09 Case Studies

prism csma/full/csma.nm csma/full/collisions.pctl -const bmax=2,K=4 -aroptions refine=all,nopre
prism csma/full/csma.nm csma/full/collisions.pctl -const bmax=2,K=8 -aroptions refine=all,nopre
prism csma/full/csma.nm csma/full/collisions.pctl -const bmax=4,K=4 -aroptions refine=all,nopre
#prism csma/full/csma.nm csma/full/collisions.pctl -const bmax=4,K=8 -aroptions refine=all,nopre

prism csma/abst/csma.nm -const bmax=1 csma/abst/eventually.pctl -aroptions refine=all,nopre
prism csma/abst/csma.nm -const bmax=1 csma/abst/deadline.pctl -const T=1000 -aroptions refine=all,nopre
prism csma/abst/csma.nm -const bmax=1 csma/abst/deadline.pctl -const T=2000 -aroptions refine=all,nopre
#prism csma/abst/csma.nm -const bmax=1 csma/abst/deadline.pctl -const T=3000 -aroptions refine=all,nopre

prism firewire/impl/firewire.nm firewire/impl/eventually.pctl -const delay=360 -aroptions refine=all,nopre
prism firewire/impl/firewire.nm firewire/impl/deadline.pctl -const delay=360,T=2500 -aroptions refine=all,nopre
prism firewire/impl/firewire.nm firewire/impl/deadline.pctl -const delay=360,T=5000 -aroptions refine=all,nopre
prism firewire/impl/firewire.nm firewire/impl/deadline.pctl -const delay=360,T=7500 -aroptions refine=all,nopre

prism firewire/abst/firewire.nm firewire/abst/eventually.pctl -const delay=360 -aroptions refine=all,nopre
prism firewire/abst/firewire.nm firewire/abst/deadline.pctl -const delay=360,T=5000 -aroptions refine=all,nopre
prism firewire/abst/firewire.nm firewire/abst/deadline.pctl -const delay=360,T=10000 -aroptions refine=all,nopre
prism firewire/abst/firewire.nm firewire/abst/deadline.pctl -const delay=360,T=20000 -aroptions refine=all,nopre

prism zeroconf/zeroconf.nm zeroconf/incorrect.pctl -aroptions refine=all,nopre
prism zeroconf/zeroconf.nm zeroconf/deadline.pctl -const T=100 -aroptions refine=all,nopre
prism zeroconf/zeroconf.nm zeroconf/deadline.pctl -const T=150 -aroptions refine=all,nopre
prism zeroconf/zeroconf.nm zeroconf/deadline.pctl -const T=200 -aroptions refine=all,nopre

prism repudiation/honest/repudiation.nm repudiation/honest/eventually.pctl -aroptions refine=all,nopre
prism repudiation/honest/repudiation.nm repudiation/honest/deadline.pctl -const T=40 -aroptions refine=all,nopre
prism repudiation/honest/repudiation.nm repudiation/honest/deadline.pctl -const T=80 -aroptions refine=all,nopre
prism repudiation/honest/repudiation.nm repudiation/honest/deadline.pctl -const T=100 -aroptions refine=all,nopre

prism repudiation/malicious/repudiation.nm repudiation/malicious/eventually.pctl -aroptions refine=all,nopre
prism repudiation/malicious/repudiation.nm repudiation/malicious/deadline.pctl -const T=5 -aroptions refine=all,nopre
prism repudiation/malicious/repudiation.nm repudiation/malicious/deadline.pctl -const T=10 -aroptions refine=all,nopre
#prism repudiation/malicious/repudiation.nm repudiation/malicious/deadline.pctl -const T=20 -aroptions refine=all,nopre,eref=1e-8
