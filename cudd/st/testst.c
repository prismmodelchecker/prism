/**
  @file

  @ingroup st

  @brief Simple test program of the st library.

  @copyright@parblock
  Copyright (c) 2015, Regents of the University of Colorado

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

/**
   @brief Just some struct type.
*/
typedef struct mys {
    double a;
    int b;
    int c;
} mys_t;


/** \cond */

static int testString(void);
static int testStruct(void);
static int testUintPtr(void);
static int testInt(void);
static int testArg(void);
static int mys_cmp(void const * key1, void const * key2);
static int mys_hash(void const * key, int size);
static enum st_retval mys_accm(void * key, void * value, void * arg);
static int array_hash(void const * key, int modulus, void const * arg);
static int array_cmp(void const * key1, void const * key2, void const *arg);

/** \endcond */

/**
   @brief Main program.

   @return the number of failed tests.

   @details Uses TAP (Test Anything Protocol) to report results.
*/
int
main(void)
{
    int ret = 0;
    printf("TAP version 13\n1..5\n");

    if (testString() != 0) {
        ret++;
        printf("not ");
    }
    printf("ok 1 string table\n");

    if (testStruct() != 0) {
        ret++;
        printf("not ");
    }
    printf("ok 2 struct-to-uintptr_t map\n");

    if (testUintPtr() != 0) {
        ret++;
        printf("not ");
    }
    printf("ok 3 uintptr_t-to-string map\n");

    if (testInt() != 0) {
        ret++;
        printf("not ");
    }
    printf("ok 4 int-to-int map\n");

    if (testArg() != 0) {
        ret++;
        printf("not ");
    }
    printf("ok 5 table with arg\n");

    return ret;
}


/**
   @brief Tests a table that stores C strings.

   @return 0 if successful; the number of errors otherwise.
*/
static int
testString(void)
{
    int error = 0;
    char foo[] = "foo";
    char *cp = foo;
    char bar[] = "bar";
    char foobar[] = "foobar";
    st_table * tbl = st_init_table((st_compare_t) strcmp, st_strhash);
    if (!tbl)
        error++;
    if (st_insert(tbl, foo, NULL) != 0)
        error++;
    if (st_insert(tbl, bar, NULL) != 0)
        error++;
    if (st_insert(tbl, foobar, NULL) != 0)
        error++;
    if (!st_is_member(tbl, "foo"))
        error++;
    if (!st_delete(tbl, (void **) &cp, NULL))
        error++;
    if (st_count(tbl) != 2)
        error++;
    if (st_insert(tbl, bar, NULL) != 1)
        error++;
    st_free_table(tbl);
    return error;
}


/**
   @brief Tests a table that maps user-defined structs to uintptr_t.

   @return 0 if successful; the number of errors otherwise.
*/
static int
testStruct(void)
{
    int error = 0;
    mys_t m1 = {3.5, 4, 11};
    mys_t m2 = {6.7, 5, -2};
    uintptr_t u;
    st_table * tbl = st_init_table(mys_cmp, mys_hash);
    if (!tbl)
        error++;
    if (st_insert(tbl, &m1, (void *)(uintptr_t) 2) != 0)
        error++;
    if (st_insert(tbl, &m2, (void *)(uintptr_t) 5) != 0)
        error++;
    if (st_lookup(tbl, &m1, (void **) &u) != 1)
        error++;
    if (u != 2)
        error++;
    u = 0;
    if (st_foreach(tbl, mys_accm, &u) != 1)
        error++;
    if (u != 7)
        error++;
    st_free_table(tbl);    
    return error;
}


/**
   @brief Tests a table that maps values of type uintptr_t to strings.

   @return 0 if successful; the number of errors otherwise.
*/
static int
testUintPtr(void)
{
    int error = 0;
    char foo[] = "foo";
    char * cp;
    st_table * tbl = st_init_table(st_numcmp, st_numhash);
    if (!tbl)
        error++;
    if (st_insert(tbl, (void *)(uintptr_t) 2, foo) != 0)
        error++;
    if (st_lookup(tbl, (void *)(uintptr_t) 2, (void **) &cp) != 1)
        error++;
    if (strcmp(cp, "foo") != 0)
        error++;
    if (st_is_member(tbl, (void *)(uintptr_t) 76))
        error++;
    st_free_table(tbl);
    return error;
}


