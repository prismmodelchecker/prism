//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: CUDD functions
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

#include "util.h"
#include "cuddInt.h"
#include "dd_cudd.h"

#include <map>
#include <set>
#include <string>

extern FILE *dd_out;

// A flag indicating that a CUDD error has occurred
// that could not be signalled by returning a NULL DdNode*
// from a function
bool dd_cudd_error_flag = false;

static int Cudd_CheckZeroRefVerbose(DdManager *ddman);

//-----------------------------------------------------------------------------------

DdManager *DD_InitialiseCUDD()
{
	// choose some ensible defaults
	// (cudd max memory = 200 MB)
	// (cudd epsilon = 1.0e-15, very close to min precision of doubles, 1.1e-16)
	
	return DD_InitialiseCUDD(200*1024, 1.0e-15);
}

//-----------------------------------------------------------------------------------

DdManager *DD_InitialiseCUDD(long max_mem, double epsilon)
{
	DdManager *ddman;
	
	// initialise CUDD package
	ddman = Cudd_Init(0, 0, CUDD_UNIQUE_SLOTS, CUDD_CACHE_SLOTS, max_mem * 1024);
	Cudd_SetStderr(ddman, stdout);
	Cudd_SetMaxMemory(ddman, max_mem * 1024);
	Cudd_SetEpsilon(ddman, epsilon);

	return ddman;
}

//-----------------------------------------------------------------------------------

void DD_SetCUDDMaxMem(DdManager *ddman, long max_mem)
{
	Cudd_SetMaxMemory(ddman, max_mem * 1024);
}

//-----------------------------------------------------------------------------------

void DD_SetCUDDEpsilon(DdManager *ddman, double epsilon)
{
	Cudd_SetEpsilon(ddman, epsilon);
}

//-----------------------------------------------------------------------------------

void DD_PrintCacheInfo(DdManager *ddman)
{
	static double old_lookups, old_hits;
	double slots = Cudd_ReadCacheSlots(ddman);
	double used_slots = Cudd_ReadCacheUsedSlots(ddman);
	double lookups = Cudd_ReadCacheLookUps(ddman);
	double hits = Cudd_ReadCacheHits(ddman);
	double percent;
	
	fprintf(dd_out, "Cache info: %.2f%% of %.0f slots used, ", 100.0*used_slots, slots);
	percent = (lookups <= old_lookups) ? 0 : 100.0*(hits-old_hits)/(lookups-old_lookups);
	fprintf(dd_out, "lookup success: %.0f/%.0f = %.2f%% ", hits-old_hits, lookups-old_lookups, percent);
	percent = (lookups <= 0) ? 0 : 100.0*hits/lookups;
	fprintf(dd_out, "(total %.0f/%.0f = %.2f%%)\n", hits, lookups, 100.0*hits/lookups);
	old_lookups = lookups;
	old_hits = hits;
}

//-----------------------------------------------------------------------------------

void DD_CloseDownCUDD(DdManager *ddman) { DD_CloseDownCUDD(ddman, true); }
void DD_CloseDownCUDD(DdManager *ddman, bool check)
{
//	fprintf(dd_out, "Memory in use: %.1fKB\n", Cudd_ReadMemoryInUse(ddman)/1024.0);
//	fprintf(dd_out, "Peak number of nodes: %ld\n", Cudd_ReadPeakNodeCount(ddman));
//	fprintf(dd_out, "Peak number of live nodes: %d\n", Cudd_ReadPeakLiveNodeCount(ddman));
//	fprintf(dd_out, "Number of cache entries: %u\n", ddman->cacheSlots);
//	fprintf(dd_out, "Hard limit for cache size: %u\n", Cudd_ReadMaxCacheHard(ddman));
//	fprintf(dd_out, "Soft limit for cache size: %u\n", Cudd_ReadMaxCache(ddman));


	if (check) {
		// if required, check everthing is closed down OK and warn if not
		// for now, we disable the debug check since there are increasingly
		// problems occurring on 64 bit Linux/Mac
		/*if (Cudd_DebugCheck(ddman)) {
			printf("\nWarning: CUDD reports an error on closing.\n");
		}*/
		if (Cudd_CheckZeroRef(ddman) > 0) {
			fprintf(dd_out, "\nWarning: CUDD reports %d non-zero references.\n", Cudd_CheckZeroRef(ddman));
		}
	}
	
	// close down the CUDD package
	Cudd_Quit(ddman);
}

//-----------------------------------------------------------------------------------

