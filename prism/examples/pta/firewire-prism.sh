#!/bin/sh

# abst

prism-explicit firewire/abst/{firewire.nm,eventually.pctl} -const delay=360 -aroptions nopre,refine=all,opt

prism-explicit firewire/abst/{firewire.nm,deadline.pctl} -const delay=360,T=5000 -aroptions nopre,refine=all,opt
prism-explicit firewire/abst/{firewire.nm,deadline.pctl} -const delay=360,T=10000 -aroptions nopre,refine=all,opt
prism-explicit firewire/abst/{firewire.nm,deadline.pctl} -const delay=360,T=20000 -aroptions nopre,refine=all,opt

prism-explicit firewire/abst/{firewire.nm,time.pctl} -const delay=360 -aroptions nopre,refine=all,opt

# impl

prism-explicit firewire/impl/{firewire.nm,eventually.pctl} -const delay=360 -aroptions nopre,refine=all,opt

prism-explicit firewire/impl/{firewire.nm,deadline.pctl} -const delay=360,T=2500 -aroptions nopre,refine=all,opt
prism-explicit firewire/impl/{firewire.nm,deadline.pctl} -const delay=360,T=5000 -aroptions nopre,refine=all,opt
prism-explicit firewire/impl/{firewire.nm,deadline.pctl} -const delay=360,T=7500 -aroptions nopre,refine=all,opt


