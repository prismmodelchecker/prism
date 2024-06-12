/**
  @file

  @ingroup cudd

  @brief Procedure to manage level queues.

  @details The functions in this file allow an application to easily
  manipulate a queue where nodes are prioritized by level. The
  emphasis is on efficiency. Therefore, the queue items can have
  variable size.  If the application does not need to attach
  information to the nodes, it can declare the queue items to be of
  type DdQueueItem. Otherwise, it can declare them to be of a
  structure type such that the first three fields are data
  pointers. The third pointer points to the node.  The first two
  pointers are used by the level queue functions. The remaining fields
  are initialized to 0 when a new item is created, and are then left
  to the exclusive use of the application. On the DEC Alphas the three
  pointers must be 32-bit pointers when CUDD is compiled with 32-bit
  pointers.  The level queue functions make sure that each node
  appears at most once in the queue. They do so by keeping a hash
  table where the node is used as key.  Queue items are recycled via a
  free list for efficiency.

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
#include "cuddInt.h"

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


/**
  @brief Hash function for the table of a level queue.

  @sideeffect None

  @see hashInsert hashLookup hashDelete

*/
#if SIZEOF_VOID_P == 8 && SIZEOF_INT == 4
#define lqHash(key,shift) \
(((unsigned)(ptruint)(key) * DD_P1) >> (shift))
#else
#define lqHash(key,shift) \
(((unsigned)(key) * DD_P1) >> (shift))
#endif


/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static DdQueueItem * hashLookup(DdLevelQueue *queue, void *key);
static int hashInsert(DdLevelQueue *queue, DdQueueItem *item);
static void hashDelete(DdLevelQueue *queue, DdQueueItem *item);
static int hashResize(DdLevelQueue *queue);

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Initializes a level queue.

  @details A level queue is a queue where inserts are based on the
  levels of the nodes. Within each level the policy is FIFO. Level
  queues are useful in traversing a %BDD top-down. Queue items are kept
  in a free list when dequeued for efficiency.

  @return a pointer to the new queue if successful; NULL otherwise.

  @sideeffect None

  @see cuddLevelQueueQuit cuddLevelQueueEnqueue cuddLevelQueueDequeue

*/
DdLevelQueue *
cuddLevelQueueInit(
  int  levels /**< number of levels */,
  int  itemSize /**< size of the item */,
  int  numBuckets /**< initial number of hash buckets */,
  DdManager * manager /*<< DD manager */)
{
    DdLevelQueue *queue;
    int logSize;

    queue = ALLOC(DdLevelQueue,1);
    if (queue == NULL)
	return(NULL);
    /* Keep pointers to the insertion points for all levels. */
    queue->last = ALLOC(DdQueueItem *, levels);
    if (queue->last == NULL) {
	FREE(queue);
	return(NULL);
    }
    /* Use a hash table to test for uniqueness. */
    if (numBuckets < 2) numBuckets = 2;
    logSize = cuddComputeFloorLog2(numBuckets);
    queue->numBuckets = 1U << logSize;
    queue->shift = sizeof(int) * 8 - logSize;
    queue->buckets = ALLOC(DdQueueItem *, queue->numBuckets);
    if (queue->buckets == NULL) {
	FREE(queue->last);
	FREE(queue);
	return(NULL);
    }
    memset(queue->last, 0, levels * sizeof(DdQueueItem *));
    memset(queue->buckets, 0, queue->numBuckets * sizeof(DdQueueItem *));
    queue->first = NULL;
    queue->freelist = NULL;
    queue->levels = levels;
    queue->itemsize = itemSize;
    queue->size = 0;
    queue->maxsize = queue->numBuckets * DD_MAX_SUBTABLE_DENSITY;
    queue->manager = manager;
    return(queue);

} /* end of cuddLevelQueueInit */


