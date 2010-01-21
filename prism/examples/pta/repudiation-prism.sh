#!/bin/sh

# honest
prism repudiation/honest/{repudiation.nm,eventually.pctl} -aroptions refine=all,opt
prism repudiation/honest/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=40
prism repudiation/honest/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=80
prism repudiation/honest/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=100

# malicious
prism repudiation/malicious/{repudiation.nm,eventually.pctl} -aroptions refine=all,opt
prism repudiation/malicious/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=5
prism repudiation/malicious/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=10
prism repudiation/malicious/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=20

