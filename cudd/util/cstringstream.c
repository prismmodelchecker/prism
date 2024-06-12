/**
  @file 

  @ingroup cstringstream

  @brief Simple string streams in C.

  @author Fabio Somenzi

  @copyright@parblock
  Copyright (c) 2014-2015, Regents of the University of Colorado

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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "cstringstream.h"

/**
 * @brief Type of a simple extensible string buffer.
 */
struct _cstringstream {
  size_t capacity; /**< elements allocated */
  size_t inUse;    /**< elements currently in use */
  char * data;     /**< actual data */
};

cstringstream newStringStream(void) {
  cstringstream ss;
  ss = (cstringstream) malloc(sizeof(struct _cstringstream));
  if (!ss) return NULL;
  ss->capacity = 1; /* parsimonious */
  ss->inUse = 0;
  ss->data = (char *) malloc(sizeof(char) * ss->capacity);
  if (!ss->data) {
    free(ss);
    return NULL;
  }
  return ss;
}

void deleteStringStream(cstringstream ss) {
  if (ss) {
    free(ss->data);
    free(ss);
  }
}

int clearStringStream(cstringstream ss) {
  if (!ss) return -1;
  ss->inUse = 0;
  return 0;
}

cstringstream copyStringStream(const_cstringstream src) {
  cstringstream dest;
  if (!src) return 0;
  dest = newStringStream();
  if (!dest) return 0;
  if (resizeStringStream(dest, src->inUse)) {
    deleteStringStream(dest);
    return 0;
  }
  strncpy(dest->data, src->data, src->inUse);
  return dest;
}

int resizeStringStream(cstringstream ss, size_t newSize) {
  if (newSize > ss->capacity) {
    /* To avoid too many calls to realloc, we choose the larger of
     * twice the current size and the new requested size. */
    size_t newCapacity = 2 * ss->capacity;
    if (newCapacity < newSize)
      newCapacity = newSize;
    char * tmp = (char *) realloc(ss->data, newCapacity * sizeof(char));
    /* If the allocation fails, leave the array alone. */
    if (!tmp) return -1;
    ss->data = tmp;
    ss->capacity = newCapacity;
  }
  /* Here we are guaranteed that newSize <= ss->capacity. */
  ss->inUse = newSize;
  return 0;
}

int sizeStringStream(const_cstringstream ss, size_t * num) {
  if (!ss || !num) return -1;
  *num = ss->inUse;
  return 0;
}

int getStringStream(const_cstringstream ss, size_t index, char * c) {
  if (!ss || !c || index >= ss->inUse) return -1;
  *c = ss->data[index];
  return 0;
}

int appendCharStringStream(cstringstream ss, char c) {
  if (!ss) return -1;
  if (resizeStringStream(ss, ss->inUse + 1)) return -1;
  /* Now we have space. */
  ss->data[ss->inUse-1] = c;
  return 0;
}

int appendStringStringStream(cstringstream ss, char const * s) {
  if (!ss) return -1;
  size_t len = strlen(s);
  if (resizeStringStream(ss, ss->inUse + len)) return -1;
  /* Now we have space. */
  strncpy(ss->data + ss->inUse - len, s, len); 
  return 0;
}

int appendIntStringStream(cstringstream ss, int d) {
  char str[256];
  if (!ss) return -1;
  sprintf(str, "%d", d);
  return appendStringStringStream(ss, str);
}

int appendUnsignedStringStream(cstringstream ss, unsigned u) {
  char str[256];
  if (!ss) return -1;
  sprintf(str, "%u", u);
  return appendStringStringStream(ss, str);
}

int appendLongStringStream(cstringstream ss, long ld) {
  char str[256];
  if (!ss) return -1;
  sprintf(str, "%ld", ld);
  return appendStringStringStream(ss, str);
}

int appendUnsignedLongStringStream(cstringstream ss, unsigned long lu) {
  char str[256];
  if (!ss) return -1;
  sprintf(str, "%lu", lu);
  return appendStringStringStream(ss, str);
}

int appendDoubleStringStream(cstringstream ss, double g) {
  char str[256];
  if (!ss) return -1;
  sprintf(str, "%g", g);
  return appendStringStringStream(ss, str);
}

int putStringStream(cstringstream ss, size_t index, char c) {
  if (!ss || index >= ss->inUse) return -1;
  ss->data[index] = c;
  return 0;
}

char * stringFromStringStream(const_cstringstream ss) {
  if (!ss) return 0;
  char * str = (char *) malloc(sizeof(char) * (ss->inUse + 1));
  if (!str) return 0;
  strncpy(str, ss->data, ss->inUse);
  str[ss->inUse] = '\0';
  return str;
}
