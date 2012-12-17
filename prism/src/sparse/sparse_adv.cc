//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Hongyang Qu <hongyang.qu@comlab.ox.ac.uk> (University of Oxford)
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

//#include <math.h>
//#include "dv.h"
#include "sparse_adv.h"
//#include "PrismSparseGlob.h"
//#include <new>

//------------------------------------------------------------------------------

// Export the model (MDP) as a dot file

void export_model(NDSparseMatrix *ndsm, int n, int *yes_vec, int start_index)
{
	FILE *f;
	int i, j, k, k_r, l1, h1, l2, h2, l2_r, h2_r, count;
	double d1, d2, kb, kbt;
	
	// Extract required info from sparse matrix
	double *non_zeros = ndsm->non_zeros;
	unsigned char *row_counts = ndsm->row_counts;
	int *row_starts = (int *)ndsm->row_counts;
	unsigned char *choice_counts = ndsm->choice_counts;
	int *choice_starts = (int *)ndsm->choice_counts;
	bool use_counts = ndsm->use_counts;
	unsigned int *cols = ndsm->cols;
	
	/* write the produce mdp to file model.dot */
	printf("Writing the model to model.dot\n"); fflush(stdout);

	f = fopen("model.dot", "w"); /* create a file for writing */
	//f = NULL;
	if(f==NULL) {
		printf("\nWarning: Output of graph cancelled (could not open file \"%s\").\n", "model.dot");
	} else {
		fprintf(f, "digraph model {\n");
		for(i=0; i<n; i++)
		if(i == start_index)
		fprintf(f, "	%1d [label=\"%1d\", shape=ellipse]\n", i, i);
		else if(yes_vec[i]> 0)
		fprintf(f, "	%1d [label=\"%1d\", shape=doublecircle]\n", i, i);
		else
		fprintf(f, "	%1d [label=\"%1d\", shape=circle]\n", i, i);
		count = n; l1 = h1 = l2 = h2 = 0;
		for(i=0; i<n; i++) {
			if (!use_counts) {
				l1 = row_starts[i];
				h1 = row_starts[i+1];
			} else {
				l1 = h1;
				h1 += row_counts[i];
			}
			for (j = l1; j < h1; j++) {
				if (!use_counts) {
					l2 = choice_starts[j];
					h2 = choice_starts[j+1];
				} else {
					l2 = h2;
					h2 += choice_counts[j];
				}
				if(h2-l2>1) {
					fprintf(f, "	%1d [label=\"\", shape=point]\n", count);
					fprintf(f, "		%1d -> %1d [label=\"%d\"]\n", i, count, j);
					for(k=l2; k<h2; k++)
					fprintf(f, "		%1d -> %1d [label=\"%g(%d)\"]\n", count,
							cols[k], non_zeros[k], k);
					count ++;
				} else
				fprintf(f, "		%1d -> %1d [label=\"%d:%g(%d)\"]\n", i,
						cols[l2], j, non_zeros[l2], l2);
			}
		}
		fprintf(f, "}\n");
		fclose(f);
	}
}

//------------------------------------------------------------------------------

// Export the adversary (as a dot file)

