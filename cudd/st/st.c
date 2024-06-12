/**
  @file

  @ingroup st

  @brief Symbol table package.

  @details The st library provides functions to create, maintain,
  and query symbol tables.

  @copyright@parblock
  Copyright (c) 1994-1998 The Regents of the Univ. of California.
  All rights reserved.

  Permission is hereby granted, without written agreement and without license
  or royalty fees, to use, copy, modify, and distribute this software and its
  documentation for any purpose, provided that the above copyright notice and
  the following two paragraphs appear in all copies of this software.

  IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
  DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
  OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
  CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

  THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN
  "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE
  MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
  @endparblock

  @copyright@parblock
  Copyright (c) 1999-2015, Regents of the University of Colorado

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
#include "st.h"

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/**
 * @brief Type of symbol table entries.
 */     
typedef struct st_table_entry st_table_entry;

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/

/**
 * @brief Symbol table entry.
 */
struct st_table_entry {
    void *key;
    void *record;
    st_table_entry *next;
};

/**
 * @brief Symbol table header.
 */
struct st_table {
    st_compare_t compare;
    st_hash_t hash;
    st_compare_arg_t compare_arg;
    st_hash_arg_t hash_arg;
    void const * arg;
    int num_bins;
    int num_entries;
    int max_density;
    int reorder_flag;
    double grow_factor;
    st_table_entry **bins;
};

/**
 * @brief Symbol table generator.
 */
struct st_generator {
    st_table const *table;
    st_table_entry const *entry;
    int index;
};

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

/**
 * @brief Compares two numbers or two pointers.
 *
 * @details Used by the default comparison functions.
 */
#define ST_NUMCMP(x,y) ((x) != (y))

/**
 * @brief Hash function for numbers.
 */
#define ST_NUMHASH(x,size) ((int)((uintptr_t)(x)%(uintptr_t)(size)))

/**
 * @brief Amount by which pointers should be shifted right when hashing.
 *
 * @details This is to discard bits that are (likely to be) 0 due to
 * alignment constraints.
 */
#if SIZEOF_VOID_P == 8
#define st_shift 3
#else
#define st_shift 2
#endif

/**
 * @brief Hash function for pointers.
 */
#define ST_PTRHASH(x,size) ((int)(((uintptr_t)(x)>>st_shift)%(uintptr_t)(size)))

/**
 * @brief Compares two entries.
 */
#define EQUAL(table, x, y) \
    ((((table)->compare == st_numcmp) || ((table)->compare == st_ptrcmp)) ?\
     (ST_NUMCMP((x),(y)) == 0) : ((table)->compare) ?\
     ((*(table)->compare)((x), (y)) == 0) :\
     ((*(table)->compare_arg)((x), (y), (table)->arg) == 0))

/**
 * @brief Computes the hash of one entry.
 */
#define do_hash(key, table)\
    (((table)->hash == st_ptrhash) ? ST_PTRHASH((key), (table)->num_bins) : \
     ((table)->hash == st_numhash) ? ST_NUMHASH((key), (table)->num_bins) : \
     ((table)->hash) ? (*(table)->hash)((key), (table)->num_bins) : \
     (*(table)->hash_arg)((key), (table)->num_bins, (table)->arg))

/**
 * @brief Compares the new key to one in a collision list.
 */
#define PTR_NOT_EQUAL(table, ptr, user_key)\
    ((ptr) != NIL(st_table_entry) && \
     !EQUAL((table), (user_key), (ptr)->key))

/**
 * @brief Looks up an entry in a collision list.
 *
 * @details If the entry is found and the reorder flag is set, the found
 * entry is brought to the fore of the collision list.
 */
#define FIND_ENTRY(table, hash_val, key, ptr, last) \
    (last) = &(table)->bins[hash_val];\
    (ptr) = *(last);\
    while (PTR_NOT_EQUAL((table), (ptr), (key))) {\
	(last) = &(ptr)->next; (ptr) = *(last);\
    }\
    if ((ptr) != NIL(st_table_entry) && (table)->reorder_flag) {\
	*(last) = (ptr)->next;\
	(ptr)->next = (table)->bins[hash_val];\
	(table)->bins[hash_val] = (ptr);\
    }

