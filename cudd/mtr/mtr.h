/**
  @file 

  @ingroup mtr

  @brief Multiway-branch tree manipulation

  @details This package provides two layers of functions. Functions
  of the lower level manipulate multiway-branch trees, implemented
  according to the classical scheme whereby each node points to its
  first child and its previous and next siblings. These functions are
  collected in mtrBasic.c.<p>
  Functions of the upper layer deal with group trees, that is the trees
  used by group sifting to represent the grouping of variables. These
  functions are collected in mtrGroup.c.

  @see The CUDD package documentation; specifically on group
  sifting.

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

#ifndef MTR_H_
#define MTR_H_

/*---------------------------------------------------------------------------*/
/* Nested includes                                                           */
/*---------------------------------------------------------------------------*/

#include <stdio.h>

#ifdef __cplusplus
extern "C" {
#endif

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

/* Flag definitions */
#define MTR_DEFAULT	0x00000000
#define MTR_TERMINAL	0x00000001
#define MTR_SOFT	0x00000002
#define MTR_FIXED	0x00000004
#define MTR_NEWNODE	0x00000008

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/**
 * @brief multi-way tree node.
 */
typedef struct MtrNode_ MtrNode;

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Function prototypes                                                       */
/*---------------------------------------------------------------------------*/

MtrNode * Mtr_AllocNode(void);
void Mtr_DeallocNode(MtrNode *node);
MtrNode * Mtr_InitTree(void);
void Mtr_FreeTree(MtrNode *node);
MtrNode * Mtr_CopyTree(MtrNode const *node, int expansion);
void Mtr_MakeFirstChild(MtrNode *parent, MtrNode *child);
void Mtr_MakeLastChild(MtrNode *parent, MtrNode *child);
MtrNode * Mtr_CreateFirstChild(MtrNode *parent);
MtrNode * Mtr_CreateLastChild(MtrNode *parent);
void Mtr_MakeNextSibling(MtrNode *first, MtrNode *second);
void Mtr_PrintTree(MtrNode const *node);
MtrNode * Mtr_InitGroupTree(int lower, int size);
MtrNode * Mtr_MakeGroup(MtrNode *root, unsigned int low, unsigned int high, unsigned int flags);
MtrNode * Mtr_DissolveGroup(MtrNode *group);
MtrNode * Mtr_FindGroup(MtrNode *root, unsigned int low, unsigned int high);
int Mtr_SwapGroups(MtrNode *first, MtrNode *second);
void Mtr_ReorderGroups(MtrNode *treenode, int *permutation);
void Mtr_PrintGroups(MtrNode const *root, int silent);
int Mtr_PrintGroupedOrder(MtrNode const * root, int const *invperm, FILE *fp);
MtrNode * Mtr_ReadGroups(FILE *fp, int nleaves);

#ifdef __cplusplus
}
#endif

#endif /* MTR_H_ */