void export_adversary_ltl_dot(NDSparseMatrix *ndsm, int n, long nnz, int *yes_vec, double *maybe_vec, int num_lp_vars, int *map_var, double *lp_soln, int start_index)
{
	long sp_nodes = 0; // pointer of dot nodes
	long extra_node = n; // index of intermediate nodes for actions
	long sp_edges = 0; // pointer of dot edges
	//int queue[n]; // search queue
	int *queue;
	long head = 0; // pointer to the head of the queue
	long tail = 0; // pointer to the tail of the queue
	//int nodes[n]; // indicate whether the node has been visited
	int *nodes;

	FILE *f;
	int i, j, k, k_r, l1, h1, l2, h2, l2_r, h2_r, count;
	double d1, d2, kb, kbt;

	// Extract required info from sparse matrix
	double *non_zeros = ndsm->non_zeros;
	unsigned char *row_counts = ndsm->row_counts;
	int *row_starts = (int *)ndsm->row_counts;
	unsigned char *choice_counts = ndsm->choice_counts;
	int *choice_starts = (int *)ndsm->choice_counts;
	bool use_counts = ndsm->use_counts;
	unsigned int *cols = ndsm->cols;
	
	queue = new int[n];
	nodes = new int[n];
	for(i=0; i<n; i++)
		nodes[i] = 0;
	
	int (*dot_nodes)[2];
	int (*dot_edges)[2];
	double *edge_probabilities;
	int *terminate_nodes;
	//printf("allocated memory for local variables:\n"); 
	//printf("n=%1d, num_lp_vars=%1d, nnz=%1d\n", n, num_lp_vars, nnz);
	//fflush(stdout);
	
	//int dot_nodes[n+num_lp_vars][2]; // first entry: node number, second: shape 
	//int dot_edges[nnz+num_lp_vars][2]; // first entry: source node, second: target
	//double edge_probabilities[nnz+num_lp_vars]; // edge's probability
	
	dot_nodes = new int[n+num_lp_vars][2];
	dot_edges = new int[nnz+num_lp_vars][2];
	edge_probabilities = new double[nnz+num_lp_vars];
	terminate_nodes = new int[n]; // the entry stores the node number for newly added terminate nodes
	
	for(i=0; i<n; i++)
		terminate_nodes[i] = -1;
	
	// put the initial state in the queue and dot_nodes
	queue[tail++] = start_index;
	dot_nodes[sp_nodes][0] = start_index;
	dot_nodes[sp_nodes++][1] = 0; // shape = 0: ellipse, 1: circle, 2: point, 3: doublecircle, 4: box
	
	// recursive procedure
	int head_node = -1;
	while(head < tail) {
		head_node = queue[head++];
		
		if(nodes[head_node] == 1)
			continue;
		nodes[head_node] = 1;
		// If the head node is a target state, do nothing
		if(yes_vec[head_node]> 0 || maybe_vec[head_node]> 0) {
							// the number of branches in the current state
			h1 = map_var[head_node+1] - map_var[head_node];
			
			double sum = 0;
			for(i=0; i<h1; i++)
				sum += lp_soln[map_var[head_node] + i];
			
			for(i=0; i<h1; i++) {
				// test if the action has non-zero probability
				if(lp_soln[map_var[head_node] + i]> 0) {
					if(yes_vec[head_node]> 0 && i==h1-1) { // extra transition
						// add an intermediate node to dot_nodes
						if(terminate_nodes[head_node] < 0) {
							dot_nodes[sp_nodes][0] = extra_node;
							dot_nodes[sp_nodes++][1] = 4;
							dot_edges[sp_edges][1] = extra_node;
							terminate_nodes[head_node] = extra_node;
						} else
							dot_edges[sp_edges][1] = terminate_nodes[head_node];
						// add an edge to dot_edges
						dot_edges[sp_edges][0] = head_node;
						// add the probability to the edge
						//edge_probabilities[sp_edges++] = lp_soln[map_var[head_node] + i];
						edge_probabilities[sp_edges++] = sum == 1 ? lp_soln[map_var[head_node] + i] :
							lp_soln[map_var[head_node] + i]/sum;
						extra_node++;
					} else {
						// get all successor states of this action
						// First: locate the action
						if (!use_counts) {
							l1 = row_starts[head_node];
						} else {
							l1 = 0;
							for(j=0; j<head_node; j++)
								l1 += row_counts[j];
						}
						l1 += i;
						
						// Second: find all columns for this choice
						if (!use_counts) {
							l2 = choice_starts[l1];
							h2 = choice_starts[l1+1];
						} else {
							l2 = 0;
							for(j=0; j<l1; j++)
								l2 += choice_counts[j];
							h2 = l2 + choice_counts[j];
						}
						if(h2-l2>1) {
							// add an intermediate node to dot_nodes
							dot_nodes[sp_nodes][0] = extra_node;
							dot_nodes[sp_nodes++][1] = 2;
							// add an edge to dot_edges
							dot_edges[sp_edges][0] = head_node;
							dot_edges[sp_edges][1] = extra_node;
							// add the probability to the edge
							//edge_probabilities[sp_edges++] = lp_soln[map_var[head_node] + i];
							edge_probabilities[sp_edges++] = sum == 1 ? lp_soln[map_var[head_node] + i] :
								lp_soln[map_var[head_node] + i]/sum;
							
							for(j=l2; j<h2; j++) {
								// add the successor state to dot_nodes
								dot_nodes[sp_nodes][0] = cols[j];
								if(yes_vec[cols[j]]> 0)
									dot_nodes[sp_nodes++][1] = 3;
								else
									dot_nodes[sp_nodes++][1] = 1;
								
								// add an edge to dot_edges
								dot_edges[sp_edges][0] = extra_node;
								dot_edges[sp_edges][1] = cols[j];
								// add the probability to the edge
								edge_probabilities[sp_edges++] = non_zeros[j];
								
								if((maybe_vec[cols[j]]> 0 || yes_vec[cols[j]]> 0) &&
									 nodes[cols[j]] == 0)
									queue[tail++] = cols[j];
							}
							extra_node++;
						} else {
							// add the successor state to dot_nodes
							dot_nodes[sp_nodes][0] = cols[l2];
							if(yes_vec[cols[l2]]> 0)
								dot_nodes[sp_nodes++][1] = 3;
							else
								dot_nodes[sp_nodes++][1] = 1;
							// add an edge to dot_edges
							dot_edges[sp_edges][0] = head_node;
							dot_edges[sp_edges][1] = cols[l2];
							// add the probability to the edge
							//edge_probabilities[sp_edges++] = lp_soln[map_var[head_node] + i];
							edge_probabilities[sp_edges++] = sum == 1 ? lp_soln[map_var[head_node] + i] :
								lp_soln[map_var[head_node] + i]/sum;
							
							if((maybe_vec[cols[l2]]> 0 || yes_vec[cols[l2]]> 0) &&
								 nodes[cols[l2]] == 0) {
								queue[tail++] = cols[l2];
							}
						}
					}
				}
			}
		}
	}	
	
	printf("generating adversary file\n"); fflush(stdout);
	
	// write to file
	f = fopen("adversary.dot", "w"); /* create a file for writing */
	if(f==NULL) {
		printf("\nWarning: Output of adversary cancelled (could not open file \"%s\").\n", "adversary.dot");
	} else {
		fprintf(f, "digraph adversary {\n");
		for(i=0; i<sp_nodes; i++) {
			if(dot_nodes[i][0] >= n) {
				if(dot_nodes[i][1] == 2)
					fprintf(f, "	%1d [label=\"\", shape=point]\n", dot_nodes[i][0]);
				else
					fprintf(f, "	%1d [label=\"\", shape=box, fillcolor=black]\n", dot_nodes[i][0]);
			} else
				fprintf(f, "	%1d [label=\"%1d\", shape=%s]\n", dot_nodes[i][0],
								dot_nodes[i][0],
								((dot_nodes[i][1]==0)? "ellipse" :
								 ((dot_nodes[i][1]==1)? "circle" : "doublecircle")));
		}
		for(i=0; i<sp_edges; i++) {
			fprintf(f, "		%1d -> %1d [label=\"%g\"]\n", dot_edges[i][0],
							dot_edges[i][1], edge_probabilities[i]);
		}
		fprintf(f, "}\n");
		fclose(f);
	}
	
	delete[] dot_nodes;
	delete[] dot_edges;
	delete[] edge_probabilities;
	delete[] terminate_nodes;
	
	delete[] queue;
	delete[] nodes;
}