/**
 * @brief Adds an entry to a table.
 *
 * @deprecated This macro does not check if memory allocation fails.
 * Use at your own risk.
 */
#define ADD_DIRECT(table, key, value, hash_val, newt)\
{\
    if (table->num_entries/table->num_bins >= table->max_density) {\
	rehash(table);\
	hash_val = do_hash(key,table);\
    }\
    \
    newt = ALLOC(st_table_entry, 1);\
    \
    newt->key = key;\
    newt->record = value;\
    newt->next = table->bins[hash_val];\
    table->bins[hash_val] = newt;\
    table->num_entries++;\
}

/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static int rehash (st_table *);

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/

/**
  @brief Creates and initializes a table.

  @details Creates and initializes a table with the comparison function
  compare_fn and hash function hash_fn. compare_fn is

      int compare_fn(const void *key1, const void *key2)

  It returns `<,=,> 0` depending on whether `key1 <,=,> key2` by some
  measure.<p>
  hash_fn is

      int hash_fn(void *key, int modulus)

  It returns an integer between `0` and `modulus-1` such that if
  `compare_fn(key1,key2) == 0` then `hash_fn(key1) == hash_fn(key2)`.<p>
  There are five predefined hash and comparison functions in st.
  For keys as numbers:

      st_numhash(key, modulus) { return (unsigned int) key % modulus; }
      st_numcmp(x,y) { return (int) x - (int) y; }

  For keys as pointers:

      st_ptrhash(key, modulus) { return ((unsigned int) key/4) % modulus }
      st_ptrcmp(x,y) { return (int) x - (int) y; }

  For keys as strings:

      st_strhash(x,y) - a reasonable hashing function for strings
      strcmp(x,y) - the standard library function

  It is recommended to use these particular functions if they fit your
  needs, since st will recognize certain of them and run more quickly
  because of it.

  @sideeffect None

  @see st_init_table_with_params st_free_table

*/
st_table *
st_init_table(st_compare_t compare, st_hash_t hash)
{
    return st_init_table_with_params(compare, hash, ST_DEFAULT_INIT_TABLE_SIZE,
				     ST_DEFAULT_MAX_DENSITY,
				     ST_DEFAULT_GROW_FACTOR,
				     ST_DEFAULT_REORDER_FLAG);

} /* st_init_table */


/**
  @brief Create a table with given parameters.

  @details The full blown table initializer.  compare and hash are
  the same as in st_init_table. density is the largest the average
  number of entries per hash bin there should be before the table is
  grown.  grow_factor is the factor the table is grown by when it
  becomes too full. size is the initial number of bins to be allocated
  for the hash table.  If reorder_flag is non-zero, then every time an
  entry is found, it is moved to the top of the chain.<p>
  st_init_table(compare, hash) is equivelent to

      st_init_table_with_params(compare, hash, ST_DEFAULT_INIT_TABLE_SIZE,
                                ST_DEFAULT_MAX_DENSITY, ST_DEFAULT_GROW_FACTOR,
                                ST_DEFAULT_REORDER_FLAG);

  @sideeffect None

  @see st_init_table st_free_table

*/
st_table *
st_init_table_with_params(
  st_compare_t compare,
  st_hash_t hash,
  int size,
  int density,
  double grow_factor,
  int reorder_flag)
{
    int i;
    st_table *newt;

    newt = ALLOC(st_table, 1);
    if (newt == NIL(st_table)) {
	return NIL(st_table);
    }
    newt->compare = compare;
    newt->hash = hash;
    newt->compare_arg = (st_compare_arg_t) 0;
    newt->hash_arg = (st_hash_arg_t) 0;
    newt->arg = NIL(void);
    newt->num_entries = 0;
    newt->max_density = density;
    newt->grow_factor = grow_factor;
    newt->reorder_flag = reorder_flag;
    if (size <= 0) {
	size = 1;
    }
    newt->num_bins = size;
    newt->bins = ALLOC(st_table_entry *, size);
    if (newt->bins == NIL(st_table_entry *)) {
	FREE(newt);
	return NIL(st_table);
    }
    for(i = 0; i < size; i++) {
	newt->bins[i] = 0;
    }
    return newt;

} /* st_init_table_with_params */


