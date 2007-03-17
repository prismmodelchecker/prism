#! /bin/sh
CREATE="ln -s"
if test -d include
  then
    :
  else
    mkdir include
    cd include
    $CREATE ../cudd/cudd.h .
    $CREATE ../cudd/cuddInt.h .
    $CREATE ../epd/epd.h .
    $CREATE ../dddmp/dddmp.h .
    $CREATE ../mtr/mtr.h .
    #$CREATE ../obj/cuddObj.hh .
    $CREATE ../st/st.h .
    $CREATE ../util/util.h .
    #$CREATE ../mnemosyne/mnemosyne.h .
    cd ..
fi
if test -d lib
  then
    :
  else
    mkdir lib
    cd lib
    $CREATE ../cudd/libcudd.a .
    $CREATE ../epd/libepd.a .
    $CREATE ../dddmp/libdddmp.a .
    $CREATE ../mtr/libmtr.a .
    $CREATE ../st/libst.a .
    $CREATE ../util/libutil.a .
    cd ..
fi