//------------------------------------------------------------------------------

// Export the adversary (as a dot file)

void export_adversary_ltl_dot_reward(const char *export_adv_filename, NDSparseMatrix *ndsm, 
																		 int *actions, const char** action_names, 
																		 int n, long nnz, int *yes_vec, double *maybe_vec, 
																		 int num_lp_vars, int *map_var, double *lp_soln, double *back_arr_reals, 
																		 int start_index)
{
	long sp_nodes = 0; // pointer of dot nodes
	long extra_node = n; // index of intermediate nodes for actions
	long sp_edges = 0; // pointer of dot edges
	//int queue[n]; // search queue
	int *queue;
	long head = 0; // pointer to the head of the queue
	long tail = 0; // pointer to the tail of the queue
	//int nodes[n]; // indicate whether the node has been visited
	int *nodes;

	FILE *f, *f1;
	int i, j, k, k_r, l1, h1, l2, h2, l2_r, h2_r, count;
	double d1, d2, kb, kbt;

	// Extract required info from sparse matrix
	double *non_zeros = ndsm->non_zeros;
	unsigned char *row_counts = ndsm->row_counts;
	int *row_starts = (int *)ndsm->row_counts;
	unsigned char *choice_counts = ndsm->choice_counts;
	int *choice_starts = (int *)ndsm->choice_counts;
	bool use_counts = ndsm->use_counts;
	unsigned int *cols = ndsm->cols;

	queue = new int[n];
	nodes = new int[n];
	for(i=0; i<n; i++)
		nodes[i] = 0;
	
	int (*dot_nodes)[2];
	int (*dot_edges)[2];
	int headnode_sp;
	int headnode_len;
	double *edge_probabilities;
	unsigned long *edge_labels;
	int *edge_weight;
	int *terminate_nodes;
	//printf("allocated memory for local variables:\n"); 
	//printf("n=%1d, num_lp_vars=%1d, nnz=%1d\n", n, num_lp_vars, nnz);
	//fflush(stdout);
	
	//int dot_nodes[n+num_lp_vars][2]; // first entry: node number, second: shape 
	//int dot_edges[nnz+num_lp_vars][2]; // first entry: source node, second: target
	//double edge_probabilities[nnz+num_lp_vars]; // edge's probability
	
	dot_nodes = new int[n+num_lp_vars][2];
	dot_edges = new int[nnz+num_lp_vars][2];
	edge_probabilities = new double[nnz+num_lp_vars];
	edge_labels = new unsigned long[nnz+num_lp_vars];
	edge_weight = new int[nnz+num_lp_vars];
	terminate_nodes = new int[n]; // the entry stores the node number for newly added terminate nodes
	
	for(i=0; i<n; i++)
		terminate_nodes[i] = -1;
	
	// put the initial state in the queue and dot_nodes
	queue[tail++] = start_index;
	dot_nodes[sp_nodes][0] = start_index;
	dot_nodes[sp_nodes++][1] = 0; // shape = 0: ellipse, 1: circle, 2: point, 3: doublecircle, 4: box
	
	// recursive procedure
	int head_node = -1;
	while(head < tail) {
		head_node = queue[head++];
		
		if(nodes[head_node] == 1)
			continue;
		nodes[head_node] = 1;
		// If the head node is a target state, do nothing
		if(yes_vec[head_node]> 0 || maybe_vec[head_node]> 0) {
			if (!use_counts) {
				headnode_sp = row_starts[head_node];
				headnode_len = row_starts[head_node+1] - row_starts[head_node];
			} else {
				headnode_sp = 0;
				for(j=0; j<head_node; j++)
					headnode_sp += row_counts[j];
				headnode_len = row_counts[head_node];
			}
			// the number of branches in the current state
			h1 = map_var[head_node+1] - map_var[head_node];
			
			double sum = 0;
			for(i=0; i<h1; i++)
				sum += lp_soln[map_var[head_node] + i];
			
			for(i=0; i<h1; i++) {
				// test if the action has non-zero probability
				if(lp_soln[map_var[head_node] + i]> 0) {
					if(yes_vec[head_node]> 0 && i>=headnode_len) { // extra transition
						// add an intermediate node to dot_nodes
						if(terminate_nodes[head_node] < 0) {
							dot_nodes[sp_nodes][0] = extra_node;
							dot_nodes[sp_nodes++][1] = 4;
							dot_edges[sp_edges][1] = extra_node;
							terminate_nodes[head_node] = extra_node;
						} else
							dot_edges[sp_edges][1] = terminate_nodes[head_node];
						// add an edge to dot_edges
						dot_edges[sp_edges][0] = head_node;
						// add the probability to the edge
						//edge_probabilities[sp_edges++] = lp_soln[map_var[head_node] + i];
						edge_labels[sp_edges] = 0;
						edge_weight[sp_edges] = 0;
						edge_probabilities[sp_edges++] = sum == 1 ? lp_soln[map_var[head_node] + i] :
							lp_soln[map_var[head_node] + i]/sum;
						extra_node++;
					} else if(i<headnode_len) {
						// get all successor states of this action
						// First: locate the action
						l1 = headnode_sp + i;
						
						// Second: find all columns for this choice
						if (!use_counts) {
							l2 = choice_starts[l1];
							h2 = choice_starts[l1+1];
						} else {
							l2 = 0;
							for(j=0; j<l1; j++)
								l2 += choice_counts[j];
							h2 = l2 + choice_counts[j];
						}
						if(h2-l2>1) {
							// add an intermediate node to dot_nodes
							dot_nodes[sp_nodes][0] = extra_node;
							dot_nodes[sp_nodes++][1] = 2;
							// add an edge to dot_edges
							dot_edges[sp_edges][0] = head_node;
							dot_edges[sp_edges][1] = extra_node;
							// add the probability to the edge
							//edge_probabilities[sp_edges++] = lp_soln[map_var[head_node] + i];
							//printf("headnode = %d, l1 = %d\n", head_node, l1);
							edge_labels[sp_edges] = (actions != NULL && actions[l1]>0) ? (unsigned long)(action_names[actions[l1]-1]) : 0;
							edge_weight[sp_edges] = back_arr_reals[map_var[head_node] + i] > 0.0 ? 1 : 0;
							edge_probabilities[sp_edges++] = sum == 1 ? lp_soln[map_var[head_node] + i] :
								lp_soln[map_var[head_node] + i]/sum;
							
							for(j=l2; j<h2; j++) {
								// add the successor state to dot_nodes
								k=1;
								for(k_r=0; k_r<sp_nodes; k_r++) 
									if(dot_nodes[k_r][0] == cols[j]) {
										k=0; 
										break;
									}
								if(k) {
									dot_nodes[sp_nodes][0] = cols[j];
									if(yes_vec[cols[j]]> 0)
										dot_nodes[sp_nodes++][1] = 3;
									else
										dot_nodes[sp_nodes++][1] = 1;
								}
								// add an edge to dot_edges
								dot_edges[sp_edges][0] = extra_node;
								dot_edges[sp_edges][1] = cols[j];
								// add the probability to the edge
								edge_labels[sp_edges] = 0;
								edge_weight[sp_edges] = 0;
								edge_probabilities[sp_edges++] = non_zeros[j];
								
								if((maybe_vec[cols[j]]> 0 || yes_vec[cols[j]]> 0) &&
									 nodes[cols[j]] == 0)
									queue[tail++] = cols[j];
							}
							extra_node++;
						} else {
							// add the successor state to dot_nodes
							k=1;
							for(j=0; j<sp_nodes; j++) 
								if(dot_nodes[j][0] == cols[l2]) {
									k=0; 
									break;
								}
							if(k) {
								dot_nodes[sp_nodes][0] = cols[l2];
								if(yes_vec[cols[l2]]> 0)
									dot_nodes[sp_nodes++][1] = 3;
								else
									dot_nodes[sp_nodes++][1] = 1;
							}
							// add an edge to dot_edges
							dot_edges[sp_edges][0] = head_node;
							dot_edges[sp_edges][1] = cols[l2];
							// add the probability to the edge
							//edge_probabilities[sp_edges++] = lp_soln[map_var[head_node] + i];
							//printf("headnode = %d, len = %d, l1 = %d\n", head_node, headnode_len, l1);
							edge_labels[sp_edges] = (actions != NULL && actions[l1]>0) ? (unsigned long)(action_names[actions[l1]-1]) : 0;
							edge_weight[sp_edges] = back_arr_reals[map_var[head_node] + i] > 0.0 ? 1 : 0;
							edge_probabilities[sp_edges++] = sum == 1 ? lp_soln[map_var[head_node] + i] :
								lp_soln[map_var[head_node] + i]/sum;
							
							if((maybe_vec[cols[l2]]> 0 || yes_vec[cols[l2]]> 0) &&
								 nodes[cols[l2]] == 0) {
								queue[tail++] = cols[l2];
							}
						}
					}
				}
			}
		}
	}	
	
	printf("generating adversary file\n"); fflush(stdout);
	
	// write to file
	int len = strlen(export_adv_filename);
	char style[] = ", style=bold";
	char *fname = new char[len+5];
	memcpy (fname, export_adv_filename, len+1);
	fname[len] = '.';
	fname[len+1] = 'd';
	fname[len+2] = 'o';
	fname[len+3] = 't';
	fname[len+4] = '\0';
	f = fopen(fname, "w"); /* create a file for writing */

	if(f==NULL) {
		printf("\nWarning: Output of adversary cancelled (could not open file \"%s\").\n", fname);
	} else {
		f1 = fopen("product-multi.dot", "r");
		char states[n][200];
		int state_sp = 0;
		char *lb, *rb;
		if(f1 != NULL) {
			printf("\nUsing product-multi.dot to extract state information\n");
			char line[200];
			while (fgets(line, sizeof(line), f1) != NULL ) {
				lb = strchr(line, '(');
				rb = strchr(line, ')');
				if(lb != NULL && rb != NULL && ((unsigned long)rb) > ((unsigned long)lb)) {
					*(rb+1) = '\0';
					strcpy(states[state_sp++], lb); 
				}
			}
			fclose (f1);
		}
		fprintf(f, "digraph adversary {\n");
		for(i=0; i<sp_nodes; i++) {
			if(dot_nodes[i][0] >= n) {
				if(dot_nodes[i][1] == 2)
					fprintf(f, "	%1d [label=\"\", shape=point]\n", dot_nodes[i][0]);
				else
					fprintf(f, "	%1d [label=\"\", shape=box, fillcolor=black]\n", dot_nodes[i][0]);
			} else {
				if(dot_nodes[i][0]<state_sp)
					fprintf(f, "	%1d [label=\"%1d\\n%s\", shape=%s]\n", dot_nodes[i][0], 
									dot_nodes[i][0], states[dot_nodes[i][0]],
									((dot_nodes[i][1]==0)? "ellipse" :
									 ((dot_nodes[i][1]==1)? "octagon" : "doubleoctagon")));
				else
					fprintf(f, "	%1d [label=\"%1d\", shape=%s]\n", dot_nodes[i][0],
									dot_nodes[i][0],
									((dot_nodes[i][1]==0)? "ellipse" :
									 ((dot_nodes[i][1]==1)? "octagon" : "doubleoctagon")));
			}
		}
		for(i=0; i<sp_edges; i++) {
			//printf("printing edges %d\n", i); fflush(stdout);
			if(edge_labels[i])
				fprintf(f, "		%1d -> %1d [label=\"%g, %s\"%s]\n", dot_edges[i][0],
								dot_edges[i][1], edge_probabilities[i], (char *)(edge_labels[i]), edge_weight[i] ? style : "");
			else
				fprintf(f, "		%1d -> %1d [label=\"%g\"%s]\n", dot_edges[i][0],
								dot_edges[i][1], edge_probabilities[i], edge_weight[i] ? style : "");
		}
		fprintf(f, "}\n");
		fclose(f);
	}
	
	delete[] dot_nodes;
	delete[] dot_edges;
	delete[] edge_probabilities;
	delete[] edge_labels;
	delete[] edge_weight;
	delete[] terminate_nodes;
	
	delete[] queue;
	delete[] nodes;
}
//------------------------------------------------------------------------------

