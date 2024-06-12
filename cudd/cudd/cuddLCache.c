/**
  @file

  @ingroup cudd

  @brief Functions for local caches.

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

#define DD_MAX_HASHTABLE_DENSITY 2	/* tells when to resize a table */

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
  @brief Computes hash function for keys of one operand.

  @sideeffect None

  @see ddLCHash3 ddLCHash

*/
#if SIZEOF_VOID_P == 8 && SIZEOF_INT == 4
#define ddLCHash1(f,shift) \
(((unsigned)(ptruint)(f) * DD_P1) >> (shift))
#else
#define ddLCHash1(f,shift) \
(((unsigned)(f) * DD_P1) >> (shift))
#endif


/**
  @brief Computes hash function for keys of two operands.

  @sideeffect None

  @see ddLCHash3 ddLCHash

*/
#if SIZEOF_VOID_P == 8 && SIZEOF_INT == 4
#define ddLCHash2(f,g,shift) \
((((unsigned)(ptruint)(f) * DD_P1 + \
   (unsigned)(ptruint)(g)) * DD_P2) >> (shift))
#else
#define ddLCHash2(f,g,shift) \
((((unsigned)(f) * DD_P1 + (unsigned)(g)) * DD_P2) >> (shift))
#endif


/**
  @brief Computes hash function for keys of three operands.

  @sideeffect None

  @see ddLCHash2 ddLCHash

*/
#define ddLCHash3(f,g,h,shift) ddCHash2(f,g,h,shift)


/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static void cuddLocalCacheResize (DdLocalCache *cache);
static unsigned int ddLCHash (DdNodePtr *key, unsigned int keysize, int shift);
static void cuddLocalCacheAddToList (DdLocalCache *cache);
static void cuddLocalCacheRemoveFromList (DdLocalCache *cache);
static int cuddHashTableResize (DdHashTable *hash);
static DdHashItem * cuddHashTableAlloc (DdHashTable *hash);

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Initializes a local computed table.

  @return a pointer the the new local cache in case of success; NULL
  otherwise.

  @sideeffect None

  @see cuddInitCache

*/
DdLocalCache *
cuddLocalCacheInit(
  DdManager * manager /**< manager */,
  unsigned int  keySize /**< size of the key (number of operands) */,
  unsigned int  cacheSize /**< Initial size of the cache */,
  unsigned int  maxCacheSize /**< Size of the cache beyond which no resizing occurs */)
{
    DdLocalCache *cache;
    int logSize;

    cache = ALLOC(DdLocalCache,1);
    if (cache == NULL) {
	manager->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }
    cache->manager = manager;
    cache->keysize = keySize;
    cache->itemsize = (keySize + 1) * sizeof(DdNode *);
#ifdef DD_CACHE_PROFILE
    cache->itemsize += sizeof(ptrint);
#endif
    logSize = cuddComputeFloorLog2(ddMax(cacheSize,manager->slots/2));
    cacheSize = 1U << logSize;
    cache->item = (DdLocalCacheItem *)
	ALLOC(char, cacheSize * cache->itemsize);
    if (cache->item == NULL) {
	manager->errorCode = CUDD_MEMORY_OUT;
	FREE(cache);
	return(NULL);
    }
    cache->slots = cacheSize;
    cache->shift = sizeof(int) * 8 - logSize;
    cache->maxslots = ddMin(maxCacheSize,manager->slots);
    cache->minHit = manager->minHit;
    /* Initialize to avoid division by 0 and immediate resizing. */
    cache->lookUps = (double) (int) (cacheSize * cache->minHit + 1);
    cache->hits = 0;
    manager->memused += cacheSize * cache->itemsize + sizeof(DdLocalCache);

    /* Initialize the cache. */
    memset(cache->item, 0, cacheSize * cache->itemsize);

    /* Add to manager's list of local caches for GC. */
    cuddLocalCacheAddToList(cache);

    return(cache);

} /* end of cuddLocalCacheInit */


/**
  @brief Shuts down a local computed table.

  @sideeffect None

  @see cuddLocalCacheInit

*/
void
cuddLocalCacheQuit(
  DdLocalCache * cache /**< cache to be shut down */)
{
    cache->manager->memused -=
	cache->slots * cache->itemsize + sizeof(DdLocalCache);
    cuddLocalCacheRemoveFromList(cache);
    FREE(cache->item);
    FREE(cache);

    return;

} /* end of cuddLocalCacheQuit */


