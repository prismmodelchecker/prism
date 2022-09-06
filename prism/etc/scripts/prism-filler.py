#! /usr/bin/env python
# coding=utf-8

# run without arguments for usage info

import re
import sys
import os
from optparse import OptionParser

list_of_files = []
directories = []

def treat_file_names(file) : 
	global list_of_files
	#first get the actual string if a number or None is provided
	if file is None : 
		file = list_of_files[-1]
	if type(file) is int :
		file = list_of_files[file]
	list_of_files.append(file)

	#now get a path
	#first try the directories
	for d in directories :
		joined = os.path.join(d, file) 
		if (os.path.isfile(joined)) :
			return joined
	#otherwise just return the file itself (note: if it does not exist, an exception will be thrown anyway)
	return file

#extract total computation time
def p_time(file = None) :
	file = treat_file_names(file)
	log_content = open(file).read()
	matched = re.search("Time for model checking:[^0-9]*(?P<time>[0-9.]*)", log_content)
	if matched is None :
		return None
	else :
		return matched.group("time");

#extract total number of states
def p_states(file = None) :
	file = treat_file_names(file)
	log_content = open(file).read()
	matched = re.search("States:[^0-9]*(?P<states>[0-9.]*)", log_content)
	if matched is None :
		return None
	else :
		return matched.group("states");

#extract time for LP solving (TODO does not take setup into consideration)
def p_lptime(file = None) :
	file = treat_file_names(file)
	log_content = open(file).read()
	matched = re.search("LP problem solved in [^0-9]*(?P<time>[0-9.]*)", log_content)
	if matched is None :
		return None
	else :
		return matched.group("time");

#extract number of variables in LP
def p_lpsize(file = None) :
	file = treat_file_names(file)
	log_content = open(file).read()
	matched = re.search("Number of LP variables[^0-9]*(?P<vars>[0-9.]*)", log_content)
	if matched is None :
		return None
	else :
		return matched.group("vars");

#extract time for value iteration (in multi obj)
def p_vitime(file = None) :
	file = treat_file_names(file)
	log_content = open(file).read()
	matched = re.search("The value iteration.s. took [^0-9]*(?P<time>[0-9.]*)", log_content)
	if matched is None :
		return None
	else :
		return matched.group("time");

#extract number of objectives (in multi obj)
def p_numobj(file = None) :
	file = treat_file_names(file)
	log_content = open(file).read()
	matched = re.search("Number of objectives: [^0-9]*(?P<num>[0-9.]*)", log_content)
	if matched is None :
		return None
	else :
		return matched.group("num");

#extract number of weights used (in multi obj)
def p_weights(file = None) :
	file = treat_file_names(file)
	log_content = open(file).read()
	matched = re.search("Number of weight vectors used: [^0-9]*(?P<num>[0-9.]*)", log_content)
	if matched is None :
		return None
	else :
		return matched.group("num");

#The program starts here
descrip = u"A script that allows to fill in data from prism logs to textual files\
 such as Latex or CSV.\
 Usefull when writing papers with tables containing results of experiments.\
 Basically, it searches the textual file for occurences of a pound sign (\u00A3), \
 interprets the text between any two pound signs as a python program, executes\
 it and replaces it with the output of the program (removing the pound signs). \
 Some useful functions are provided to extract information from prism logs, see end of this help.\
 Each of the functions takes one optional argument, which is a file name, \
 or a number n which says that the n-th log file searched should be used \
 (repetitions do count, and negative numbers are allowed). \
 For example, 1 stands for the second file used, -2 stands for last but one file. \
 If the argument is omitted, '-1' is used (i.e. last file)."
ep = "Predefined functions: p_time (total computation time), p_states (number of states of model), \
 p_lptime (time taken by LP solver),  p_lpsize (number of variables in linear program),\
 p_vitime (total number for value iterations, in multi-objective queries), p_numobj (number of objectives, \
 in multi-objective queries), p_weights (number of different weights used, in multi objective queries)."

parser = OptionParser(usage="usage: %prog [options] input_file",version="alpha1 (2012-03-04)",description=descrip, epilog=ep)
parser.add_option("-o", "--output", action="store", type="string", dest="outputfile", default=None, help="Output to file (stdout is used if not present")
parser.add_option("-d", "--dir", action="store", type="string", dest="dir", default=None, help="(NOT SUPPORTED YET) Use directory to look for log files. Multiple directories are separated with colon ':', and the first file found is used.")
parser.add_option("-i", "--ignore-errors", action="store_true", dest="ignoreerrors", default=False, help="The execution will not stop on the first error and the output file will be produced despite errors, with \"ERROR\" strings in it.")
(options, args) = parser.parse_args()

if len(args) != 1:
	parser.print_help()
	sys.exit(1)

#open input file
try :
	file_content = open(args[0]).read()
except IOError as e:
	print "Cannot read the input file '" + e.filename + "': " + e.strerror >> sys.stderr
	sys.exit(1)

#open output file
try :
	if (options.outputfile is None) :
		output_file = sys.stdout
	else :
		output_file = open(options.outputfile, "w")
except IOError as e:
	print >> sys.stderr, "Cannot access the output file '" + e.filename + "': " + e.strerror
	sys.exit(1)

#parse directories
if not options.dir is None :
	directories = options.dir.split(':')

#do the work, first, get a string between two pound signs
matched = re.search("£(?P<cmd>[^£]*)£", file_content)
while not matched is None :
	was_successful = True
	#evaluate the user provided expression
	replacement = None
	try :
		replacement =  eval(matched.group("cmd"))
	except NameError as e :
		print >> sys.stderr, "Cannot evaluate '" + matched.group("cmd") + "': " + e.args[0]
		was_successful = False
	except IOError as (noerror, strerror) :
		print >> sys.stderr, "Cannot evaluate '" + matched.group("cmd") + "': " + strerror
		was_successful = False
	except IndexError as e :
		print >> sys.stderr, "Cannot evaluate '" + matched.group("cmd") + "', you are probably trying to open n-th last file when less than n files were opened so far."
		print >> sys.stderr, "The error message reported is: " + e.args[0]
		was_successful = False
	except SyntaxError as e :
		print >> sys.stderr, "Cannot evaluate '" + matched.group("cmd") + "', invalid syntax. Have you enclosed the file name in qutation marks?"
		print >> sys.stderr, "The error message reported is: " + e.args[0]
		was_successful = False

	if replacement is None and was_successful :
		print  >> sys.stderr, "Evaluation of '" + matched.group("cmd") + "' did not return any text. Possible problems are using wrong log file or calling a wrong function."
		was_successful = False;

	#if there was some problem, either substitute with a dummy string or exit, depending on user preferences.
	if (not was_successful) and options.ignoreerrors:
		replacement = "ERROR"
	if (not was_successful) and not options.ignoreerrors:
		sys.exit(1)

	#replace the text
	file_content = file_content.replace(matched.group(),replacement, 1);
	#get next match
	matched = re.search("£(?P<cmd>[^£]*)£", file_content)
#output the string with substitutions
print >> output_file, file_content
