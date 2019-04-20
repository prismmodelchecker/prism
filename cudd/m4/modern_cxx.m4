# MODERN_CXX
# ----------
# Check whether the C++ compiler supports enough of C++11.
AC_DEFUN([MODERN_CXX],
[AC_CACHE_CHECK([whether enough of C++11 is supported],
[ac_cv_have_modern_cxx],
[AC_LANG_PUSH([C++])
 AC_COMPILE_IFELSE([AC_LANG_SOURCE([[
class Myclass { explicit operator bool() const { return true; } };
int main() {
  void *p = nullptr;
}]])], [ac_cv_have_modern_cxx=yes], [ac_cv_have_modern_cxx=no])
 AC_LANG_POP([C++])])dnl
])# MODERN_CXX