int Cudd_CheckZeroRefVerbose(DdManager *manager)
{
 	int size;
 	int i, j;
 	int remain;
 	DdNode **nodelist;
 	DdNode *node;
 	DdNode *sentinel = &(manager->sentinel);
 	DdSubtable *subtable;
 	int count = 0;
 	int index;

	printf("Checking for non-zero references...\n");

 	/* First look at the BDD/ADD subtables. */
 	remain = 1; /* reference from the manager */
 	size = manager->size;
 	remain += 2 * size;	/* reference from the BDD projection functions */

 	for (i = 0; i < size; i++) {
	subtable = &(manager->subtables[i]);
	nodelist = subtable->nodelist;
	for (j = 0; (unsigned) j < subtable->slots; j++) {
	 	node = nodelist[j];
	 	while (node != sentinel) {
		if (node->ref != 0 && node->ref != DD_MAXREF) {
		 	index = (int) node->index;
		 	if (node != manager->vars[index]) {
				printf("* node found (index %d)\n", index);
				count++;
		 	} 
			else {
				if (node->ref != 1) {
					printf("* variable found (index %d)\n", index);
				 	count++;
				}
		 	}
		}
		node = node->next;
	 	}
	}
 	}

 	/* Then look at the ZDD subtables. */
 	size = manager->sizeZ;
 	if (size) /* references from ZDD universe */
	remain += 2;

 	for (i = 0; i < size; i++) {
	subtable = &(manager->subtableZ[i]);
	nodelist = subtable->nodelist;
	for (j = 0; (unsigned) j < subtable->slots; j++) {
	 	node = nodelist[j];
	 	while (node != NULL) {
		if (node->ref != 0 && node->ref != DD_MAXREF) {
		 	index = (int) node->index;
		 	if (node == manager->univ[manager->permZ[index]]) {
			if (node->ref > 2) {
			 	count++;
			}
		 	} else {
			count++;
		 	}
		}
		node = node->next;
	 	}
	}
 	}

 	/* Now examine the constant table. Plusinfinity, minusinfinity, and
 	** zero are referenced by the manager. One is referenced by the
 	** manager, by the ZDD universe, and by all projection functions.
 	** All other nodes should have no references.
 	*/
 	nodelist = manager->constants.nodelist;
 	for (j = 0; (unsigned) j < manager->constants.slots; j++) {
	node = nodelist[j];
	while (node != NULL) {
	 	if (node->ref != 0 && node->ref != DD_MAXREF) {
		if (node == manager->one) {
		 	if ((int) node->ref != remain) {
			count++;
		 	}
		} else if (node == manager->zero ||
		node == manager->plusinfinity ||
		node == manager->minusinfinity) {
		 	if (node->ref != 1) {
			count++;
		 	}
		} else {
			printf("* constant found (index %g)\n", node->type.value);
		 	count++;
		}
	 	}
	 	node = node->next;
	}
 	}
 	return(count);

}

//-----------------------------------------------------------------------------------

// -------------- Reference analysis ------------------------------------------------

// dump info about a node
static void dump_node(DdNode* node)
{
	if (Cudd_IsConstant(node)) {
		printf("%p: value=%f, refs=%d\n", node, Cudd_V(node), node->ref);
	} else {
		int index = node->index;
		printf("%p: var=%d, refs=%d\n", node, index, node->ref);
	}
}

// add a reference for node to the reference map
static void add_reference(std::map<DdNode*, int>& references, DdNode* node, int increase=1)
{
	std::map<DdNode*, int>::iterator it = references.find(node);
	if (it == references.end()) {
		// node was not yet registered
		references[node] = increase;
	} else {
		// increase the value
		int& v = it->second;
		v += increase;
	}
}