/**
  @brief Inserts a result in a local cache.

  @sideeffect None

*/
void
cuddLocalCacheInsert(
  DdLocalCache * cache,
  DdNodePtr * key,
  DdNode * value)
{
    unsigned int posn;
    DdLocalCacheItem *entry;

    posn = ddLCHash(key,cache->keysize,cache->shift);
    entry = (DdLocalCacheItem *) ((char *) cache->item +
				  posn * cache->itemsize);
    memcpy(entry->key,key,cache->keysize * sizeof(DdNode *));
    entry->value = value;
#ifdef DD_CACHE_PROFILE
    entry->count++;
#endif

} /* end of cuddLocalCacheInsert */


/**
  @brief Looks up in a local cache.

  @return the result if found; it returns NULL if no result is found.

  @sideeffect None

*/
DdNode *
cuddLocalCacheLookup(
  DdLocalCache * cache,
  DdNodePtr * key)
{
    unsigned int posn;
    DdLocalCacheItem *entry;
    DdNode *value;

    cache->lookUps++;
    posn = ddLCHash(key,cache->keysize,cache->shift);
    entry = (DdLocalCacheItem *) ((char *) cache->item +
				  posn * cache->itemsize);
    if (entry->value != NULL &&
	memcmp(key,entry->key,cache->keysize*sizeof(DdNode *)) == 0) {
	cache->hits++;
	value = Cudd_Regular(entry->value);
	if (value->ref == 0) {
	    cuddReclaim(cache->manager,value);
	}
	return(entry->value);
    }

    /* Cache miss: decide whether to resize */

    if (cache->slots < cache->maxslots &&
	cache->hits > cache->lookUps * cache->minHit) {
	cuddLocalCacheResize(cache);
    }

    return(NULL);

} /* end of cuddLocalCacheLookup */


/**
  @brief Clears the dead entries of the local caches of a manager.

  @details Used during garbage collection.

  @sideeffect None

*/
void
cuddLocalCacheClearDead(
  DdManager * manager)
{
    DdLocalCache *cache = manager->localCaches;
    unsigned int keysize;
    unsigned int itemsize;
    unsigned int slots;
    DdLocalCacheItem *item;
    DdNodePtr *key;
    unsigned int i, j;

    while (cache != NULL) {
	keysize = cache->keysize;
	itemsize = cache->itemsize;
	slots = cache->slots;
	item = cache->item;
	for (i = 0; i < slots; i++) {
	    if (item->value != NULL) {
		if (Cudd_Regular(item->value)->ref == 0) {
		    item->value = NULL;
		} else {
		    key = item->key;
		    for (j = 0; j < keysize; j++) {
			if (Cudd_Regular(key[j])->ref == 0) {
			    item->value = NULL;
			    break;
			}
		    }
		}
	    }
	    item = (DdLocalCacheItem *) ((char *) item + itemsize);
	}
	cache = cache->next;
    }
    return;

} /* end of cuddLocalCacheClearDead */


/**
  @brief Clears the local caches of a manager.

  @details Used before reordering.

  @sideeffect None

*/
void
cuddLocalCacheClearAll(
  DdManager * manager)
{
    DdLocalCache *cache = manager->localCaches;

    while (cache != NULL) {
	memset(cache->item, 0, cache->slots * cache->itemsize);
	cache = cache->next;
    }
    return;

} /* end of cuddLocalCacheClearAll */


#ifdef DD_CACHE_PROFILE

#define DD_HYSTO_BINS 8