/**
  @brief Shuts down a level queue.

  @details Releases all the associated memory.

  @sideeffect None

  @see cuddLevelQueueInit

*/
void
cuddLevelQueueQuit(
  DdLevelQueue * queue)
{
    DdQueueItem *item;

    while (queue->freelist != NULL) {
	item = queue->freelist;
	queue->freelist = item->next;
	FREE(item);
    }
    while (queue->first != NULL) {
	item = (DdQueueItem *) queue->first;
	queue->first = item->next;
	FREE(item);
    }
    FREE(queue->buckets);
    FREE(queue->last);
    FREE(queue);
    return;

} /* end of cuddLevelQueueQuit */


/**
  @brief Inserts a new key in a level queue.

  @details A new entry is created in the queue only if the node is not
  already enqueued.

  @return a pointer to the queue item if successful; NULL otherwise.

  @sideeffect None

  @see cuddLevelQueueInit cuddLevelQueueDequeue

*/
void *
cuddLevelQueueEnqueue(
  DdLevelQueue * queue /**< level queue */,
  void * key /**< key to be enqueued */,
  int  level /**< level at which to insert */)
{
    DdQueueItem *item;

#ifdef DD_DEBUG
    assert(level < queue->levels);
#endif
    /* Check whether entry for this node exists. */
    item = hashLookup(queue,key);
    if (item != NULL) return(item);

    /* Get a free item from either the free list or the memory manager. */
    if (queue->freelist == NULL) {
	item = (DdQueueItem *) ALLOC(char, queue->itemsize);
	if (item == NULL)
	    return(NULL);
    } else {
	item = queue->freelist;
	queue->freelist = item->next;
    }
    /* Initialize. */
    memset(item, 0, queue->itemsize);
    item->key = key;
    /* Update stats. */
    queue->size++;

    if (queue->last[level]) {
	/* There are already items for this level in the queue. */
	item->next = queue->last[level]->next;
	queue->last[level]->next = item;
    } else {
	/* There are no items at the current level.  Look for the first
	** non-empty level preceeding this one. */
        int plevel = level;
	while (plevel != 0 && queue->last[plevel] == NULL)
	    plevel--;
	if (queue->last[plevel] == NULL) {
	    /* No element precedes this one in the queue. */
	    item->next = (DdQueueItem *) queue->first;
	    queue->first = item;
	} else {
	    item->next = queue->last[plevel]->next;
	    queue->last[plevel]->next = item;
	}
    }
    queue->last[level] = item;

    /* Insert entry for the key in the hash table. */
    if (hashInsert(queue,item) == 0) {
	return(NULL);
    }
    return(item);

} /* end of cuddLevelQueueEnqueue */


/**
  @brief Inserts the first key in a level queue.

  @return a pointer to the queue item if successful; NULL otherwise.

  @sideeffect None

  @see cuddLevelQueueEnqueue

*/
void *
cuddLevelQueueFirst(
  DdLevelQueue * queue /**< level queue */,
  void * key /**< key to be enqueued */,
  int  level /**< level at which to insert */)
{
    DdQueueItem *item;

#ifdef DD_DEBUG
    assert(level < queue->levels);
    /* Check whether entry for this node exists. */
    item = hashLookup(queue,key);
    assert(item == NULL);
#endif

    /* Get a free item from either the free list or the memory manager. */
    if (queue->freelist == NULL) {
	item = (DdQueueItem *) ALLOC(char, queue->itemsize);
	if (item == NULL)
	    return(NULL);
    } else {
	item = queue->freelist;
	queue->freelist = item->next;
    }
    /* Initialize. */
    memset(item, 0, queue->itemsize);
    item->key = key;
    /* Update stats. */
    queue->size = 1;

    /* No element precedes this one in the queue. */
    queue->first = item;
    queue->last[level] = item;

    /* Insert entry for the key in the hash table. */
    if (hashInsert(queue,item) == 0) {
	return(NULL);
    }
    return(item);

} /* end of cuddLevelQueueFirst */