/**
   @brief Creates and initializes a table.

   @details Like st_init_table_with_params, but the comparison and
   hash functions are passed an extra parameter `arg` that is
   registered in the table at initialization.

   @see st_init_table_with_params
*/
st_table *
st_init_table_with_params_and_arg(
  st_compare_arg_t compare,
  st_hash_arg_t hash,
  void const * arg,
  int size,
  int density,
  double growth_factor,
  int reorder_flag)
{
    st_table *table;

    table = st_init_table_with_params((st_compare_t) 0, (st_hash_t) 0, size,
                                      density, growth_factor, reorder_flag);
    if (table == NIL(st_table))
        return NIL(st_table);
    table->compare_arg = compare;
    table->hash_arg = hash;
    table->arg = arg;

    return table;

} /* st_init_table_with_params_and_arg */


/**
   @brief Creates and initializes a table.

   @details Like st_init_table, but the comparison and hash functions are
   passed an extra parameter `arg` that is registered in the table at
   initialization.

   @see st_init_table st_init_table_with_params_and_arg
*/
st_table *
st_init_table_with_arg(
  st_compare_arg_t compare,
  st_hash_arg_t hash,
  void const * arg)
{
    return st_init_table_with_params_and_arg(compare, hash, arg,
                                             ST_DEFAULT_INIT_TABLE_SIZE,
                                             ST_DEFAULT_MAX_DENSITY,
                                             ST_DEFAULT_GROW_FACTOR,
                                             ST_DEFAULT_REORDER_FLAG);

} /* st_init_table_with_arg */


/**
  @brief Free a table.

  @details Any internal storage associated with table is freed.  It is
  the user's responsibility to free any storage associated with the
  pointers he placed in the table (by perhaps using st_foreach).

  @sideeffect None

  @see st_init_table st_init_table_with_params

*/
void
st_free_table(st_table *table)
{
    st_table_entry *ptr, *next;
    int i;

    for(i = 0; i < table->num_bins ; i++) {
	ptr = table->bins[i];
	while (ptr != NIL(st_table_entry)) {
	    next = ptr->next;
	    FREE(ptr);
	    ptr = next;
	}
    }
    FREE(table->bins);
    FREE(table);

} /* st_free_table */


/**
  @brief Lookup up `key` in `table`.

  @details If an entry is found, 1 is returned and if `value` is not
  nil, the variable it points to is set to the associated value.  If
  an entry is not found, 0 is returned and the variable pointed by
  value is unchanged.

  @sideeffect The location pointed by value is modified.

  @see st_lookup_int

*/
int
st_lookup(st_table *table, void const *key, void **value)
{
    int hash_val;
    st_table_entry *ptr, **last;

    hash_val = do_hash(key, table);

    FIND_ENTRY(table, hash_val, key, ptr, last);

    if (ptr == NIL(st_table_entry)) {
	return 0;
    } else {
	if (value != NIL(void *)) {
	    *value = ptr->record;
	}
	return 1;
    }

} /* st_lookup */


/**
  @brief Lookup up `key` in `table`.

  @details If an entry is found, 1 is returned and if `value` is not
  nil, the variable it points to is set to the associated integer
  value.  If an entry is not found, 0 is return and the variable
  pointed by `value` is unchanged.

  @sideeffect The location pointed by value is modified.

  @see st_lookup

*/
int
st_lookup_int(st_table *table, void const *key, int *value)
{
    int hash_val;
    st_table_entry *ptr, **last;

    hash_val = do_hash(key, table);

    FIND_ENTRY(table, hash_val, key, ptr, last);
    
    if (ptr == NIL(st_table_entry)) {
	return 0;
    } else {
	if (value != NIL(int)) {
	    *value = (int) (intptr_t) ptr->record;
	}
	return 1;
    }

} /* st_lookup_int */