/**
  @brief Computes and prints a profile of a local cache usage.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

*/
int
cuddLocalCacheProfile(
  DdLocalCache * cache)
{
    double count, mean, meansq, stddev, expected;
    long max, min;
    int imax, imin;
    int i, retval, slots;
    long *hystogram;
    int nbins = DD_HYSTO_BINS;
    int bin;
    long thiscount;
    double totalcount;
    int nzeroes;
    DdLocalCacheItem *entry;
    FILE *fp = cache->manager->out;

    slots = cache->slots;

    meansq = mean = expected = 0.0;
    max = min = (long) cache->item[0].count;
    imax = imin = nzeroes = 0;
    totalcount = 0.0;

    hystogram = ALLOC(long, nbins);
    if (hystogram == NULL) {
	return(0);
    }
    for (i = 0; i < nbins; i++) {
	hystogram[i] = 0;
    }

    for (i = 0; i < slots; i++) {
	entry = (DdLocalCacheItem *) ((char *) cache->item +
				      i * cache->itemsize);
	thiscount = (long) entry->count;
	if (thiscount > max) {
	    max = thiscount;
	    imax = i;
	}
	if (thiscount < min) {
	    min = thiscount;
	    imin = i;
	}
	if (thiscount == 0) {
	    nzeroes++;
	}
	count = (double) thiscount;
	mean += count;
	meansq += count * count;
	totalcount += count;
	expected += count * (double) i;
	bin = (i * nbins) / slots;
	hystogram[bin] += thiscount;
    }
    mean /= (double) slots;
    meansq /= (double) slots;
    stddev = sqrt(meansq - mean*mean);

    retval = fprintf(fp,"Cache stats: slots = %d average = %g ", slots, mean);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"standard deviation = %g\n", stddev);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Cache max accesses = %ld for slot %d\n", max, imax);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Cache min accesses = %ld for slot %d\n", min, imin);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Cache unused slots = %d\n", nzeroes);
    if (retval == EOF) return(0);

    if (totalcount) {
	expected /= totalcount;
	retval = fprintf(fp,"Cache access hystogram for %d bins", nbins);
	if (retval == EOF) return(0);
	retval = fprintf(fp," (expected bin value = %g)\n# ", expected);
	if (retval == EOF) return(0);
	for (i = nbins - 1; i>=0; i--) {
	    retval = fprintf(fp,"%ld ", hystogram[i]);
	    if (retval == EOF) return(0);
	}
	retval = fprintf(fp,"\n");
	if (retval == EOF) return(0);
    }

    FREE(hystogram);
    return(1);

} /* end of cuddLocalCacheProfile */
#endif


/**
  @brief Initializes a hash table.

  @details The table associates tuples of DdNode pointers to one DdNode pointer.
  This type of table is used for functions that cannot (or prefer not to) use
  the main computed table.  The package also provides functions that allow the
  caller to store arbitrary pointers in the table.

  @return a pointer to the new table if successful; NULL otherwise.

  @sideeffect None

  @see cuddHashTableQuit cuddHashTableGenericQuit

*/
DdHashTable *
cuddHashTableInit(
  DdManager * manager /**< %DD manager */,
  unsigned int keySize /**< number of pointers in the key */,
  unsigned int initSize /**< initial size of the table */)
{
    DdHashTable *hash;
    int logSize;

    hash = ALLOC(DdHashTable, 1);
    if (hash == NULL) {
	manager->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }
    hash->keysize = keySize;
    hash->manager = manager;
    hash->memoryList = NULL;
    hash->nextFree = NULL;
    hash->itemsize = (keySize + 1) * sizeof(DdNode *) +
	sizeof(ptrint) + sizeof(DdHashItem *);
    /* We have to guarantee that the shift be < 32. */
    if (initSize < 2) initSize = 2;
    logSize = cuddComputeFloorLog2(initSize);
    hash->numBuckets = 1U << logSize;
    hash->shift = sizeof(int) * 8 - logSize;
    hash->bucket = ALLOC(DdHashItem *, hash->numBuckets);
    if (hash->bucket == NULL) {
	manager->errorCode = CUDD_MEMORY_OUT;
	FREE(hash);
	return(NULL);
    }
    memset(hash->bucket, 0, hash->numBuckets * sizeof(DdHashItem *));
    hash->size = 0;
    hash->maxsize = hash->numBuckets * DD_MAX_HASHTABLE_DENSITY;
    return(hash);

} /* end of cuddHashTableInit */


/**
  @brief Shuts down a hash table.

  @details Dereferences all the values.

  @sideeffect None

  @see cuddHashTableInit

*/
void
cuddHashTableQuit(
  DdHashTable * hash)
{
    unsigned int i;
    DdManager *dd = hash->manager;
    DdHashItem *bucket;
    DdHashItem **memlist, **nextmem;
    unsigned int numBuckets = hash->numBuckets;

    for (i = 0; i < numBuckets; i++) {
	bucket = hash->bucket[i];
	while (bucket != NULL) {
	    Cudd_RecursiveDeref(dd, bucket->value);
	    bucket = bucket->next;
	}
    }

    memlist = hash->memoryList;
    while (memlist != NULL) {
	nextmem = (DdHashItem **) memlist[0];
	FREE(memlist);
	memlist = nextmem;
    }

    FREE(hash->bucket);
    FREE(hash);

    return;

} /* end of cuddHashTableQuit */


