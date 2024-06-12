#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.69])
AC_INIT([cudd], [3.0.0], [Fabio@Colorado.EDU])
AC_CONFIG_AUX_DIR([build-aux])
AC_CONFIG_MACRO_DIR([m4])
AC_CANONICAL_HOST
AM_INIT_AUTOMAKE(
  [1.14 -Wall -Werror foreign subdir-objects color-tests silent-rules]
)
AC_REQUIRE_AUX_FILE([tap-driver.sh])

AC_ARG_ENABLE([dddmp],
  [AS_HELP_STRING([--enable-dddmp],[include libdddmp in libcudd])])
AM_CONDITIONAL([DDDMP], [test x$enable_dddmp = xyes])

AC_ARG_ENABLE([obj],
  [AS_HELP_STRING([--enable-obj],[include libobj in libcudd])])
AM_CONDITIONAL([OBJ], [test x$enable_obj = xyes])

AC_ARG_WITH([system-qsort],
  [AS_HELP_STRING([--with-system-qsort],
                  [use system qsort instead of portable one])],
  [],[with_system_qsort=no])
if test x$with_system_qsort != xno ; then
  AC_DEFINE([USE_SYSTEM_QSORT], [1], [Define to 1 to use system qsort])
fi

# Set our own default (instead of "-g -O2") unless CFLAGS is already defined.
: ${CFLAGS="-Wall -Wextra -g -O3"}
: ${CXXFLAGS="-Wall -Wextra -std=c++0x -g -O3"}

AC_PROG_CC
AC_PROG_CXX
AM_PROG_AR
LT_PREREQ([2.4])
LT_INIT([win32-dll disable-shared])

AC_CONFIG_SRCDIR([st/st.c])
AC_CONFIG_HEADERS([config.h])

# Checks for programs.
AC_C_BIGENDIAN
AM_CONDITIONAL([CROSS_COMPILING],[test x$cross_compiling = xyes])

# Building documentation requires doxygen, pdflatex, and makeindex.
AC_CHECK_PROGS([DOXYGEN], [doxygen])
if test -z "$DOXYGEN"; then
   AC_MSG_WARN([Doxygen not found - continuing without Doxygen support])
fi
AM_CONDITIONAL([HAVE_DOXYGEN],[test -n "$DOXYGEN"])
AM_COND_IF([HAVE_DOXYGEN], [AC_CONFIG_FILES([Doxyfile])])

AC_CHECK_PROGS([PDFLATEX], [pdflatex])
if test -z "$PDFLATEX"; then
    AC_MSG_WARN([pdflatex not found - unable to compile manual to PDF])
fi
AC_CHECK_PROGS([MAKEINDEX], [makeindex])
if test -z "$MAKEINDEX"; then
    AC_MSG_WARN([makeindex not found - unable to compile manual to PDF])
fi
AM_CONDITIONAL([HAVE_PDFLATEX],[test -n "$PDFLATEX" && test -n "$MAKEINDEX"])
AM_COND_IF([HAVE_PDFLATEX], [AC_CONFIG_FILES([doc/cudd.tex])])

# Checks for libraries.
#AC_CHECK_LIB([m],[pow])
AC_SEARCH_LIBS([pow],[m])
AC_CHECK_LIB([pthread],[pthread_create],[have_pthreads=yes],[have_pthreads=no])
AM_CONDITIONAL([HAVE_PTHREADS],[test x$have_pthreads = xyes])
# Check for Windows API functions.
AC_SEARCH_LIBS([WSAStartup],[ws2_32])
AC_SEARCH_LIBS([GetProcessMemoryInfo],[psapi])

# Checks for header files.
# First check for mandatory headers...
AC_CHECK_HEADERS([float.h inttypes.h limits.h stddef.h stdlib.h string.h assert.h math.h], [], [have_mandatory_headers=no])
if test "x${have_mandatory_headers}" = xno; then
  AC_MSG_ERROR([One or more mandatory headers missing.  Check 'config.log'.])