/**
  @brief Insert value in `table` under the key `key`.

  @return 1 if there was an entry already under the key; 0 if there
  was no entry under the key and insertion was successful;
  ST_OUT_OF_MEM otherwise.  In either of the first two cases the new
  value is added.

  @sideeffect None

*/
int
st_insert(st_table *table, void *key, void *value)
{
    int hash_val;
    st_table_entry *newt;
    st_table_entry *ptr, **last;

    hash_val = do_hash(key, table);

    FIND_ENTRY(table, hash_val, key, ptr, last);

    if (ptr == NIL(st_table_entry)) {
	if (table->num_entries/table->num_bins >= table->max_density) {
	    if (rehash(table) == ST_OUT_OF_MEM) {
		return ST_OUT_OF_MEM;
	    }
	    hash_val = do_hash(key, table);
	}
	newt = ALLOC(st_table_entry, 1);
	if (newt == NIL(st_table_entry)) {
	    return ST_OUT_OF_MEM;
	}
	newt->key = key;
	newt->record = value;
	newt->next = table->bins[hash_val];
	table->bins[hash_val] = newt;
	table->num_entries++;
	return 0;
    } else {
	ptr->record = value;
	return 1;
    }

} /* st_insert */


/**
  @brief Place 'value' in 'table' under the key 'key'.

  @details This is done without checking if 'key' is in 'table'
  already.  This should only be used if you are sure there is not
  already an entry for 'key', since it is undefined which entry you
  would later get from st_lookup or st_find_or_add.

  @return 1 if successful; ST_OUT_OF_MEM otherwise.

  @sideeffect None

  @see st_lookup st_find_or_add

*/
int
st_add_direct(st_table *table, void *key, void *value)
{
    int hash_val;
    st_table_entry *newt;

    if (table->num_entries / table->num_bins >= table->max_density) {
	if (rehash(table) == ST_OUT_OF_MEM) {
	    return ST_OUT_OF_MEM;
	}
    }
    hash_val = do_hash(key, table);
    newt = ALLOC(st_table_entry, 1);
    if (newt == NIL(st_table_entry)) {
	return ST_OUT_OF_MEM;
    }
    newt->key = key;
    newt->record = value;
    newt->next = table->bins[hash_val];
    table->bins[hash_val] = newt;
    table->num_entries++;
    return 1;

} /* st_add_direct */


/**
  @brief Lookup `key` in `table`; if not found, create an entry.

  @details In either case set slot to point to the field in the entry
  where the value is stored.  The value associated with `key` may then
  be changed by accessing directly through slot.  As an example:

      void **slot;
      void *key;
      void *value = item_ptr // ptr to a malloc'd structure

      if (st_find_or_add(table, key, &slot) == 1) {
          FREE(*slot); // free the old value of the record
      }
      *slot = value;  // attach the new value to the record

  This replaces the equivelent code:

      if (st_lookup(table, key, &ovalue) == 1) {
          FREE(ovalue);
      }
      st_insert(table, key, value);

  @return 1 if an entry already existed, 0 if it did not exist and
  creation was successful; ST_OUT_OF_MEM otherwise.

  @sideeffect The location pointed by slot is modified.

  @see st_find

*/
int
st_find_or_add(st_table *table, void *key, void ***slot)
{
    int hash_val;
    st_table_entry *newt, *ptr, **last;

    hash_val = do_hash(key, table);

    FIND_ENTRY(table, hash_val, key, ptr, last);

    if (ptr == NIL(st_table_entry)) {
	if (table->num_entries / table->num_bins >= table->max_density) {
	    if (rehash(table) == ST_OUT_OF_MEM) {
		return ST_OUT_OF_MEM;
	    }
	    hash_val = do_hash(key, table);
	}
	newt = ALLOC(st_table_entry, 1);
	if (newt == NIL(st_table_entry)) {
	    return ST_OUT_OF_MEM;
	}
	newt->key = key;
	newt->record = NIL(void);
	newt->next = table->bins[hash_val];
	table->bins[hash_val] = newt;
	table->num_entries++;
	if (slot != NIL(void **)) *slot = &newt->record;
	return 0;
    } else {
	if (slot != NIL(void **)) *slot = &ptr->record;
	return 1;
    }

} /* st_find_or_add */