// Analyze the nodes, return all encountered nodes in the set nodes
// and return the number of internal references for each node in the
// map internal_refs.
// An internal reference is a reference via the then or else pointer of
// another node or a reference by the manager (projection functions, some constants).
static void DD_AnalyzeRefCounts(DdManager *manager, std::set<DdNode*>& nodes, std::map<DdNode*,int>& internal_refs)
{
 	int size;
 	int i, j;
 	int remain;
 	DdNode **nodelist;
 	DdNode *node;
 	DdNode *sentinel = &(manager->sentinel);
 	DdSubtable *subtable;
 	int count = 0;
 	int index;
 	const bool debug = false;

#ifndef DD_NO_DEATH_ROW
	cuddClearDeathRow(manager);
#endif

	if (manager->sizeZ > 0) {
		printf("Can not handle ZDD in Cudd, abort...");
		return;
	}

	size = manager->size;
	if (debug)
		printf("manager->size = %d\n", size);

 	for (i = 0; i < size; i++) {
	subtable = &(manager->subtables[i]);
	nodelist = subtable->nodelist;
	for (j = 0; (unsigned) j < subtable->slots; j++) {
	 	node = nodelist[j];
	 	while (node != sentinel) {
	 	nodes.insert(node);
	 	if (debug) printf("add:\n ");
	 	if (debug) dump_node(node);
	 	if (node->ref == DD_MAXREF) {
	 		// TODO: Error handling, how do we deal with this case?
	 	}
		index = (int) node->index;
		if (node == manager->vars[index]) {
			// a projection function, deal with references from the manager later
			if (debug) printf("%p is projection\n", node);
		} else if (node->ref > 0) {
			DdNode *t = Cudd_Regular(Cudd_T(node));
			add_reference(internal_refs, t);
			if (debug) printf("t ref: %p  -> %d\n", t, internal_refs[t]);
			DdNode *e = Cudd_Regular(Cudd_E(node));
			add_reference(internal_refs, e);
			if (debug) printf("e ref: %p  -> %d\n", e, internal_refs[e]);
		}
		node = node->next;
	 	}
	}
 	}

	for (i=0; i<size; i++) {
		node = manager->vars[i];
		add_reference(internal_refs, node);
		if (debug) printf("::%p projection -> %d\n", node, internal_refs[node]);
		DdNode *t = Cudd_Regular(Cudd_T(node));
		add_reference(internal_refs, t);
		if (debug) printf("t ref: %p  -> %d\n", t, internal_refs[t]);
		DdNode *e = Cudd_Regular(Cudd_E(node));
		add_reference(internal_refs, e);
		if (debug) printf("e ref: %p  -> %d\n", e, internal_refs[e]);
	}

 	// Examine the constant table. Plusinfinity, minusinfinity, one and
 	// zero are referenced by the manager.
 	nodelist = manager->constants.nodelist;
 	for (j = 0; (unsigned) j < manager->constants.slots; j++) {
	node = nodelist[j];
	while (node != NULL) {
		nodes.insert(node);
	 	if (node->ref == DD_MAXREF) {
	 		// Error handling
	 	}
		if (node == manager->one ||
		    node == manager->zero ||
		    node == manager->plusinfinity ||
		    node == manager->minusinfinity) {
			// manager refs
			add_reference(internal_refs, node);
			if (debug) printf("%p Constant(%f) -> %d\n", node, Cudd_V(node), internal_refs[node]);
		}
	 	node = node->next;
	}
	}
}


// Print a report about the nodes of this manager, with the number of internal references
void DD_ReportExternalRefCounts(DdManager *manager)
{
	std::set<DdNode*> nodes;
	std::map<DdNode*, int> internal_refs;

	DD_AnalyzeRefCounts(manager, nodes, internal_refs);
	printf("%s nodes\n", std::to_string(nodes.size()).c_str());  // no portable format string flag for size_t at the moment

	for (std::set<DdNode*>::iterator it = nodes.begin();
	     it != nodes.end();
	     ++it) {
	     DdNode* node = *it;
		printf("%d -> ", (internal_refs.find(node) != internal_refs.end() ? internal_refs[node] : 0));
		dump_node(node);
	}

	// Analysis
	printf("\nExternal references:\n");
	for (std::set<DdNode*>::iterator it = nodes.begin();
	     it != nodes.end();
	     ++it) {
		DdNode* node = *it;
		int internal = (internal_refs.find(node) != internal_refs.end() ? internal_refs[node] : 0);

		if (node->ref > internal) {
			dump_node(node);
			printf(" Internal references: %d\n", internal);
		} else if (node->ref < internal) {
			dump_node(node);
			printf("Underflow! Internal references: %d\n", internal);
		}
	}
}

// Analyze the BDD and return the implied number of external references per node
// in the map external_refs (only return nodes with non-zero external references)
void DD_GetExternalRefCounts(DdManager *manager, std::map<DdNode*,int>& external_refs)
{
	std::set<DdNode*> nodes;
	std::map<DdNode*, int> internal_refs;

	DD_AnalyzeRefCounts(manager, nodes, internal_refs);

	for (std::set<DdNode*>::iterator it = nodes.begin();
	     it != nodes.end();
	     ++it) {
		DdNode* node = *it;
		int internal = (internal_refs.find(node) != internal_refs.end() ? internal_refs[node] : 0);

		if (node->ref != internal) {
			external_refs[node] = node->ref - internal;
		}
	}
	// printf("Found %lu problematic nodes\n", external_refs.size());
}

//-----------------------------------------------------------------------------------

// Get the value of the DD error flag
bool DD_GetErrorFlag(DdManager *ddman)
{
	return dd_cudd_error_flag || (ddman->errorCode != CUDD_NO_ERROR);
}

//-----------------------------------------------------------------------------------

// Set the DD error flag. Should be set if a CUDD error has been
// detected that could not be signalled by returning a NULL DdNode*
// from the function
void DD_SetErrorFlag()
{
	dd_cudd_error_flag = true;
}


//-----------------------------------------------------------------------------------


