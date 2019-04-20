/**
  @file 

  @ingroup cstringstream

  @brief Package for simple stringstreams in C.

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

  $Id: cstringstream.h,v 1.1 2015/07/01 20:36:47 fabio Exp fabio $

*/
#ifndef CSTRINGSTREAM_H_
#define CSTRINGSTREAM_H_

#ifdef __cplusplus
extern "C" {
#endif

/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/**
 * @brief Type of a string stream.
 */
typedef struct _cstringstream * cstringstream;

/**
 * @brief Const-qualified version of cstringstream.
 */
typedef struct _cstringstream const * const_cstringstream;

/*---------------------------------------------------------------------------*/
/* Function prototypes                                                       */
/*---------------------------------------------------------------------------*/

/**
 * @brief Returns a new cstringstream with an empty string.
 * @return NULL if creation fails.
 */
cstringstream newStringStream(void);
/**
 * @brief Frees cstringstream ss.
 */
void deleteStringStream(cstringstream ss);
/**
 * @brief Clears the contents of cstringstream ss.
 * @return 0 if succesful and -1 if ss is an invalid pointer.
 */
int clearStringStream(cstringstream ss);
/**
 * @brief Copies cstringstream src to a new cstringstream.
 * @return 0 if succesful or -1 if src is an invalid pointer
 * or memory allocation fails.
 */
cstringstream copyStringStream(const_cstringstream src);
/**
 * @brief Changes the size of cstringstream ss.
 * @return 0 if successful or -1 if resizing fails.
 */
int resizeStringStream(cstringstream ss, size_t newSize);
/**
 * @brief Writes the size of cstringstream ss to the location pointed by num.
 * @return 0 if succesful or -1 if ss is an invalid pointer.
 */
int sizeStringStream(const_cstringstream ss, size_t * num);
/**
 * @brief Writes the i-th element of cstringstream ss to the location
 * pointed by c.
 * @return 0 if successful or -1 otherwise.
 */
int getStringStream(const_cstringstream ss, size_t i, char * c);
/**
 * @brief Adds char c at the end of cstringstream ss.
 * @return 0 if successful or -1 otherwise.
 */
int appendCharStringStream(cstringstream ss, char c);
/**
 * @brief Adds string s at the end of cstringstream ss.
 * @return 0 if successful or -1 otherwise.
 */
int appendStringStringStream(cstringstream ss, char const * s);
/**
 * @brief Adds int d at the end of cstringstream ss.
 * @return 0 if successful or -1 otherwise.
 */
int appendIntStringStream(cstringstream ss, int d);
/**
 * @brief Adds unsigned u at the end of cstringstream ss.
 * @return 0 if successful or -1 otherwise.
 */
int appendUnsignedStringStream(cstringstream ss, unsigned u);
/**
 * @brief Adds long ld at the end of cstringstream ss.
 * @return 0 if successful or -1 otherwise.
 */
int appendLongStringStream(cstringstream ss, long ld);
/**
 * @brief Adds unsigned long lu at the end of cstringstream ss.
 * @return 0 if successful or -1 otherwise.
 */
int appendUnsignedLongStringStream(cstringstream ss, unsigned long lu);
/**
 * @brief Adds double g at the end of cstringstream ss.
 * @return 0 if successful or -1 otherwise.
 */
int appendDoubleStringStream(cstringstream ss, double g);
/**
 * @brief Sets the i-th element of cstringstream ss to c.
 * @return 0 if successful or -1 otherwise.
 *
 * The i-th element of ss must already exist.
 */
int putStringStream(cstringstream ss, size_t index, char c);
/**
 * @brief Returns a NULL-terminated string from the contents of
 * cstringstream ss.
 * @details In case of failure, it returns NULL.
 * The returned string must be freed by the caller.
 */
char * stringFromStringStream(const_cstringstream ss);

#ifdef __cplusplus
}
#endif

#endif