/**
  @brief Remove an item from the front of a level queue.

  @sideeffect None

  @see cuddLevelQueueEnqueue

*/
void
cuddLevelQueueDequeue(
  DdLevelQueue * queue,
  int  level)
{
    DdQueueItem *item = (DdQueueItem *) queue->first;

    /* Delete from the hash table. */
    hashDelete(queue,item);

    /* Since we delete from the front, if this is the last item for
    ** its level, there are no other items for the same level. */
    if (queue->last[level] == item) {
	queue->last[level] = NULL;
    }

    queue->first = item->next;
    /* Put item on the free list. */
    item->next = queue->freelist;
    queue->freelist = item;
    /* Update stats. */
    queue->size--;
    return;

} /* end of cuddLevelQueueDequeue */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Looks up a key in the hash table of a level queue.

  @return a pointer to the item with the given key if the key is
  found; NULL otherwise.

  @sideeffect None

  @see cuddLevelQueueEnqueue hashInsert

*/
static DdQueueItem *
hashLookup(
  DdLevelQueue * queue,
  void * key)
{
    int posn;
    DdQueueItem *item;

    posn = lqHash(key,queue->shift);
    item = queue->buckets[posn];

    while (item != NULL) {
	if (item->key == key) {
	    return(item);
	}
	item = item->cnext;
    }
    return(NULL);

} /* end of hashLookup */


/**
  @brief Inserts an item in the hash table of a level queue.

  @details No check is performed to see if an item with the same key
  is already in the hash table.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see cuddLevelQueueEnqueue

*/
static int
hashInsert(
  DdLevelQueue * queue,
  DdQueueItem * item)
{
    int result;
    int posn;

    if (queue->size > queue->maxsize) {
	result = hashResize(queue);
	if (result == 0) return(0);
    }

    posn = lqHash(item->key,queue->shift);
    item->cnext = queue->buckets[posn];
    queue->buckets[posn] = item;

    return(1);
    
} /* end of hashInsert */


/**
  @brief Removes an item from the hash table of a level queue.

  @details Nothing is done if the item is not in the table.

  @sideeffect None

  @see cuddLevelQueueDequeue hashInsert

*/
static void
hashDelete(
  DdLevelQueue * queue,
  DdQueueItem * item)
{
    int posn;
    DdQueueItem *prevItem;

    posn = lqHash(item->key,queue->shift);
    prevItem = queue->buckets[posn];

    if (prevItem == NULL) return;
    if (prevItem == item) {
	queue->buckets[posn] = prevItem->cnext;
	return;
    }

    while (prevItem->cnext != NULL) {
	if (prevItem->cnext == item) {
	    prevItem->cnext = item->cnext;
	    return;
	}
	prevItem = prevItem->cnext;
    }
    return;

} /* end of hashDelete */


/**
  @brief Resizes the hash table of a level queue.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see hashInsert

*/
static int
hashResize(
  DdLevelQueue * queue)
{
    int j;
    int posn;
    DdQueueItem *item;
    DdQueueItem *next;
    int numBuckets;
    DdQueueItem **buckets;
    DdQueueItem **oldBuckets = queue->buckets;
    int shift;
    int oldNumBuckets = queue->numBuckets;
    extern DD_OOMFP MMoutOfMemory;
    DD_OOMFP saveHandler;

    /* Compute the new size of the subtable. */
    numBuckets = oldNumBuckets << 1;
    saveHandler = MMoutOfMemory;
    MMoutOfMemory = queue->manager->outOfMemCallback;
    buckets = queue->buckets = ALLOC(DdQueueItem *, numBuckets);
    MMoutOfMemory = saveHandler;
    if (buckets == NULL) {
	queue->maxsize <<= 1;
	return(1);
    }

    queue->numBuckets = numBuckets;
    shift = --(queue->shift);
    queue->maxsize <<= 1;
    memset(buckets, 0, numBuckets * sizeof(DdQueueItem *));
    for (j = 0; j < oldNumBuckets; j++) {
	item = oldBuckets[j];
	while (item != NULL) {
	    next = item->cnext;
	    posn = lqHash(item->key, shift);
	    item->cnext = buckets[posn];
	    buckets[posn] = item;
	    item = next;
	}
    }
    FREE(oldBuckets);
    return(1);

} /* end of hashResize */
