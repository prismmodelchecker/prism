/**
  @file

  @ingroup util

  @brief Ancient implementation of qsort.

  @details This is shipped with CUDD so that results of reordering may
  be more reproducible across different platforms.

  qsort.c	4.2 (Berkeley) 3/9/83

  Our own version of the system qsort routine which is faster by an average
  of 25%, with lows and highs of 10% and 50%.
  The THRESHold below is the insertion sort threshold, and has been adjusted
  for records of size 48 bytes.
  The MTHREShold is where we stop finding a better median.

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

#ifndef USE_SYSTEM_QSORT
/**
   @brief Threshold for insertion.
*/
#define		THRESH		4
/**
   @brief Threshold for median.
*/
#define		MTHRESH		6

/**
   @brief Miscellaneous information.
*/
typedef struct {
    QSFP	qcmp;			/**< the comparison routine */
    int		qsz;			/**< size of each record */
    int		thresh;			/**< THRESHold in chars */
    int		mthresh;		/**< MTHRESHold in chars */
} info_t;

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

/** \cond */

static	void		qst (char *base, char *max, info_t const * info);

/** \endcond */

/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/

#undef min
#undef max
#endif
/**
 * @brief Implements the quicksort algorithm.
 *
 * @details First, set up some global parameters for qst to share.
 * Then, quicksort with qst(), and then a cleanup insertion sort
 * ourselves.  Sound simple?  It's not...
 */
void
util_qsort(
  void *vbase /**< start address of array */,
  int n       /**< number of items */,
  int size    /**< size of each item */,
  QSFP compar /**< comparison function */)
{
#ifdef USE_SYSTEM_QSORT
    qsort(vbase, n, size, compar);
#else
    char *base = (char *) vbase;
    char c, *i, *j, *lo, *hi;
    char *min, *max;
    info_t info;

    if (n <= 1)
        return;
    info.qsz = size;
    info.qcmp = compar;
    info.thresh = size * THRESH;
    info.mthresh = size * MTHRESH;
    max = base + n * size;
    if (n >= THRESH) {
        qst(base, max, &info);
        hi = base + info.thresh;
    } else {
        hi = max;
    }
    /*
     * First put smallest element, which must be in the first THRESH, in
     * the first position as a sentinel.  This is done just by searching
     * the first THRESH elements (or the first n if n < THRESH), finding
     * the min, and swapping it into the first position.
     */
    for (j = lo = base; (lo += size) < hi; )
        if ((*compar)(j, lo) > 0)
            j = lo;
    if (j != base) {
        /* swap j into place */
        for (i = base, hi = base + size; i < hi; ) {
            c = *j;
            *j++ = *i;
            *i++ = c;
        }
    }
    /*
     * With our sentinel in place, we now run the following hyper-fast
     * insertion sort. For each remaining element, min, from [1] to [n-1],
     * set hi to the index of the element AFTER which this one goes.
     * Then, do the standard insertion sort shift on a character at a time
     * basis for each element in the frob.
     */
    for (min = base; (hi = min += size) < max; ) {
        while ((*compar)(hi -= size, min) > 0)
            /* void */;
        if ((hi += size) != min) {
            for (lo = min + size; --lo >= min; ) {
                c = *lo;
                for (i = j = lo; (j -= size) >= hi; i = j)
                    *i = *j;
                *i = c;
            }
        }
    }
#endif
}


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/

/**
 * @brief Do a quicksort.
 *
 * @details First, find the median element, and put that one in the
 * first place as the discriminator.  (This "median" is just the
 * median of the first, last and middle elements).  (Using this median
 * instead of the first element is a big win).  Then, the usual
 * partitioning/swapping, followed by moving the discriminator into
 * the right place.  Then, figure out the sizes of the two partions,
 * do the smaller one recursively and the larger one via a repeat of
 * this code.  Stopping when there are less than THRESH elements in a
 * partition and cleaning up with an insertion sort (in our caller) is
 * a huge win.  All data swaps are done in-line, which is space-losing
 * but time-saving.  (And there are only three places where this is
 * done).
 */
#ifndef USE_SYSTEM_QSORT
static void
qst(char *base, char *max, info_t const * info)
{
    char c, *i, *j, *jj;
    int ii;
    char *mid, *tmp;
    intptr_t lo, hi;

    /*
     * At the top here, lo is the number of characters of elements in the
     * current partition.  (Which should be max - base).
     * Find the median of the first, last, and middle element and make
     * that the middle element.  Set j to largest of first and middle.
     * If max is larger than that guy, then it's that guy, else compare
     * max with loser of first and take larger.  Things are set up to
     * prefer the middle, then the first in case of ties.
     */
    lo = max - base;		/* number of elements as chars */
    do	{
        mid = i = base + info->qsz * ((lo / info->qsz) >> 1);
        if (lo >= info->mthresh) {
            j = ((*info->qcmp)((jj = base), i) > 0 ? jj : i);
            if ((*info->qcmp)(j, (tmp = max - info->qsz)) > 0) {
                /* switch to first loser */
                j = (j == jj ? i : jj);
                if ((*info->qcmp)(j, tmp) < 0)
                    j = tmp;
            }
            if (j != i) {
                ii = info->qsz;
                do	{
                    c = *i;
                    *i++ = *j;
                    *j++ = c;
                } while (--ii);
            }
        }
        /*
         * Semi-standard quicksort partitioning/swapping
         */
        for (i = base, j = max - info->qsz; ; ) {
            while (i < mid && (*info->qcmp)(i, mid) <= 0)
                i += info->qsz;
            while (j > mid) {
                if ((*info->qcmp)(mid, j) <= 0) {
                    j -= info->qsz;
                    continue;
                }
                tmp = i + info->qsz;	/* value of i after swap */
                if (i == mid) {
                    /* j <-> mid, new mid is j */
                    mid = jj = j;
                } else {
                    /* i <-> j */
                    jj = j;
                    j -= info->qsz;
                }
                goto swap;
            }
            if (i == mid) {
                break;
            } else {
                /* i <-> mid, new mid is i */
                jj = mid;
                tmp = mid = i;	/* value of i after swap */
                j -= info->qsz;
            }
        swap:
            ii = info->qsz;
            do	{
                c = *i;
                *i++ = *jj;
                *jj++ = c;
            } while (--ii);
            i = tmp;
        }
        /*
         * Look at sizes of the two partitions, do the smaller
         * one first by recursion, then do the larger one by
         * making sure lo is its size, base and max are update
         * correctly, and branching back.  But only repeat
         * (recursively or by branching) if the partition is
         * of at least size THRESH.
         */
        i = (j = mid) + info->qsz;
        if ((lo = j - base) <= (hi = max - i)) {
            if (lo >= info->thresh)
                qst(base, j, info);
            base = i;
            lo = hi;
        } else {
            if (hi >= info->thresh)
                qst(i, max, info);
            max = j;
        }
    } while (lo >= info->thresh);
}
#endif
