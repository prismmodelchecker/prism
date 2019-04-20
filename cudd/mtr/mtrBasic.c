/**
  @file

  @ingroup mtr

  @brief Basic manipulation of multiway branching trees.

  @see cudd package

  @author Fabio Somenzi

  @copyright@parblock
  Copyright (c) 1995-2015, Regents of the University of Colorado

  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

  Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.

  Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

  Neither the name of the University of Colorado nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.
  @endparblock

*/

#include "util.h"
#include "mtrInt.h"


/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/

/**
  @brief Allocates new tree node.

  @return pointer to node.

  @sideeffect None

  @see Mtr_DeallocNode

*/
MtrNode *
Mtr_AllocNode(void)
{
    MtrNode *node;

    node = ALLOC(MtrNode,1);
    node->flags = node->low = node->size = node->index = 0;
    return node;

} /* Mtr_AllocNode */


/**
  @brief Deallocates tree node.

  @sideeffect None

  @see Mtr_AllocNode

*/
void
Mtr_DeallocNode(
  MtrNode * node /**< node to be deallocated */)
{
    FREE(node);
    return;

} /* end of Mtr_DeallocNode */


/**
  @brief Initializes tree with one node.

  @return pointer to node.

  @sideeffect None

  @see Mtr_FreeTree Mtr_InitGroupTree

*/
MtrNode *
Mtr_InitTree(void)
{
    MtrNode *node;

    node = Mtr_AllocNode();
    if (node == NULL) return(NULL);

    node->parent = node->child = node->elder = node->younger = NULL;

    return(node);

} /* end of Mtr_InitTree */


/**
  @brief Disposes of tree rooted at node.

  @sideeffect None

  @see Mtr_InitTree

*/
void
Mtr_FreeTree(
  MtrNode * node)
{
    if (node == NULL) return;
    if (! MTR_TEST(node,MTR_TERMINAL)) Mtr_FreeTree(node->child);
    Mtr_FreeTree(node->younger);
    Mtr_DeallocNode(node);
    return;

} /* end of Mtr_FreeTree */


/**
  @brief Makes a copy of tree.

  @details If parameter expansion is greater than 1, it will expand
  the tree by that factor. It is an error for expansion to be less
  than 1.

  @return a pointer to the copy if successful; NULL otherwise.

  @sideeffect None

  @see Mtr_InitTree

*/
MtrNode *
Mtr_CopyTree(
  MtrNode const * node,
  int  expansion)
{
    MtrNode *copy;

    if (node == NULL) return(NULL);
    if (expansion < 1) return(NULL);
    copy = Mtr_AllocNode();
    if (copy == NULL) return(NULL);
    copy->parent = copy->elder = copy->child = copy->younger = NULL;
    if (node->child != NULL) {
	copy->child = Mtr_CopyTree(node->child, expansion);
	if (copy->child == NULL) {
	    Mtr_DeallocNode(copy);
	    return(NULL);
	}
    }
    if (node->younger != NULL) {
	copy->younger = Mtr_CopyTree(node->younger, expansion);
	if (copy->younger == NULL) {
	    Mtr_FreeTree(copy);
	    return(NULL);
	}
    }
    copy->flags = node->flags;
    copy->low = node->low * expansion;
    copy->size = node->size * expansion;
    copy->index = node->index * expansion;
    if (copy->younger) copy->younger->elder = copy;
    if (copy->child) {
	MtrNode *auxnode = copy->child;
	while (auxnode != NULL) {
	    auxnode->parent = copy;
	    auxnode = auxnode->younger;
	}
    }
    return(copy);

} /* end of Mtr_CopyTree */


/**
  @brief Makes child the first child of parent.

  @sideeffect None

  @see Mtr_MakeLastChild Mtr_CreateFirstChild

*/
void
Mtr_MakeFirstChild(
  MtrNode * parent,
  MtrNode * child)
{
    child->parent = parent;
    child->younger = parent->child;
    child->elder = NULL;
    if (parent->child != NULL) {
#ifdef MTR_DEBUG
	assert(parent->child->elder == NULL);
#endif
	parent->child->elder = child;
    }
    parent->child = child;
    return;

} /* end of Mtr_MakeFirstChild */


/**
  @brief Makes child the last child of parent.

  @sideeffect None

  @see Mtr_MakeFirstChild Mtr_CreateLastChild

*/
void
Mtr_MakeLastChild(
  MtrNode * parent,
  MtrNode * child)
{
    MtrNode *node;

    child->younger = NULL;

    if (parent->child == NULL) {
	parent->child = child;
	child->elder = NULL;
    } else {
	for (node = parent->child;
	     node->younger != NULL;
	     node = node->younger);
	node->younger = child;
	child->elder = node;
    }
    child->parent = parent;
    return;

} /* end of Mtr_MakeLastChild */


/**
  @brief Creates a new node and makes it the first child of parent.

  @return pointer to new child.

  @sideeffect None

  @see Mtr_MakeFirstChild Mtr_CreateLastChild

*/
MtrNode *
Mtr_CreateFirstChild(
  MtrNode * parent)
{
    MtrNode *child;

    child = Mtr_AllocNode();
    if (child == NULL) return(NULL);

    child->child = NULL;
    Mtr_MakeFirstChild(parent,child);
    return(child);

} /* end of Mtr_CreateFirstChild */


/**
  @brief Creates a new node and makes it the last child of parent.

  @return pointer to new child.

  @sideeffect None

  @see Mtr_MakeLastChild Mtr_CreateFirstChild

*/
MtrNode *
Mtr_CreateLastChild(
  MtrNode * parent)
{
    MtrNode *child;

    child = Mtr_AllocNode();
    if (child == NULL) return(NULL);

    child->child = NULL;
    Mtr_MakeLastChild(parent,child);
    return(child);

} /* end of Mtr_CreateLastChild */


/**
  @brief Makes second the next sibling of first.

  @details Second becomes a child of the parent of first.

  @sideeffect None

*/
void
Mtr_MakeNextSibling(
  MtrNode * first,
  MtrNode * second)
{
    second->parent = first->parent;
    second->elder = first;
    second->younger = first->younger;
    if (first->younger != NULL) {
	first->younger->elder = second;
    }
    first->younger = second;
    return;

} /* end of Mtr_MakeNextSibling */


/**
  @brief Prints a tree, one node per line.

  @sideeffect None

  @see Mtr_PrintGroups

*/
void
Mtr_PrintTree(
  MtrNode const * node)
{
    if (node == NULL) return;
    (void) fprintf(stdout,
        "N=0x%-8" PRIxPTR " C=0x%-8" PRIxPTR " Y=0x%-8" PRIxPTR
        " E=0x%-8" PRIxPTR " P=0x%-8" PRIxPTR " F=%x L=%u S=%u\n",
        (uintptr_t) node, (uintptr_t) node->child,
        (uintptr_t) node->younger, (uintptr_t) node->elder,
        (uintptr_t) node->parent, node->flags, node->low, node->size);
    if (!MTR_TEST(node,MTR_TERMINAL)) Mtr_PrintTree(node->child);
    Mtr_PrintTree(node->younger);
    return;

} /* end of Mtr_PrintTree */

/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/
