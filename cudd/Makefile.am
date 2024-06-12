ACLOCAL_AMFLAGS = -I m4

include_HEADERS = cudd/cudd.h
if DDDMP
include_HEADERS += dddmp/dddmp.h
endif
if OBJ
include_HEADERS += cplusplus/cuddObj.hh
endif

check_PROGRAMS =
check_SCRIPTS =
dist_check_DATA =
EXTRA_DIST = README RELEASE.NOTES LICENSE groups.dox
TESTS =
CLEANFILES =
noinst_LTLIBRARIES =
TEST_LOG_DRIVER = env AM_TAP_AWK='$(AWK)' $(SHELL) \
  $(top_srcdir)/build-aux/tap-driver.sh

do_subst = sed \
  -e 's,[@]EXEEXT[@],$(EXEEXT),g' \
  -e 's,[@]srcdir[@],$(srcdir),g'

include $(top_srcdir)/cudd/Included.am
include $(top_srcdir)/util/Included.am
include $(top_srcdir)/st/Included.am
include $(top_srcdir)/epd/Included.am
include $(top_srcdir)/mtr/Included.am
include $(top_srcdir)/dddmp/Included.am
include $(top_srcdir)/cplusplus/Included.am
#include $(top_srcdir)/nanotrav/Included.am
#include $(top_srcdir)/doc/Included.am

dist-hook:
	rm -rf `find $(distdir) -name .svn`

.PHONY : 

all: #html/index.html doc/cudd.pdf

if HAVE_DOXYGEN

html/index.html: Doxyfile $(lib_LTLIBRARIES)
	@if $(AM_V_P); then dest='2>&1'; else dest='> /dev/null 2>&1'; fi; \
	eval "$(DOXYGEN) $< $${dest}"

clean-local:
	rm -rf html doxygen_sqlite3.db

else

html/index.html:

endif

CLEANFILES += $(check_SCRIPTS)