/**
  @brief Lookup `key` in `table`.

  @details Like st_find_or_add, but does not create an entry if one is
  not found.

  @sideeffect The location pointed by slot is modified.

  @see st_find_or_add

*/
int
st_find(st_table *table, void const *key, void ***slot)
{
    int hash_val;
    st_table_entry *ptr, **last;

    hash_val = do_hash(key, table);

    FIND_ENTRY(table, hash_val, key, ptr, last);

    if (ptr == NIL(st_table_entry)) {
	return 0;
    } else {
	if (slot != NIL(void **)) {
	    *slot = &ptr->record;
	}
	return 1;
    }

} /* st_find */


/**
  @brief Returns a copy of old_table and all its members.

  @details (st_table *) 0 is returned if there was insufficient memory
  to do the copy.

  @sideeffect None

*/
st_table *
st_copy
(st_table const *old_table)
{
    st_table *new_table;
    st_table_entry *ptr, *newptr, *next, *newt;
    int i, j, num_bins = old_table->num_bins;

    new_table = ALLOC(st_table, 1);
    if (new_table == NIL(st_table)) {
	return NIL(st_table);
    }

    *new_table = *old_table;
    new_table->bins = ALLOC(st_table_entry *, num_bins);
    if (new_table->bins == NIL(st_table_entry *)) {
	FREE(new_table);
	return NIL(st_table);
    }
    for(i = 0; i < num_bins ; i++) {
	new_table->bins[i] = NIL(st_table_entry);
	ptr = old_table->bins[i];
	while (ptr != NIL(st_table_entry)) {
	    newt = ALLOC(st_table_entry, 1);
	    if (newt == NIL(st_table_entry)) {
		for (j = 0; j <= i; j++) {
		    newptr = new_table->bins[j];
		    while (newptr != NIL(st_table_entry)) {
			next = newptr->next;
			FREE(newptr);
			newptr = next;
		    }
		}
		FREE(new_table->bins);
		FREE(new_table);
		return NIL(st_table);
	    }
	    *newt = *ptr;
	    newt->next = new_table->bins[i];
	    new_table->bins[i] = newt;
	    ptr = ptr->next;
	}
    }
    return new_table;

} /* st_copy */


/**
  @brief Deletes the entry with the key pointed to by `keyp`.

  @details If the entry is found, 1 is returned, the variable pointed
  by `keyp` is set to the actual key and the variable pointed by
  `value` is set to the corresponding entry.  (This allows the freeing
  of the associated storage.)  If the entry is not found, then 0 is
  returned and nothing is changed.

  @sideeffect The locations pointed by keyp and value are modified.

  @see st_delete_int

*/
int
st_delete(st_table *table, void **keyp, void **value)
{
    int hash_val;
    void *key = *keyp;
    st_table_entry *ptr, **last;

    hash_val = do_hash(key, table);

    FIND_ENTRY(table, hash_val, key, ptr ,last);

    if (ptr == NIL(st_table_entry)) {
	return 0;
    }

    *last = ptr->next;
    if (value != NIL(void *)) *value = ptr->record;
    *keyp = ptr->key;
    FREE(ptr);
    table->num_entries--;
    return 1;

} /* st_delete */


/**
  @brief Deletes the entry with the key pointed to by `keyp`.

  @details `value` must be a pointer to an integer.  If the entry is
  found, 1 is returned, the variable pointed by `keyp` is set to the
  actual key and the variable pointed by `value` is set to the
  corresponding entry.  (This allows the freeing of the associated
  storage.) If the entry is not found, then 0 is returned and nothing
  is changed.

  @sideeffect The locations pointed by keyp and value are modified.

  @see st_delete

*/
int
st_delete_int(st_table *table, void **keyp, int *value)
{
    int hash_val;
    void *key = *keyp;
    st_table_entry *ptr, **last;

    hash_val = do_hash(key, table);

    FIND_ENTRY(table, hash_val, key, ptr ,last);

    if (ptr == NIL(st_table_entry)) {
        return 0;
    }

    *last = ptr->next;
    if (value != NIL(int)) *value = (int) (intptr_t) ptr->record;
    *keyp = ptr->key;
    FREE(ptr);
    table->num_entries--;
    return 1;

} /* st_delete_int */


