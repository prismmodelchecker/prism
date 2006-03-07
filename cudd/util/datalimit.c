/* $Id: datalimit.c,v 1.4 2003/08/01 16:25:20 fabio Exp fabio $ */

#ifndef HAVE_SYS_RESOURCE_H
#define HAVE_SYS_RESOURCE_H 1
#endif
#ifndef HAVE_SYS_TIME_H
#define HAVE_SYS_TIME_H 1
#endif
#ifndef HAVE_GETRLIMIT
#define HAVE_GETRLIMIT 1
#endif

#if HAVE_SYS_RESOURCE_H == 1
#if HAVE_SYS_TIME_H == 1
#include <sys/time.h>
#endif
#include <sys/resource.h>
#endif

#ifndef RLIMIT_DATA_DEFAULT
#define RLIMIT_DATA_DEFAULT 67108864	/* assume 64MB by default */
#endif

#ifndef EXTERN
#   ifdef __cplusplus
#	define EXTERN extern "C"
#   else
#	define EXTERN extern
#   endif
#endif

EXTERN long getSoftDataLimit(void);

long
getSoftDataLimit(void)
{
#if HAVE_SYS_RESOURCE_H == 1 && HAVE_GETRLIMIT == 1 && defined(RLIMIT_DATA)
    struct rlimit rl;
    int result;

    result = getrlimit(RLIMIT_DATA, &rl);
    if (result != 0 || rl.rlim_cur == RLIM_INFINITY)
	return((long) RLIMIT_DATA_DEFAULT);
    else
	return((long) rl.rlim_cur);
#else
    return((long) RLIMIT_DATA_DEFAULT);
#endif

} /* end of getSoftDataLimit */