/**
   @brief Tests a table that maps ints to ints.

   @return 0 if successful; the number of errors otherwise.
*/
static int
testInt(void)
{
    int error = 0;
    int n1 = -2;
    int n2;
    void * e;
    int i;
    st_generator * gen;
    st_table * tbl = st_init_table(st_numcmp, st_numhash);
    if (!tbl)
        error++;
    if (st_insert(tbl, (void *)(intptr_t) n1, (void *)(intptr_t) 3) != 0)
        error++;
    if (st_lookup_int(tbl, (void *)(intptr_t) n1, &n2) != 1)
        error++;
    if (n2 != 3)
        error++;
    e = (void *)(intptr_t) n1;
    if (st_delete_int(tbl, &e, &n2) != 1)
        error++;
    if ((int)(intptr_t) e != n1 || n2 != 3)
        error++;
    if (st_count(tbl) != 0)
        error++;
    for (i = 0; i < 100000; i++) {
        if (st_insert(tbl, (void *)(intptr_t) i, (void *)(intptr_t) i) != 0)
            error++;
    }
    st_foreach_item_int(tbl, gen, &e, &n1) {
        if ((int)(intptr_t) e != n1)
            error++;
    }
    st_free_table(tbl);
    return error;
}


/**
   @brief Tests a table of arrays of ints.

   @return 0 if successful; 1 otherwise.
*/
static int
testArg(void)
{
    size_t const n = 5;
    int error = 0;
    int a1[] = {0,1,2,3,4};
    int a2[] = {4,3,2,1,0};
    int *a3 = a1;
    intptr_t val = 0;
    st_table *tbl = st_init_table_with_arg(array_cmp, array_hash, (void *) n);
    if (!tbl)
        error++;
    if (st_insert(tbl, a1, (void *)(intptr_t) 1) != 0)
        error++;
    if (st_insert(tbl, a2, (void *)(intptr_t) 2) != 0)
        error++;
    if (!st_is_member(tbl, a1))
        error++;
    if (!st_delete(tbl, (void **) &a3, (void **) &val))
        error++;
    if (a3[0] != a1[0] || val != 1)
        error++;
    if (st_is_member(tbl, a1))
        error++;
    if (!st_is_member(tbl, a2))
        error++;
    st_free_table(tbl);
    return error;
}


/**
   @brief Compares two items of type mys_t.

   @return 0 if they compare equal and 1 otherwise.
*/
static int
mys_cmp(void const * key1, void const * key2)
{
    mys_t const *m1 = (mys_t const *) key1;
    mys_t const *m2 = (mys_t const *) key2;

    return m1->b != m2->b || m1->c != m2->c;
}


/**
   @brief Hashes one item of type mys_t.

   @return the hash value.
*/
static int
mys_hash(void const * key, int size)
{
    mys_t const *m = (mys_t const *) key;
    return (int)((((unsigned) m->b >> 4) ^ ((unsigned) m->c >> 5)) % size);
}


/**
   @brief Accumulates the values associated to items of type mys_t.

   @return ST_CONTINUE
*/
static enum st_retval
mys_accm(void * key, void * value, void * arg)
{
    (void) key; /* avoid warning */
    uintptr_t v = (uintptr_t) value;
    uintptr_t * accum = (uintptr_t *) arg;
    *accum += v;
    return ST_CONTINUE;
}


/**
   @brief Compares two arrays of ints.

   @details The length of the two arrays is in `arg`.

   @return 0 if they compare equal and 1 otherwise.
*/
static int
array_cmp(void const * key1, void const * key2, void const *arg)
{
    int const *a1 = (int const *) key1;
    int const *a2 = (int const *) key2;
    size_t const size = (size_t const) arg;
    size_t i;
    for (i = 0; i < size; i++) {
        if (a1[i] != a2[i])
            return 1;
    }
    return 0;
}


/**
   @brief Hashes one array of ints.

   @return the hash value.
*/
static int
array_hash(void const * key, int modulus, void const * arg)
{
    int const *a = (int const *) key;
    size_t const size = (size_t const) arg;
    int val = 0;
    size_t i;
    for (i = 0; i < size; i++) {
        val = val * 997 + a[i];
    }
    return ((val < 0) ? -val : val) % modulus;
}