/**
  @brief Returns the number of entries in the table `table`.

  @sideeffect None

*/
int st_count(st_table const *table)
{
  return table->num_entries;
}


/**
  @brief Iterates over the elements of a table.

  @details
  For each (key, value) record in `table`, st_foreach
  calls func with the arguments

      (*func)(key, value, arg)

  If func returns ST_CONTINUE, st_foreach continues
  processing entries.  If func returns ST_STOP, st_foreach stops
  processing and returns immediately.  If func returns ST_DELETE, then
  the entry is deleted from the symbol table and st_foreach continues.
  In the case of ST_DELETE, it is func's responsibility to free the
  key and value, if necessary.  The order in which the records are
  visited will be seemingly random.

  @return 1 if all items in the table were generated and 0 if the
  generation sequence was aborted using ST_STOP.

  @sideeffect None

  @see st_foreach_item st_foreach_item_int

*/
int
st_foreach(st_table *table, st_foreach_t func, void *arg)
{
    st_table_entry *ptr, **last;
    enum st_retval retval;
    int i;

    for(i = 0; i < table->num_bins; i++) {
	last = &table->bins[i]; ptr = *last;
	while (ptr != NIL(st_table_entry)) {
	    retval = (*func)(ptr->key, ptr->record, arg);
	    switch (retval) {
	    case ST_CONTINUE:
		last = &ptr->next; ptr = *last;
		break;
	    case ST_STOP:
		return 0;
	    case ST_DELETE:
		*last = ptr->next;
		table->num_entries--;	/* cstevens@ic */
		FREE(ptr);
		ptr = *last;
	    }
	}
    }
    return 1;

} /* st_foreach */


/**
  @brief String hash function.

  @sideeffect None

  @see st_init_table

*/
int
st_strhash(void const *string, int modulus)
{
    int val = 0;
    int c;
    char const * s = (char const *) string;
    
    while ((c = *s++) != '\0') {
	val = val*997 + c;
    }

    return ((val < 0) ? -val : val)%modulus;

} /* st_strhash */


/**
  @brief Integral number hash function.

  @sideeffect None

  @see st_init_table st_numcmp

*/
int
st_numhash(void const *x, int size)
{
    return ST_NUMHASH(x, size);

} /* st_numhash */


/**
  @brief Pointer hash function.

  @sideeffect None

  @see st_init_table st_ptrcmp

*/
int
st_ptrhash(void const *x, int size)
{
    return ST_PTRHASH(x, size);

} /* st_ptrhash */


/**
  @brief Integral number comparison function.

  @sideeffect None

  @see st_init_table st_numhash

*/
int
st_numcmp(void const *x, void const *y)
{
    return ST_NUMCMP(x, y);

} /* st_numcmp */


/**
  @brief Pointer comparison function.

  @sideeffect None

  @see st_init_table st_ptrhash

*/
int
st_ptrcmp(void const *x, void const *y)
{
    return ST_NUMCMP(x, y);

} /* st_ptrcmp */


/**
  @brief Initializes a generator.

  @details Returns a generator handle which when used with
  st_gen() will progressively return each (key, value) record in
  `table`.

  @sideeffect None

  @see st_free_gen

*/
st_generator *
st_init_gen(st_table const *table)
{
    st_generator *gen;

    gen = ALLOC(st_generator, 1);
    if (gen == NIL(st_generator)) {
	return NIL(st_generator);
    }
    gen->table = table;
    gen->entry = NIL(st_table_entry);
    gen->index = 0;
    return gen;

} /* st_init_gen */