/**
  @brief Shuts down a hash table.

  @details Shuts down a hash table, when the values are not DdNode
  pointers.

  @sideeffect None

  @see cuddHashTableInit

*/
void
cuddHashTableGenericQuit(
  DdHashTable * hash)
{
    DdHashItem **memlist, **nextmem;

    memlist = hash->memoryList;
    while (memlist != NULL) {
	nextmem = (DdHashItem **) memlist[0];
	FREE(memlist);
	memlist = nextmem;
    }

    FREE(hash->bucket);
    FREE(hash);

    return;

} /* end of cuddHashTableGenericQuit */


/**
  @brief Inserts an item in a hash table.

  @details Inserts an item in a hash table when the key has more than
  three pointers.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see [cuddHashTableInsert1 cuddHashTableInsert2 cuddHashTableInsert3
  cuddHashTableLookup

*/
int
cuddHashTableInsert(
  DdHashTable * hash,
  DdNodePtr * key,
  DdNode * value,
  ptrint count)
{
    int result;
    unsigned int posn;
    DdHashItem *item;
    unsigned int i;

#ifdef DD_DEBUG
    assert(hash->keysize > 3);
#endif

    if (hash->size > hash->maxsize) {
	result = cuddHashTableResize(hash);
	if (result == 0) return(0);
    }
    item = cuddHashTableAlloc(hash);
    if (item == NULL) return(0);
    hash->size++;
    item->value = value;
    cuddRef(value);
    item->count = count;
    for (i = 0; i < hash->keysize; i++) {
	item->key[i] = key[i];
    }
    posn = ddLCHash(key,hash->keysize,hash->shift);
    item->next = hash->bucket[posn];
    hash->bucket[posn] = item;

    return(1);

} /* end of cuddHashTableInsert */


/**
  @brief Looks up a key in a hash table.

  @details Looks up a key consisting of more than three pointers in a
  hash table.  If the entry is present, its reference counter is
  decremented if not saturated. If the counter reaches 0, the value of
  the entry is dereferenced, and the entry is returned to the free
  list.

  @return the value associated to the key if there is an entry for the
  given key in the table; NULL otherwise.

  @sideeffect None

  @see cuddHashTableLookup1 cuddHashTableLookup2 cuddHashTableLookup3
  cuddHashTableInsert

*/
DdNode *
cuddHashTableLookup(
  DdHashTable * hash,
  DdNodePtr * key)
{
    unsigned int posn;
    DdHashItem *item, *prev;
    unsigned int i, keysize;

#ifdef DD_DEBUG
    assert(hash->keysize > 3);
#endif

    posn = ddLCHash(key,hash->keysize,hash->shift);
    item = hash->bucket[posn];
    prev = NULL;

    keysize = hash->keysize;
    while (item != NULL) {
	DdNodePtr *key2 = item->key;
	int equal = 1;
	for (i = 0; i < keysize; i++) {
	    if (key[i] != key2[i]) {
		equal = 0;
		break;
	    }
	}
	if (equal) {
	    DdNode *value = item->value;
	    cuddSatDec(item->count);
	    if (item->count == 0) {
		cuddDeref(value);
		if (prev == NULL) {
		    hash->bucket[posn] = item->next;
		} else {
		    prev->next = item->next;
		}
		item->next = hash->nextFree;
		hash->nextFree = item;
		hash->size--;
	    }
	    return(value);
	}
	prev = item;
	item = item->next;
    }
    return(NULL);

} /* end of cuddHashTableLookup */


/**
  @brief Inserts an item in a hash table.

  @details Inserts an item in a hash table when the key is one pointer.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see cuddHashTableInsert cuddHashTableInsert2 cuddHashTableInsert3
  cuddHashTableLookup1

*/
int
cuddHashTableInsert1(
  DdHashTable * hash,
  DdNode * f,
  DdNode * value,
  ptrint count)
{
    int result;
    unsigned int posn;
    DdHashItem *item;

#ifdef DD_DEBUG
    assert(hash->keysize == 1);
#endif

    if (hash->size > hash->maxsize) {
	result = cuddHashTableResize(hash);
	if (result == 0) return(0);
    }
    item = cuddHashTableAlloc(hash);
    if (item == NULL) return(0);
    hash->size++;
    item->value = value;
    cuddRef(value);
    item->count = count;
    item->key[0] = f;
    posn = ddLCHash1(f,hash->shift);
    item->next = hash->bucket[posn];
    hash->bucket[posn] = item;

    return(1);

} /* end of cuddHashTableInsert1 */


