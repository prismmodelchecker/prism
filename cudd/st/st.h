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

#ifndef ST_H_
#define ST_H_

/*---------------------------------------------------------------------------*/
/* Nested includes                                                           */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

/**
 * @brief Value returned if memory is exhausted.
 */
#define ST_OUT_OF_MEM -10000

/**
 * @brief Default value for the maximum table density.
 * @see st_init_table_with_params
 */
#define ST_DEFAULT_MAX_DENSITY 5

/**
 * @brief Default value for the initial table size.
 * @see st_init_table_with_params
 */
#define ST_DEFAULT_INIT_TABLE_SIZE 11

/**
 * @brief Default table growth factor.
 * @see st_init_table_with_params
 */
#define ST_DEFAULT_GROW_FACTOR 2.0

/**
 * @brief Default table reorder flag.
 * @see st_init_table_with_params
 */
#define ST_DEFAULT_REORDER_FLAG 0

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/**
 * @brief Type of symbol tables.
 */
typedef struct st_table st_table;

/**
 * @brief Type of symbol table generators.
 */
typedef struct st_generator st_generator;

/**
 * @brief Type of return values for iterators.
 */
enum st_retval {ST_CONTINUE, ST_STOP, ST_DELETE};

/**
 *  @brief Type for function passed to @ref st_foreach.
 */
typedef enum st_retval (*st_foreach_t)(void *, void *, void *);

/**
 * @brief Type of comparison functions.
 */
typedef int (*st_compare_t)(void const *, void const *);

/**
 * @brief Type of hash functions.
 */
typedef int (*st_hash_t)(void const *, int);

/**
 * @brief Type of comparison functions with extra argument.
 */
typedef int (*st_compare_arg_t)(void const *, void const *, void const *);

/**
 * @brief Type of hash functions with extra argument.
 */
typedef int (*st_hash_arg_t)(void const *, int, void const *);

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

/**
  @brief Checks whethere `key` is in `table`.

  @details Returns 1 if there is an entry under `key` in `table`, 0
  otherwise.

  @sideeffect None

  @see st_lookup

*/
#define st_is_member(table,key) st_lookup(table,key,(void **) 0)


/**
  @brief Iteration macro.

  @details
  An iteration macro which loops over all the entries in
  `table`, setting `key` to point to the key and `value` to the
  associated value (if it is not nil). `gen` is a generator variable
  used internally. Sample usage:

      void *key, *value;
      st_generator *gen;

      st_foreach_item(table, gen, &key, &value) {
          process_item(value);
      }

  @sideeffect None

  @see st_foreach_item_int st_foreach

*/
#define st_foreach_item(table, gen, key, value) \
    for(gen=st_init_gen(table); st_gen(gen,key,value) || (st_free_gen(gen),0);)


/**
  @brief Iteration macro.

  @details
  An iteration macro which loops over all the entries in
  `table`, setting `key` to point to the key and `value` to the
  associated value (if it is not nil). `value` is assumed to be a
  pointer to an integer.  `gen` is a generator variable used
  internally. Sample usage:

      void *key;
      int value;
      st_generator *gen;

      st_foreach_item_int(table, gen, &key, &value) {
          process_item(value);
      }

  @sideeffect None

  @see st_foreach_item st_foreach

*/
#define st_foreach_item_int(table, gen, key, value) \
    for(gen=st_init_gen(table); st_gen_int(gen,key,value) || (st_free_gen(gen),0);)

/*---------------------------------------------------------------------------*/
/* Function prototypes                                                       */
/*---------------------------------------------------------------------------*/

#ifdef __cplusplus
extern "C" {
#endif

st_table *st_init_table_with_params (st_compare_t, st_hash_t, int, int, double, int);
st_table *st_init_table (st_compare_t, st_hash_t);
st_table *st_init_table_with_params_and_arg (st_compare_arg_t, st_hash_arg_t, void const *, int, int, double, int);
    st_table *st_init_table_with_arg (st_compare_arg_t, st_hash_arg_t, void const *);
void st_free_table (st_table *);
int st_lookup (st_table *, void const *, void **);
int st_lookup_int (st_table *, void const *, int *);
int st_insert (st_table *, void *, void *);
int st_add_direct (st_table *, void *, void *);
int st_find_or_add (st_table *, void *, void ***);
int st_find (st_table *, void const *, void ***);
st_table *st_copy (st_table const *);
int st_delete (st_table *, void **, void **);
int st_delete_int (st_table *, void **, int *);
int st_count(st_table const *);
int st_foreach (st_table *, st_foreach_t, void *);
int st_strhash (void const *, int);
int st_numhash (void const *, int);
int st_ptrhash (void const *, int);
int st_numcmp (void const *, void const *);
int st_ptrcmp (void const *, void const *);
st_generator *st_init_gen (st_table const *);
int st_gen (st_generator *, void **, void **);
int st_gen_int (st_generator *, void **, int *);
void st_free_gen (st_generator *);

#ifdef __cplusplus
} /* end of extern "C" */
#endif

#endif /* ST_H_ */