/**
  @brief Returns the next (key, value) pair in the generation sequence.

  @details@parblock
  Given a generator returned by st_init_gen(), this
  routine returns the next (key, value) pair in the generation
  sequence.  The pointer `value_p` can be zero which means no value
  will be returned.  When there are no more items in the generation
  sequence, the routine returns 0.

  While using a generation sequence, deleting any (key, value) pair
  other than the one just generated may cause a fatal error when
  st_gen() is called later in the sequence and is therefore not
  recommended.
  @endparblock

  @sideeffect The locations pointed by key_p and value_p are modified.

  @see st_gen_int

*/
int
st_gen(st_generator *gen, void **key_p, void **value_p)
{
    int i;

    if (gen->entry == NIL(st_table_entry)) {
	/* try to find next entry */
	for(i = gen->index; i < gen->table->num_bins; i++) {
	    if (gen->table->bins[i] != NIL(st_table_entry)) {
		gen->index = i+1;
		gen->entry = gen->table->bins[i];
		break;
	    }
	}
	if (gen->entry == NIL(st_table_entry)) {
	    return 0;		/* that's all folks ! */
	}
    }
    *key_p = gen->entry->key;
    if (value_p != NIL(void *)) {
	*value_p = gen->entry->record;
    }
    gen->entry = gen->entry->next;
    return 1;

} /* st_gen */


/**
  @brief Returns the next (key, value) pair in the generation
  sequence.

  @details Given a generator returned by st_init_gen(), this
  routine returns the next (key, value) pair in the generation
  sequence.  `value_p` must be a pointer to an integer.  The pointer
  `value_p` can be zero which means no value will be returned.  When
  there are no more items in the generation sequence, the routine
  returns 0.

  @sideeffect The locations pointed by key_p and value_p are modified.

  @see st_gen

*/
int 
st_gen_int(st_generator *gen, void **key_p, int *value_p)
{
    int i;

    if (gen->entry == NIL(st_table_entry)) {
	/* try to find next entry */
	for(i = gen->index; i < gen->table->num_bins; i++) {
	    if (gen->table->bins[i] != NIL(st_table_entry)) {
		gen->index = i+1;
		gen->entry = gen->table->bins[i];
		break;
	    }
	}
	if (gen->entry == NIL(st_table_entry)) {
	    return 0;		/* that's all folks ! */
	}
    }
    *key_p = gen->entry->key;
    if (value_p != NIL(int)) {
   	*value_p = (int) (intptr_t) gen->entry->record;
    }
    gen->entry = gen->entry->next;
    return 1;

} /* st_gen_int */


/**
  @brief Reclaims the resources associated with `gen`.

  @details After generating all items in a generation sequence,
  this routine must be called to reclaim the resources associated with
  `gen`.

  @sideeffect None

  @see st_init_gen

*/
void
st_free_gen(st_generator *gen)
{
    FREE(gen);

} /* st_free_gen */


/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/

/**
  @brief Rehashes a symbol table.

  @sideeffect None

  @see st_insert

*/
static int
rehash(st_table *table)
{
    st_table_entry *ptr, *next, **old_bins;
    int             i, old_num_bins, hash_val, old_num_entries;

    /* save old values */
    old_bins = table->bins;
    old_num_bins = table->num_bins;
    old_num_entries = table->num_entries;

    /* rehash */
    table->num_bins = (int) (table->grow_factor * old_num_bins);
    if (table->num_bins % 2 == 0) {
	table->num_bins += 1;
    }
    table->num_entries = 0;
    table->bins = ALLOC(st_table_entry *, table->num_bins);
    if (table->bins == NIL(st_table_entry *)) {
	table->bins = old_bins;
	table->num_bins = old_num_bins;
	table->num_entries = old_num_entries;
	return ST_OUT_OF_MEM;
    }
    /* initialize */
    for (i = 0; i < table->num_bins; i++) {
	table->bins[i] = 0;
    }

    /* copy data over */
    for (i = 0; i < old_num_bins; i++) {
	ptr = old_bins[i];
	while (ptr != NIL(st_table_entry)) {
	    next = ptr->next;
	    hash_val = do_hash(ptr->key, table);
	    ptr->next = table->bins[hash_val];
	    table->bins[hash_val] = ptr;
	    table->num_entries++;
	    ptr = next;
	}
    }
    FREE(old_bins);

    return 1;

} /* rehash */