/**
  @brief Looks up a key consisting of one pointer in a hash table.

  @details If the entry is present, its reference count is
  decremented if not saturated. If the counter reaches 0, the value of
  the entry is dereferenced, and the entry is returned to the free
  list.

  @return the value associated to the key if there is an entry for the
  given key in the table; NULL otherwise.

  @sideeffect None

  @see cuddHashTableLookup cuddHashTableLookup2 cuddHashTableLookup3
  cuddHashTableInsert1

*/
DdNode *
cuddHashTableLookup1(
  DdHashTable * hash,
  DdNode * f)
{
    unsigned int posn;
    DdHashItem *item, *prev;

#ifdef DD_DEBUG
    assert(hash->keysize == 1);
#endif

    posn = ddLCHash1(f,hash->shift);
    item = hash->bucket[posn];
    prev = NULL;

    while (item != NULL) {
	DdNodePtr *key = item->key;
	if (f == key[0]) {
	    DdNode *value = item->value;
	    cuddSatDec(item->count);
	    if (item->count == 0) {
		cuddDeref(value);
		if (prev == NULL) {
		    hash->bucket[posn] = item->next;
		} else {
		    prev->next = item->next;
		}
		item->next = hash->nextFree;
		hash->nextFree = item;
		hash->size--;
	    }
	    return(value);
	}
	prev = item;
	item = item->next;
    }
    return(NULL);

} /* end of cuddHashTableLookup1 */


/**
  @brief Inserts a generic item in a hash table.

  @details Inserts an item in a hash table when the key is one
  pointer and the value is not a DdNode pointer.  The main difference w.r.t.
  cuddHashTableInsert1 is that the reference count of the value is not
  incremented.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see cuddHashTableInsert1 cuddHashTableGenericLookup

*/
int
cuddHashTableGenericInsert(
  DdHashTable * hash,
  DdNode * f,
  void * value)
{
    int result;
    unsigned int posn;
    DdHashItem *item;

#ifdef DD_DEBUG
    assert(hash->keysize == 1);
#endif

    if (hash->size > hash->maxsize) {
	result = cuddHashTableResize(hash);
	if (result == 0) return(0);
    }
    item = cuddHashTableAlloc(hash);
    if (item == NULL) return(0);
    hash->size++;
    item->value = (DdNode *) value;
    item->count = 0;
    item->key[0] = f;
    posn = ddLCHash1(f,hash->shift);
    item->next = hash->bucket[posn];
    hash->bucket[posn] = item;

    return(1);

} /* end of cuddHashTableGenericInsert */


/**
  @brief Looks up a key consisting of one pointer in a hash table.

  @details Looks up a key consisting of one pointer in a hash
  table when the value is not a DdNode pointer.

  @return the value associated to the key if there is an entry for the
  given key in the table; NULL otherwise.

  @sideeffect None

  @see cuddHashTableLookup1 cuddHashTableGenericInsert

*/
void *
cuddHashTableGenericLookup(
  DdHashTable * hash,
  DdNode * f)
{
    unsigned int posn;
    DdHashItem *item;

#ifdef DD_DEBUG
    assert(hash->keysize == 1);
#endif

    posn = ddLCHash1(f,hash->shift);
    item = hash->bucket[posn];

    while (item != NULL) {
	if (f == item->key[0]) {
            return ((void *) item->value);
	}
	item = item->next;
    }
    return(NULL);

} /* end of cuddHashTableGenericLookup */


/**
  @brief Inserts an item in a hash table.

  @details Inserts an item in a hash table when the key is
  composed of two pointers.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see cuddHashTableInsert cuddHashTableInsert1 cuddHashTableInsert3
  cuddHashTableLookup2

*/
int
cuddHashTableInsert2(
  DdHashTable * hash,
  DdNode * f,
  DdNode * g,
  DdNode * value,
  ptrint count)
{
    int result;
    unsigned int posn;
    DdHashItem *item;

#ifdef DD_DEBUG
    assert(hash->keysize == 2);
#endif

    if (hash->size > hash->maxsize) {
	result = cuddHashTableResize(hash);
	if (result == 0) return(0);
    }
    item = cuddHashTableAlloc(hash);
    if (item == NULL) return(0);
    hash->size++;
    item->value = value;
    cuddRef(value);
    item->count = count;
    item->key[0] = f;
    item->key[1] = g;
    posn = ddLCHash2(f,g,hash->shift);
    item->next = hash->bucket[posn];
    hash->bucket[posn] = item;

    return(1);

} /* end of cuddHashTableInsert2 */


