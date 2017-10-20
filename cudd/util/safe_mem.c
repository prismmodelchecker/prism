/* LINTLIBRARY */

#include "util.h"

/*
 *  These are interface routines to be placed between a program and the
 *  system memory allocator.  
 *
 *  It forces well-defined semantics for several 'borderline' cases:
 *
 *	malloc() of a 0 size object is guaranteed to return something
 *	    which is not 0, and can safely be freed (but not dereferenced)
 *	free() accepts (silently) an 0 pointer
 *	realloc of a 0 pointer is allowed, and is equiv. to malloc()
 *	For the IBM/PC it forces no object > 64K; note that the size argument
 *	    to malloc/realloc is a 'long' to catch this condition
 *
 *  The function pointer MMoutOfMemory() contains a vector to handle a
 *  'out-of-memory' error (which, by default, points at a simple wrap-up 
 *  and exit routine).
 */

#ifdef __cplusplus
extern "C" {
#endif


void (*MMoutOfMemory)(size_t) = MMout_of_memory;

#ifdef __cplusplus
}
#endif


/* MMout_of_memory -- out of memory for lazy people, flush and exit */
void 
MMout_of_memory(size_t size)
{
    (void) fflush(stdout);
    (void) fprintf(stderr,
                   "\nCUDD: out of memory allocating %" PRIszt " bytes\n",
		   (size_t) size);
    exit(1);
}


void *
MMalloc(size_t size)
{
    void *p;

    if ((p = malloc(size)) == NIL(void)) {
	if (MMoutOfMemory != 0 ) (*MMoutOfMemory)(size);
	return NIL(void);
    }
    return p;
}



void *
MMrealloc(void *obj, size_t size)
{
    void *p;

    if ((p = realloc(obj, size)) == NIL(void)) {
	if (MMoutOfMemory != 0 ) (*MMoutOfMemory)(size);
	return NIL(void);
    }
    return p;
}