// Export the adversary (as a tra file)

void export_adversary_ltl_tra(const char *export_adv_filename, NDSparseMatrix *ndsm, int *actions, const char** action_names, int *yes_vec, double *maybe_vec, int num_lp_vars, int *map_var, double *lp_soln, int start_index)
{
	FILE *f;
	int i, j, k, k_r, l1, h1, l2, h2, l2_r, h2_r, count;
	double d1, d2, kb, kbt;

	FILE *fp_adv;
	double sum, d;
	
	// Extract required info from sparse matrix
	int n = ndsm->n;
	int nnz = ndsm->nnz;
	double *non_zeros = ndsm->non_zeros;
	unsigned char *row_counts = ndsm->row_counts;
	int *row_starts = (int *)ndsm->row_counts;
	unsigned char *choice_counts = ndsm->choice_counts;
	int *choice_starts = (int *)ndsm->choice_counts;
	bool use_counts = ndsm->use_counts;
	unsigned int *cols = ndsm->cols;

	// Open file to store adversary
	fp_adv = fopen(export_adv_filename, "w");
	if (fp_adv) {
		fprintf(fp_adv, "%d ?\n", n);
	} else {
		printf("\nWarning: Adversary generation cancelled (could not open file \"%s\").\n", export_adv_filename);
		return;
	}
	
	// Traverse sparse matrix to get adversary
	h1 = h2 = 0;
	for (i = 0; i < n; i++) {
		// Compute sum of adversary choice weights for this state
		sum = 0.0;
		for (j = 0; j < (map_var[i+1] - map_var[i]); j++)
			sum += lp_soln[map_var[i] + j];
		// Go through choices
		if (!use_counts) { l1 = row_starts[i]; h1 = row_starts[i+1]; }
		else { l1 = h1; h1 += row_counts[i]; }
		for (j = l1; j < h1; j++) {
			if (!use_counts) { l2 = choice_starts[j]; h2 = choice_starts[j+1]; }
			else { l2 = h2; h2 += choice_counts[j]; }
			// Get weight for this choice
			d = lp_soln[map_var[i] + j - l1];
			if (d > 0) {
				// Normalise weight to get prob
				d /= sum;
				for (k = l2; k < h2; k++) {
					fprintf(fp_adv, "%d %d %g", i, cols[k], d * non_zeros[k]);
					if (actions != NULL) fprintf(fp_adv, " %s", actions[j]>0?action_names[actions[j]-1]:"-");
					fprintf(fp_adv, "\n");
				}
			}
		}
		// Add action to loop in this EC, if required
		if (yes_vec[i] && lp_soln[map_var[i + 1] - 1] > 0) {
			fprintf(fp_adv, "%d %d %g _ec\n", i, i, lp_soln[map_var[i + 1] - 1] / sum);
		}
	}
	
	// Close file
	fclose(fp_adv);
}

//------------------------------------------------------------------------------