/**
  @brief Looks up a key consisting of two pointers in a hash table.

  @details If the entry is present, its reference counter is
  decremented if not saturated. If the counter reaches 0, the value of
  the entry is dereferenced, and the entry is returned to the free
  list.

  @return the value associated to the key if there is an entry for the
  given key in the table; NULL otherwise.

  @sideeffect None

  @see cuddHashTableLookup cuddHashTableLookup1 cuddHashTableLookup3
  cuddHashTableInsert2

*/
DdNode *
cuddHashTableLookup2(
  DdHashTable * hash,
  DdNode * f,
  DdNode * g)
{
    unsigned int posn;
    DdHashItem *item, *prev;

#ifdef DD_DEBUG
    assert(hash->keysize == 2);
#endif

    posn = ddLCHash2(f,g,hash->shift);
    item = hash->bucket[posn];
    prev = NULL;

    while (item != NULL) {
	DdNodePtr *key = item->key;
	if ((f == key[0]) && (g == key[1])) {
	    DdNode *value = item->value;
	    cuddSatDec(item->count);
	    if (item->count == 0) {
		cuddDeref(value);
		if (prev == NULL) {
		    hash->bucket[posn] = item->next;
		} else {
		    prev->next = item->next;
		}
		item->next = hash->nextFree;
		hash->nextFree = item;
		hash->size--;
	    }
	    return(value);
	}
	prev = item;
	item = item->next;
    }
    return(NULL);

} /* end of cuddHashTableLookup2 */


/**
  @brief Inserts an item in a hash table.

  @details Inserts an item in a hash table when the key is
  composed of three pointers.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see cuddHashTableInsert cuddHashTableInsert1 cuddHashTableInsert2
  cuddHashTableLookup3

*/
int
cuddHashTableInsert3(
  DdHashTable * hash,
  DdNode * f,
  DdNode * g,
  DdNode * h,
  DdNode * value,
  ptrint count)
{
    int result;
    unsigned int posn;
    DdHashItem *item;

#ifdef DD_DEBUG
    assert(hash->keysize == 3);
#endif

    if (hash->size > hash->maxsize) {
	result = cuddHashTableResize(hash);
	if (result == 0) return(0);
    }
    item = cuddHashTableAlloc(hash);
    if (item == NULL) return(0);
    hash->size++;
    item->value = value;
    cuddRef(value);
    item->count = count;
    item->key[0] = f;
    item->key[1] = g;
    item->key[2] = h;
    posn = ddLCHash3(f,g,h,hash->shift);
    item->next = hash->bucket[posn];
    hash->bucket[posn] = item;

    return(1);

} /* end of cuddHashTableInsert3 */


/**
  @brief Looks up a key consisting of three pointers in a hash table.

  @details If the entry is present, its reference counter is
  decremented if not saturated. If the counter reaches 0, the value of
  the entry is dereferenced, and the entry is returned to the free
  list.

  @return the value associated to the key if there is an entry for the
  given key in the table; NULL otherwise.

  @sideeffect None

  @see cuddHashTableLookup cuddHashTableLookup1 cuddHashTableLookup2
  cuddHashTableInsert3

*/
DdNode *
cuddHashTableLookup3(
  DdHashTable * hash,
  DdNode * f,
  DdNode * g,
  DdNode * h)
{
    unsigned int posn;
    DdHashItem *item, *prev;

#ifdef DD_DEBUG
    assert(hash->keysize == 3);
#endif

    posn = ddLCHash3(f,g,h,hash->shift);
    item = hash->bucket[posn];
    prev = NULL;

    while (item != NULL) {
	DdNodePtr *key = item->key;
	if ((f == key[0]) && (g == key[1]) && (h == key[2])) {
	    DdNode *value = item->value;
	    cuddSatDec(item->count);
	    if (item->count == 0) {
		cuddDeref(value);
		if (prev == NULL) {
		    hash->bucket[posn] = item->next;
		} else {
		    prev->next = item->next;
		}
		item->next = hash->nextFree;
		hash->nextFree = item;
		hash->size--;
	    }
	    return(value);
	}
	prev = item;
	item = item->next;
    }
    return(NULL);

} /* end of cuddHashTableLookup3 */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Resizes a local cache.

  @sideeffect None

