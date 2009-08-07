
# honest
prism-explicit repudiation/honest/{repudiation.nm,eventually.pctl} -aroptions refine=all,opt
prism-explicit repudiation/honest/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=40
prism-explicit repudiation/honest/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=80
prism-explicit repudiation/honest/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=100

# malicious
prism-explicit repudiation/malicious/{repudiation.nm,eventually.pctl} -aroptions refine=all,opt
prism-explicit repudiation/malicious/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=5
prism-explicit repudiation/malicious/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=10
prism-explicit repudiation/malicious/{repudiation.nm,deadline.pctl} -aroptions refine=all,opt -const T=20