fi
# ...then check for optional C headers.
AC_CHECK_HEADERS([unistd.h sys/time.h sys/times.h sys/resource.h sys/wait.h])
# Finally, check C++ optional headers.
AC_MSG_CHECKING([for working C++ thread header])
AC_LANG_PUSH([C++])
AC_COMPILE_IFELSE(
  [AC_LANG_PROGRAM([#include <thread>], [[  std::thread([] {}).join()]])],
  [have_working_thread=yes],[have_working_thread=no])
AC_LANG_POP([C++])
if test x$have_working_thread = xyes ; then
  AC_MSG_RESULT(yes)
  AC_DEFINE([HAVE_WORKING_THREAD], [1], [Define to 1 if C++ thread header is usable])
else
  AC_MSG_RESULT(no)
fi

# Checks for typedefs, structures, and compiler characteristics.
AC_CHECK_HEADER_STDBOOL
AC_C_INLINE
AC_TYPE_SIZE_T
AC_TYPE_UINT16_T
AC_TYPE_UINT32_T
AC_CHECK_TYPES([ptrdiff_t])
AC_CHECK_SIZEOF([int])
AC_CHECK_SIZEOF([long])
AC_CHECK_SIZEOF([void *])
AC_CHECK_SIZEOF([long double])
MINGW_AC_WIN32_NATIVE_HOST
AM_CONDITIONAL([MINGW64], [test x$mingw_cv_win32_host = xyes])
if test x$mingw_cv_win32_host = xyes ; then
  AC_DEFINE([__USE_MINGW_ANSI_STDIO], [1], [Define to 1 to enable C99-compliant printf on MinGW-w64])
fi
MODERN_CXX
if test x$ac_cv_have_modern_cxx = xyes ; then
  AC_DEFINE([HAVE_MODERN_CXX], [1], [Define to 1 if your compiler supports enough C++11])
fi

# Checks for library functions.
# First the mandatory functions...
AC_CHECK_FUNCS([pow sqrt strchr strstr],
  [], [have_mandatory_functions=no])
if test "x${have_mandatory_functions}" = xno; then
  AC_MSG_ERROR([One or more mandatory functions missing.  Check 'config.log'.])
fi
# ...then check for optional functions.   
AC_CHECK_FUNCS([powl gethostname getrlimit getrusage sysconf])

# Check for a working implementation of IEEE 754 floating point
# Specifically, check for correct treatment of +Infinity
AC_MSG_CHECKING([for +Infinity (IEEE 754 floating point)])
AC_CACHE_VAL(ac_cv_have_ieee_754,
[ AC_TRY_RUN([
#include <math.h>
main(void)
{
    if (HUGE_VAL != HUGE_VAL * 3 || HUGE_VAL != HUGE_VAL / 3) return 1;
    return 0;
}
],ac_cv_have_ieee_754=yes,ac_cv_have_ieee_754=no,ac_cv_have_ieee_754=maybe)])
if test x$ac_cv_have_ieee_754 = xmaybe ; then
  AC_COMPILE_IFELSE(
  [AC_LANG_PROGRAM([#include <math.h>], [  double x = INFINITY])],
  [ac_cv_have_ieee_754=yes],[ac_cv_have_ieee_754=no])
fi
if test x$ac_cv_have_ieee_754 = xyes ; then
  AC_MSG_RESULT(yes)
  AC_DEFINE([HAVE_IEEE_754], [1], [Define to 1 if you have working floating-point infinities])
else
  AC_MSG_RESULT(no)
fi


AC_CONFIG_FILES([Makefile])
AC_CONFIG_FILES([dddmp/exp/test1.sh], [chmod +x dddmp/exp/test1.sh])
AC_CONFIG_FILES([dddmp/exp/test2.sh], [chmod +x dddmp/exp/test2.sh])
AC_CONFIG_FILES([dddmp/exp/test3.sh], [chmod +x dddmp/exp/test3.sh])
AC_CONFIG_FILES([dddmp/exp/test4.sh], [chmod +x dddmp/exp/test4.sh])
AC_CONFIG_FILES([dddmp/exp/test5.sh], [chmod +x dddmp/exp/test5.sh])
AC_CONFIG_FILES([dddmp/exp/test6.sh], [chmod +x dddmp/exp/test6.sh])
AC_CONFIG_FILES([dddmp/exp/test7.sh], [chmod +x dddmp/exp/test7.sh])

AC_OUTPUT

echo \
"--------------------------------------------------
Configuration summary for ${PACKAGE_NAME} ${PACKAGE_VERSION}

Build system   : ${build}
Host system    : ${host}
Prefix         : '${prefix}'
Compilers      : '${CC} ${AM_CPPFLAGS} ${CPPFLAGS} ${AM_CFLAGS} ${CFLAGS}'
               : '${CXX} ${AM_CPPFLAGS} ${CPPFLAGS} ${AM_CXXFLAGS} ${CXXFLAGS}'
Shared library : ${enable_shared}
 dddmp enabled : ${enable_dddmp:-no}
 obj enabled   : ${enable_obj:-no}
--------------------------------------------------"