*/
static void
cuddLocalCacheResize(
  DdLocalCache * cache)
{
    DdLocalCacheItem *item, *olditem, *entry, *old;
    int i, shift;
    unsigned int posn;
    unsigned int slots, oldslots;
    extern DD_OOMFP MMoutOfMemory;
    DD_OOMFP saveHandler;

    olditem = cache->item;
    oldslots = cache->slots;
    slots = cache->slots = oldslots << 1;

#ifdef DD_VERBOSE
    (void) fprintf(cache->manager->err,
		   "Resizing local cache from %d to %d entries\n",
		   oldslots, slots);
    (void) fprintf(cache->manager->err,
		   "\thits = %.0f\tlookups = %.0f\thit ratio = %5.3f\n",
		   cache->hits, cache->lookUps, cache->hits / cache->lookUps);
#endif

    saveHandler = MMoutOfMemory;
    MMoutOfMemory = cache->manager->outOfMemCallback;
    cache->item = item =
	(DdLocalCacheItem *) ALLOC(char, slots * cache->itemsize);
    MMoutOfMemory = saveHandler;
    /* If we fail to allocate the new table we just give up. */
    if (item == NULL) {
#ifdef DD_VERBOSE
	(void) fprintf(cache->manager->err,"Resizing failed. Giving up.\n");
#endif
	cache->slots = oldslots;
	cache->item = olditem;
	/* Do not try to resize again. */
	cache->maxslots = oldslots - 1;
	return;
    }
    shift = --(cache->shift);
    cache->manager->memused += (slots - oldslots) * cache->itemsize;

    /* Clear new cache. */
    memset(item, 0, slots * cache->itemsize);

    /* Copy from old cache to new one. */
    for (i = 0; (unsigned) i < oldslots; i++) {
	old = (DdLocalCacheItem *) ((char *) olditem + i * cache->itemsize);
	if (old->value != NULL) {
	    posn = ddLCHash(old->key,cache->keysize,shift);
	    entry = (DdLocalCacheItem *) ((char *) item +
					  posn * cache->itemsize);
	    memcpy(entry->key,old->key,cache->keysize*sizeof(DdNode *));
	    entry->value = old->value;
	}
    }

    FREE(olditem);

    /* Reinitialize measurements so as to avoid division by 0 and
    ** immediate resizing.
    */
    cache->lookUps = (double) (int) (slots * cache->minHit + 1);
    cache->hits = 0;

} /* end of cuddLocalCacheResize */


/**
  @brief Computes the hash value for a local cache.

  @return the bucket index.

  @sideeffect None

*/
static unsigned int
ddLCHash(
  DdNodePtr * key,
  unsigned int keysize,
  int shift)
{
    unsigned int val = (unsigned int) (ptrint) key[0] * DD_P2;
    unsigned int i;

    for (i = 1; i < keysize; i++) {
	val = val * DD_P1 + (int) (ptrint) key[i];
    }

    return(val >> shift);

} /* end of ddLCHash */


/**
  @brief Inserts a local cache in the manager list.

  @sideeffect None

*/
static void
cuddLocalCacheAddToList(
  DdLocalCache * cache)
{
    DdManager *manager = cache->manager;

    cache->next = manager->localCaches;
    manager->localCaches = cache;
    return;

} /* end of cuddLocalCacheAddToList */


/**
  @brief Removes a local cache from the manager list.

  @sideeffect None

*/
static void
cuddLocalCacheRemoveFromList(
  DdLocalCache * cache)
{
    DdManager *manager = cache->manager;
    DdLocalCache **prevCache, *nextCache;

    prevCache = &(manager->localCaches);
    nextCache = manager->localCaches;

    while (nextCache != NULL) {
	if (nextCache == cache) {
	    *prevCache = nextCache->next;
	    return;
	}
	prevCache = &(nextCache->next);
	nextCache = nextCache->next;
    }
    return;			/* should never get here */

} /* end of cuddLocalCacheRemoveFromList */


/**
  @brief Resizes a hash table.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see cuddHashTableInsert

*/
static int
cuddHashTableResize(
  DdHashTable * hash)
{
    int j;
    unsigned int posn;
    DdHashItem *item;
    DdHashItem *next;
    DdNode **key;
    int numBuckets;
    DdHashItem **buckets;
    DdHashItem **oldBuckets = hash->bucket;
    int shift;
    int oldNumBuckets = hash->numBuckets;
    extern DD_OOMFP MMoutOfMemory;
    DD_OOMFP saveHandler;

    /* Compute the new size of the table. */
    numBuckets = oldNumBuckets << 1;
    saveHandler = MMoutOfMemory;
    MMoutOfMemory = hash->manager->outOfMemCallback;
    buckets = ALLOC(DdHashItem *, numBuckets);
    MMoutOfMemory = saveHandler;
    if (buckets == NULL) {
	hash->maxsize <<= 1;
	return(1);
    }

    hash->bucket = buckets;
    hash->numBuckets = numBuckets;
    shift = --(hash->shift);
    hash->maxsize <<= 1;
    memset(buckets, 0, numBuckets * sizeof(DdHashItem *));
    if (hash->keysize == 1) {
	for (j = 0; j < oldNumBuckets; j++) {
	    item = oldBuckets[j];
	    while (item != NULL) {
		next = item->next;
		key = item->key;
		posn = ddLCHash2(key[0], key[0], shift);
		item->next = buckets[posn];
		buckets[posn] = item;
		item = next;
	    }
	}
    } else if (hash->keysize == 2) {
	for (j = 0; j < oldNumBuckets; j++) {
	    item = oldBuckets[j];
	    while (item != NULL) {
		next = item->next;
		key = item->key;
		posn = ddLCHash2(key[0], key[1], shift);
		item->next = buckets[posn];
		buckets[posn] = item;
		item = next;
	    }
	}
    } else if (hash->keysize == 3) {
	for (j = 0; j < oldNumBuckets; j++) {
	    item = oldBuckets[j];
	    while (item != NULL) {
		next = item->next;
		key = item->key;
		posn = ddLCHash3(key[0], key[1], key[2], shift);
		item->next = buckets[posn];
		buckets[posn] = item;
		item = next;
	    }
	}
    } else {
	for (j = 0; j < oldNumBuckets; j++) {
	    item = oldBuckets[j];
	    while (item != NULL) {
		next = item->next;
		posn = ddLCHash(item->key, hash->keysize, shift);
		item->next = buckets[posn];
		buckets[posn] = item;
		item = next;
	    }
	}
    }
    FREE(oldBuckets);
    return(1);

} /* end of cuddHashTableResize */


/**
  @brief Fast storage allocation for items in a hash table.

  @details The first sizeof(void *) bytes of a chunk contain a pointer to the
  next block; the rest contains DD_MEM_CHUNK spaces for hash items.

  @return a pointer to a new item if successful; NULL is memory is full.

  @sideeffect None

  @see cuddAllocNode cuddDynamicAllocNode

*/
static DdHashItem *
cuddHashTableAlloc(
  DdHashTable * hash)
{
    int i;
    unsigned int itemsize = hash->itemsize;
    extern DD_OOMFP MMoutOfMemory;
    DD_OOMFP saveHandler;
    DdHashItem **mem, *thisOne, *next, *item;

    if (hash->nextFree == NULL) {
	saveHandler = MMoutOfMemory;
	MMoutOfMemory = hash->manager->outOfMemCallback;
	mem = (DdHashItem **) ALLOC(char,(DD_MEM_CHUNK+1) * itemsize);
	MMoutOfMemory = saveHandler;
	if (mem == NULL) {
	    if (hash->manager->stash != NULL) {
		FREE(hash->manager->stash);
		hash->manager->stash = NULL;
		/* Inhibit resizing of tables. */
		hash->manager->maxCacheHard = hash->manager->cacheSlots - 1;
		hash->manager->cacheSlack = - (int) (hash->manager->cacheSlots + 1);
		for (i = 0; i < hash->manager->size; i++) {
		    hash->manager->subtables[i].maxKeys <<= 2;
		}
		hash->manager->gcFrac = 0.2;
		hash->manager->minDead =
		    (unsigned) (0.2 * (double) hash->manager->slots);
		mem = (DdHashItem **) ALLOC(char,(DD_MEM_CHUNK+1) * itemsize);
	    }
	    if (mem == NULL) {
		(*MMoutOfMemory)((size_t)((DD_MEM_CHUNK + 1) * itemsize));
		hash->manager->errorCode = CUDD_MEMORY_OUT;
		return(NULL);
	    }
	}

	mem[0] = (DdHashItem *) hash->memoryList;
	hash->memoryList = mem;

	thisOne = (DdHashItem *) ((char *) mem + itemsize);
	hash->nextFree = thisOne;
	for (i = 1; i < DD_MEM_CHUNK; i++) {
	    next = (DdHashItem *) ((char *) thisOne + itemsize);
	    thisOne->next = next;
	    thisOne = next;
	}

	thisOne->next = NULL;

    }
    item = hash->nextFree;
    hash->nextFree = item->next;
    return(item);

} /* end of cuddHashTableAlloc */
