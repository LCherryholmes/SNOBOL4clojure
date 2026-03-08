* SNOLIB.INC - SNOBOL4+ VERSION
*
* An auxiliary file, SNOLIB.IDX, maintains a table of pointers to
* the functions in this file.
*
* AFTER MAKING ANY ALTERATION TO THIS FILE, BE SURE TO RUN THE BUILDLIB
* PROGRAM TO CREATE A CURRENT VERSION OF SNOLIB.IDX.
*
 DEFINE('ABS(X)')     :(ABS.END)
ABS
       NUMARG( .ABS, 1,   .X)
       ABS = GE(X,0) X    :S(RETURN)
       ABS = -X      :(RETURN)
ABS.END
*
 DEFINE('ACOS(X)K,TERM,T')     :(ACOS.END)
ACOS
       NUMARG( .ACOS, 1,  .X)
       (LT(X,-1) TDUMP( .ACOS,  1))
       (GT(X,1) TDUMP( .ACOS,  1))
       ACOS = LT(X,0) P...I. - ACOS( -X)  :S(RETURN)
       ACOS = 1.0
       TERM = 1.0
       X = DFLOAT(1 - X)
       K = 1
ACOS1
+      TERM = (TERM *  (2 * K - 1) * X) /
+               (4 * K)
       ACOS = ACOS + TERM / (2 *  K + 1)
       K = K + 1
       T = NE(ACOS,T) ACOS     :S(ACOS1)
       ACOS = SQRT(2 * X) * ACOS    :(RETURN)
ACOS.END
*
 DEFINE('ADD(X,Y)')      :(ADD.END)
ADD
       ADD =
+         ( NUMARG(.ADD,1,.X) NUMARG(.ADD,2,.Y) )
+         X + Y           :(RETURN)
ADD.END
*
 DEFINE('ADDPROP(UNAME,VAL,PROP)PLT,LST,ELEM,FLAG')
       :(ADDPROP.END)
ADDPROP
*       UNAME = CONVERT(UNAME,'NAME')     :F(ADDPROP.ERROR1)
       (IDENT(PROP)  TDUMP(.ADDPROP,2))
       ( ~ATOM(PROP) NULL(PROP) TDUMP(.ADDPROP,2))
       VAL = IDENT(VAL) NIL
       ADDPROP = NIL
       FLAG = ''
       PLT = $'   PrOpErTy  LiSt  TaBlE   '
       LST = ITEM(PLT,UNAME)
       IDENT(LST)         :S(ADDPROP2)
       ATOM(LST)          :S(ADDPROP.ERROR2)
       NULL(LST)          :S(ADDPROP2)
ADDPROP1
       ELEM = POP( .LST)            :F(ADDPROP2)
       ATOM(ELEM)         :S(ADDPROP.ERROR2)
       ADDPROP = ~EQUAL(CAR(ELEM),PROP)
+         ELEM ~ ADDPROP            :S(ADDPROP1)
       DIFFER(FLAG)            :S(ADDPROP1)
       FLAG = 1
       ADDPROP = MEMQ(VAL,ELEM)
+         ELEM ~ ADDPROP            :S(ADDPROP1)
       ADDPROP = ?( ~ATOM(VAL) NULL(VAL) )
+         ELEM ~ ADDPROP       :S(ADDPROP1)
       ADDPROP = (PROP ~  VAL ~ CDR(ELEM)) ~ ADDPROP
+         :(ADDPROP1)
ADDPROP2
       ADDPROP = DIFFER(FLAG)
+         LREVERSE(ADDPROP)         :S(ADDPROP4)
       ADDPROP = ?( ~ATOM(VAL) NULL(VAL) )
+         (PROP ~ NIL) ~ LREVERSE(ADDPROP)    :S(ADDPROP4)
       ADDPROP = (PROP ~ VAL ~ NIL) ~ LREVERSE(ADDPROP)
ADDPROP4
       ITEM(PLT,UNAME) = ADDPROP   :(RETURN)
ADDPROP.ERROR1
       TDUMP(.ADDPROP,1)
ADDPROP.ERROR2
       |''
       |'Program error:  In ADDPROP,'
       |'a property list is not a list of lists.'
       |'The offending object is '
       |LST
       |''
       TDUMP(.ADDPROP)
ADDPROP.END
*
 DEFINE('ADD1(X)')        :(ADD1.END)
ADD1
      ADD1 = NUMARG(.ADD1,1,.X)    X + 1           :(RETURN)
ADD1.END
*
 DEFINE('APPEND(LOL)L,A')      :(APPEND.END)
APPEND
      APPEND = LISTARG(.APPEND,1,.LOL)    NIL
APPEND1  L = POP( .LOL)       :F(APPEND3)
       LISTARG( .APPEND,  1, .L)
APPEND2  APPEND = POP( .L) ~ APPEND
+    :S(APPEND2)F(APPEND1)
APPEND3   APPEND = LREVERSE(APPEND)       :(RETURN)
APPEND.END
*
 DEFINE('ARITH(OP,ALIST)A')   :(ARITH.END)
ARITH
       (STRINGARG(.ARITH,1,.OP) LISTARG(.ARITH,2,.ALIST))
       ( NULL(ALIST)    TDUMP( .ARITH, 2) )
       ARITH = POP( .ALIST)
       NUMARG( .ARITH, 2, .ARITH)
ARITH1    A = POP( .ALIST)     :F(RETURN)
       NUMARG( .ARITH, 2, .A)
       ARITH = APPLY(OP,ARITH,A)    :S(ARITH1)
       TDUMP( .ARITH)
ARITH.END
*
 DEFINE('ASIN(X)')        :(ASIN.END)
ASIN
       NUMARG( .AS1N,  1, .X)
       (LT(X,-1) TDUMP(.ASIN,1))
       (GT(X,1) TDUMP(.ASIN, 1))
       ASIN = P...I. / 2 - ACOS(X)   :(RETURN)
ASIN.END
*
 DEFINE('ASSOC(TG,L)C')         :(ASSOC.END)
ASSOC
       ASSOC = LISTARG(.ASSOC,2,.L)    NIL
ASSOC1    C = POP( .L) :F(RETURN)
       LISTARG( .ASSOC, 2, .C)
       ASSOC = EQUAL(TG,CAR(C)) C ~ L
+        :S(RETURN)F(ASSOC1)
ASSOC.END
*
 DEFINE('ASSOCL(LTG,L)A')      :(ASSOCL.END)
ASSOCL
       ASSOCL =
+         ( LISTARG(.ASSOCL,1,.LTG) LISTARG(.ASSOCL,2,.L) )
+         NIL
ASSOCL1
       A = POP( .L)        :F(RETURN)
       LISTARG( .ASSOCL,  2, .A)
       ASSOCL = MEMQ(CAR(A),LTG)
+        A ~ L           :S(RETURN)F(ASSOCL1)
ASSOCL.END
*
 DEFINE('ATAN(X)')        :(ATAN.END)
ATAN
       NUMARG( .ATAN, 1,  .X)
       ATAN = LT(X,0) -ATAN( -X)    :S(RETURN)
       ATAN = ACOS(1 / SQRT(1 + X * X))   :(RETURN)
ATAN.END
*
 DEFINE('ATOMP(A)')       :(ATOMP.END)
ATOMP
       ATOMP = NIL
       ATOMP = ATOM(A) T       :(RETURN)
ATOMP.END
*
 DEFINE('CAAAAR(L)')     :(CAAAAR.END)
CAAAAR
       LISTARG( .CAAAAR,  1, .L)
       CAAAAR =
+        (~ATOM(CAR(L))  ~ATOM(CAR(CAR(L))) ~ATOM(CAR(CAR(CAR(L)))))
+             CAR(CAR(CAR(CAR(L))))     :S(RETURN)
       TDUMP(.CAAAAR, 1)
CAAAAR.END
*
 DEFINE('CAAADR(L)')     :(CAAADR.END)
CAAADR
       LISTARG( .CAAADR,  1, .L)
       CAAADR =
+        (~ATOM(CDR(L))  ~ATOM(CAR(CDR(L))) ~ATOM(CAR(CAR(CDR(L)))))
+             CAR(CAR(CAR(CDR(L))))     :S(RETURN)
       TDUMP(.CAAADR, 1)
CAAADR.END
*
 DEFINE('CAAAR(L)')       :(CAAAR.END)
CAAAR
       LISTARG( .CAAAR, 1, .L)
       ( ATOM(CAR(L)) TDUMP( .CAAAR, 1))
       ( ATOM(CAR(CAR(L))) TDUMP(   .CAAAR, 1))
       CAAAR = CAR(CAR(CAR(L)))     :(RETURN)
CAAAR.END
*
 DEFINE('CAADAR(L)')     :(CAADAR.END)
CAADAR
       LISTARG( .CAADAR,  1, .L)
       CAADAR =
+       (~ATOM(CAR(L)) ~ATOM(CDR(CAR(L))) ~ATOM(CAR(CDR(CAR(L)))))
+            CAR(CAR(CDR(CAR(L))))     :S(RETURN)
       TDUMP(.CAADAR, 1)
CAADAR.END
*
 DEFINE('CAADDR(L)')      :(CAADDR.END)
CAADDR
       LISTARG( .CAADDR, 1,  .L)
       CAADDR =
+       (~ATOM(CDR(L))  ~ATOM(CDR(CDR(L))) ~ATOM(CAR(CDR(CDR(L)))))
+            CAR(CAR(CDR(CDR(L))))     :S(RETURN)
       TDUMP(.CAADDR, 1)
CAADDR.END
*
 DEFINE('CAADR(L)')       :(CAADR.END)
CAADR
       LISTARG(  .CAADR, 1, .L)
       ( ATOM(CDR(L)) TDUMP( .CAADR, 1))
       ( ATOM(CAR(CDR(L))) TDUMP(   .CAADR, 1))
       CAADR = CAR(CAR(CDR(L)))     :(RETURN)
CAADR.END
*
 DEFINE('CAAR(L)')       :(CAAR.END)
CAAR
       LISTARG(  .CAAR, 1, .L)
       ( ATOM(CAR(L)) TDUMP(  .CAAR, 1))
       CAAR =  CAR(CAR(L))      :(RETURN)
CAAR.END
*
 DEFINE('CADAAR(L)')    :(CADAAR.END)
CADAAR
       LISTARG(  .CADAAR, 1, .L)
       CADAAR =
+       (~ATOM(CAR(L)) ~ATOM(CAR(CAR(L))) ~ATOM(CDR(CAR(CAR(L)))))
+            CAR(CDR(CAR(CAR(L))))     :S(RETURN)
       TDUMP( .CADAAR, 1)
CADAAR.END
*
 DEFINE('CADADR(L)')      :(CADADR.END)
CADADR
       LISTARG( .CADADR,   1, .L)
       CADADR =
+       (~ATOM(CDR(L))  ~ATOM(CAR(CDR(L))) ~ATOM(CDR(CAR(CDR(L)))))
+            CAR(CDR(CAR(CDR(L))))     :S(RETURN)
       TDUMP(.CADADR, 1)
CADADR.END
*
 DEFINE('CADAR(L)')       :(CADAR.END)
CADAR
       LISTARG(  .CADAR, 1, .L)
       ( ATOM(CAR(L)) TDUMP(  .CADAR, 1))
       ( ATOM(CDR(CAR(L))) TDUMP(   .CADAR, 1))
       CADAR = CAR(CDR(CAR(L)))     :(RETURN)
CADAR.END
*
 DEFINE('CADDAR(L)')       :(CADDAR.END)
CADDAR
       LISTARG(.CADDAR,1,.L)
       CADDAR =
+         (~ATOM(CAR(L))  ~ATOM(CDR(CAR(L))) ~ATOM(CDR(CDR(CAR(L)))))
+              CAR(CDR(CDR(CAR(L))))     :S(RETURN)
       TDUMP(.CADDAR, 1)
CADDAR.END
*
 DEFINE('CADDDR(L)')       :(CADDDR.END)
CADDDR
       LISTARG(.CADDDR, 1, .L)
       CADDDR =
+         (~ATOM(CDR(L))  ~ATOM(CDR(CDR(L))) ~ATOM(CDR(CDR(CDR(L)))))
+              CAR(CDR(CDR(CDR(L))))     :S(RETURN)
       TDUMP(.CADDDR,1)
CADDDR.END
*
 DEFINE('CADDR(L)')        :(CADDR.END)
CADDR
       LISTARG( .CADDR, 1, .L)
       ( ATOM(CDR(L)) TDUMP( .CADDR,  1))
       ( ATOM(CDR(CDR(L))) TDUMP( .CADDR, 1))
       CADDR = CAR(CDR(CDR(L)))    :(RETURN)
CADDR.END
*
 DEFINE('CADR(L)')       :(CADR.END)
CADR
       LISTARG( .CADR, 1, .L)
       ( ATOM(CDR(L)) TDUMP( .CADR,  1))
       CADR =  CAR(CDR(L))      :(RETURN)
CADR.END
*
 DEFINE('CAL(A)N')        :(CAL.END)
CAL
       (DIFFER('ARRAY',DATATYPE(A)) TDUMP( .CAL, 1))
       CAL = NIL
       N  = PROTOTYPE(A)
       N  = CONVERT(N,'INTEGER')    :S(CAL1)
       TDUMP( .CAL, 1)
CAL1   GT(N,0)        :F(RETURN)
       CAL = A<N> ~ CAL
       N = N -  1          :(CAL1)
CAL.END
*
 DEFINE('CDAAAR(L)')      :(CDAAAR.END)
CDAAAR
       LISTARG( .CDAAAR,  1, .L)
       CDAAAR =
+         (~ATOM(CAR(L)) ~ATOM(CAR(CAR(L))) ~ATOM(CAR(CAR(CAR(L)))))
+             CDR(CAR(CAR(CAR(L))))     :S(RETURN)
       TDUMP(.CDAAAR,1)
CDAAAR.END
*
 DEFINE('CDAADR(L)')      :(CDAADR.END)
CDAADR
       LISTARG(.CDAADR,1,.L)
       CDAADR =
+         (~ATOM(CDR(L)) ~ATOM(CAR(CDR(L))) ~ATOM(CAR(CAR(CDR(L)))))
+             CDR(CAR(CAR(CDR(L))))     :S(RETURN)
       TDUMP(.CDAADR,1)
CDAADR.END
*
 DEFINE('CDAAR(L)')       :(CDAAR.END)
CDAAR
       LISTARG( .CDAAR, 1, .L)
       ( ATOM(CAR(L)) TDUMP( .CDAAR,  1))
       ( ATOM(CAR(CAR(L))) TDUMP( .CDAAR, 1))
       CDAAR = CDR(CAR(CAR(L)))     :(RETURN)
CDAAR.END
*
 DEFINE('CDADAR(L)')      :(CDADAR.END)
CDADAR
       LISTARG(.CDADAR,1,.L)
       CDADAR =
+         (~ATOM(CAR(L)) ~ATOM(CDR(CAR(L))) ~ATOM(CAR(CDR(CAR(L)))))
+             CDR(CAR(CDR(CAR(L))))     :S(RETURN)
       TDUMP(.CDADAR,1)
CDADAR.END
*
 DEFINE('CDADDR(L)')      :(CDADDR.END)
CDADDR
       LISTARG(.CDADDR, 1, .L)
       CDADDR =
+         (~ATOM(CDR(L)) ~ATOM(CDR(CDR(L))) ~ATOM(CAR(CDR(CDR(L)))))
+             CDR(CAR(CDR(CDR(L))))     :S(RETURN)
       TDUMP(.CDADDR,1)
CDADDR.END
*
 DEFINE('CDADR(L)')     :(CDADR.END)
CDADR
       LISTARG( .CDADR, 1, .L)
       ( ATOM(CDR(L)) TDUMP( .CDADR, 1))
       ( ATOM(CAR(CDR(L))) TDUMP( .CDADR, 1))
       CDADR = CDR(CAR(CDR(L)))      :(RETURN)
CDADR.END
*
 DEFINE('CDAR(L)')        :(CDAR.END)
CDAR
       LISTARG( .CDAR, 1, .L)
       ( ATOM(CAR(L)) TDUMP( .CDAR,  1))
       CDAR =  CDR(CAR(L))      :(RETURN)
CDAR.END
*
 DEFINE('CDDAAR(L)')     :(CDDAAR.END)
CDDAAR
       LISTARG(.CDDAAR,1,.L)
       CDDAAR =
+         (~ATOM(CAR(L)) ~ATOM(CAR(CAR(L))) ~ATOM(CDR(CAR(CAR(L)))))
+              CDR(CDR(CAR(CAR(L))))     :S(RETURN)
       TDUMP(.CDDAAR,1)
CDDAAR.END
*
 DEFINE('CDDADR(L)')      :(CDDADR.END)
CDDADR
       LISTARG(.CDDADR, 1, .L)
       CDDADR =
+         (~ATOM(CDR(L)) ~ATOM(CAR(CDR(L))) ~ATOM(CDR(CAR(CDR(L)))))
+              CDR(CDR(CAR(CDR(L))))     :S(RETURN)
       TDUMP(.CDDADR,1)
CDDADR.END
*
 DEFINE('CDDAR(L)')      :(CDDAR.END)
CDDAR
       LISTARG( .CDDAR, 1, .L)
       ( ATOM(CAR(L)) TDUMP( .CDDAR, 1))
       ( ATOM(CDR(CAR(L))) TDUMP( .CDDAR, 1))
       CDDAR = CDR(CDR(CAR(L)))     :(RETURN)
CDDAR.END
*
 DEFINE('CDDDAR(L)')     :(CDDDAR.END)
CDDDAR
       LISTARG(.CDDDAR, 1, .L)
       CDDDAR =
+         (~ATOM(CAR(L)) ~ATOM(CDR(CAR(L))) ~ATOM(CDR(CDR(CAR(L)))))
+              CDR(CDR(CDR(CAR(L))))     :S(RETURN)
       TDUMP(.CDDDAR,1)
CDDDAR.END
*
 DEFINE('CDDDDR(L)')      :(CDDDDR.END)
CDDDDR
       LISTARG(.CDDDDR,1,.L)
       CDDDDR =
+         (~ATOM(CDR(L)) ~ATOM(CDR(CDR(L))) ~ATOM(CDR(CDR(CDR(L)))))
+              CDR(CDR(CDR(CDR(L))))     :S(RETURN)
       TDUMP(.CDDDDR,1)
CDDDDR.END
*
 DEFINE('CDDDR(L)')       :(CDDDR.END)
CDDDR
       LISTARG( .CDDDR, 1, .L)
       ( ATOM(CDR(L)) TDUMP( .CDDDR, 1))
       ( ATOM(CDR(CDR(L))) TDUMP( .CDDDR, 1))
       CDDDR = CDR(CDR(CDR(L)))     :(RETURN)
CDDDR.END
*
 DEFINE('CDDR(L)')        :(CDDR.END)
CDDR
       LISTARG( .CDDR, 1, .L)
       ( ATOM(CDR(L)) TDUMP( .CDDR, 1))
       CDDR = CDR(CDR(L))      :(RETURN)
CDDR.END
*
 DEFINE('CEIL(X)')       :(CEIL.END)
CEIL
       NUMARG( .CEIL, 1, .X)
       CEIL = -FLOOR( -X) :(RETURN)
CEIL.END
*
 DEFINE('CLA(L)N')        :(CLA.END)
CLA
       N = LISTARG(.CLA,1,.L)   LENGTH(L)
       ( LE(N,0)   TDUMP( .CLA, 1)  )
       CLA = ARRAY(N)
       N = 1
CLA1  CLA<N> = POP( .L)        :F(RETURN)
       N = N + 1     :(CLA1)
CLA.END
*
 DEFINE('CLOG(X)FACTOR,T,K')        :(CLOG.END)
CLOG
       NUMARG( .CLOG, 1, .X)
       (LE(X,0) TDUMP( .CLOG, 1))
       CLOG = LN(X) / LN...10.   :(RETURN)
CLOG.END
*
 DEFINE('COS(A,S)K')         :(COS.END)
COS
       ( NUMARG(.COS,1,.A) DIFFER(S) NUMARG(.COS,2,.S) )
       (LT(S, -1) TDUMP( .COS, 2))
       (GT(S, 1) TDUMP( .COS, 2))
       COS = LT(A,0) COS( -A, S)   :S(RETURN)
       COS = LT(A, 2 * P...I.) COS.( A, S)    :S(RETURN)
       K = FIX( A / (2 * P...I.))
       COS = COS.( A - K  * 2  * P...I., S)   :(RETURN)
COS.
       S = IDENT(S)  SIN(A)
       COS. = SQRT( 1  - S * S)
       P2 = P...I. / 2
       COS. = (GT(A,P2) LT(A,3 * P2)) -COS.   :(RETURN)
COS.END
*
 DEFINE('DEFPROP(A1,EXP,A2)')       :(DEFPROP.END)
DEFPROP
       DEFPROP = PUT(A1,A2,EXP)     :(RETURN)
DEFPROP.END
*
 DEFINE('DEG(R)')    :(DEG.END)
DEG
       DEG = NUMARG(.DEG,1,.R)   R * 57.2957795131        :(RETURN)
DEG.END
*
 DEFINE('DFLOAT(N)')      :(DFLOAT.END)
DFLOAT
       NUMARG( .DFLOAT, 1, .N)
       DFLOAT = CONVERT(N,"REAL")   :(RETURN)
DFLOAT.END
*
 DEFINE('DIFFERENCE(L)')       :(DIFFERENCE.END)
DIFFERENCE
       DIFFERENCE =  LISTARG(.DIFFERENCE,1,.L)
+         ARITH(.SUB,L)        :(RETURN)
DIFFERENCE.END
*
 DEFINE('DIV(X,Y)')       :(DIV.END)
DIV
       (NUMARG(.DIV,1,.X) NUMARG(.DIV,2,.Y))
*      (EQ(Y,0) TDUMP(.DIV,2))      :S(FRETURN)
       DIV = DFLOAT(X) /  DFLOAT(Y)      :(RETURN)
DIV.END
*
 DEFINE('EQP(A1,A2)')       :(EQP.END)
EQP
       EQP = NIL
       EQP = EQU(A1,A2) T      :(RETURN)
EQP.END
*
 DEFINE('EQU(A1,A2)')     :(EQU.END)
EQU    IDENT(A1,A2)       :S(RETURN)
       ( ATOM(A1)    ATOM(A2) )          :F(FRETURN)
       ( NUMBER(A1)  NUMBER(A2) )        :F(EQU1)
       EQ(A1,A2)               :S(RETURN)F(FRETURN)
EQU1   LEQ(A1,A2)              :S(RETURN)F(FRETURN)
EQU.END
*
 DEFINE('EQUAL(X,Y)')   :(EQUAL.END)
EQUAL  EQU(X,Y)      :S(RETURN)
       ATOM(X)                 :S(FRETURN)
       ATOM(Y)                 :S(FRETURN)
       EQUAL(CAR(X),CAR(Y))    :F(FRETURN)
       EQUAL(CDR(X),CDR(Y))         :S(RETURN)F(FRETURN)
EQUAL.END
*
 DEFINE('EQUALP(A1,A2)')       :(EQUALP.END)
EQUALP
       EQUALP = NIL
       EQUALP = EQUAL(A1,A2) T      :(RETURN)
EQUALP.END
*
 DEFINE('EVALCODE(S)')   :(EVALCODE.END)
EVALCODE
       S = CONVERT(S,"EXPRESSION")    :F(EVALCODE1)
       EVALCODE = EVAL(S)      :S(RETURN)F(FRETURN)
EVALCODE1
       TDUMP('EVALCODE',1)
EVALCODE.END
*
 DEFINE('EVERY(FN,L)A,V')   :(EVERY.END)
EVERY
       EVERY =
+         (STRINGARG(.EVERY,1,.FN) LISTARG(.EVERY,2,.L))
+         T
EVERY1  A = POP( .L)   :F(RETURN)
       %APPLY(FN,A)       :S(EVERY1)
       EVERY = NIL  :(RETURN)
EVERY.END
*
 DEFINE('EVLIS(EV...L.)EV...T.')         :(EVLIS.END)
EVLIS
       EVLIS = LISTARG( .EVLIS, 1, .EV...L. )   NIL
EVLIS1
       EV...T.  = POP( .EV...L. )        :F(EVLIS2)
       EVLIS = $EV...T. ~ EVLIS          :(EVLIS1)
EVLIS2
       EVLIS = LREVERSE(EVLIS)           :(RETURN)
EVLIS.END
*
 DEFINE('EXCLUDE(L,XCL)A')   :(EXCLUDE.END)
EXCLUDE
       EXCLUDE =
+        (LISTARG(.EXCLUDE,1,.L) LISTARG(.EXCLUDE,2,.XCL))
+        NIL
EXCLUDE1      A = POP( .L)  :F(EXCLUDE2)
       EXCLUDE  = ~MEMQ(A,XCL) INSERT(A,EXCLUDE)     :(EXCLUDE1)
EXCLUDE2      EXCLUDE = LREVERSE(EXCLUDE)     :(RETURN)
EXCLUDE.END
*
 DEFINE('EXPLODE(A)CH')       :(EXPLODE.END)
EXPLODE
       EXPLODE = NIL
       A = ~ATOM(A) UNREAD(A)
       A = REVERSE(A)       :F(EXPLODE2)
EXPLODE1
       A LEN(1) . CH =    :F(RETURN)
       EXPLODE = LIST(CH,EXPLODE)    :(EXPLODE1)
EXPLODE2
       TDUMP( .EXPLODE, 1)
EXPLODE.END
*
 DEFINE('FIND(TG,L)')    :(FIND.END)
FIND
       ATOM(L)       :F(FIND1)
       FIND = EQU(L,TG) L      :S(RETURN)
       FIND = NIL    :(RETURN)
FIND1  FIND = NULL(L) NIL  :S(RETURN)
       FIND = EQUAL(L,TG) L    :S(RETURN)
       FIND = /FIND(TG,CAR(L))       :S(RETURN)
       FIND = FIND(TG,CDR(L))        :(RETURN)
FIND.END
*
 DEFINE('FIX(X)')   :(FIX.END)
FIX
       FIX = NUMARG(.FIX,1,.X) CONVERT(X,'INTEGER')
+         :S(RETURN)F(FRETURN)
FIX.END
*
 DEFINE('FLOAT(N)')      :(FLOAT.END)
FLOAT
       FLOAT = NUMARG(.FLOAT,1,.N)   CONVERT(N,'REAL')    :(RETURN)
FLOAT.END
*
 DEFINE('FLOOR(X)')      :(FLOOR.END)
FLOOR
       NUMARG( .FLOOR, 1, .X)
       FLOOR = FIX(X)
       GE(X)    :S(RETURN)
       FLOOR = NE(X,FLOOR) FLOOR - 1     :(RETURN)
FLOOR.END
*
 DEFINE('GENSYM()')      :(GENSYM.END)
GENSYM
+      GENSYM = 'GSYM' STATEMENTS(0)
       IDENT($GENSYM)     :S(RETURN)F(GENSYM)
GENSYM.END
*
 DEFINE('GET(UNAME,PROP)PLT,LST,ELEM')       :(GET.END)
GET
*       UNAME = CONVERT(UNAME,'NAME')    :F(GET.ERROR1)
       (IDENT(PROP) TDUMP(.GET,2))
       ( ~ATOM(PROP) NULL(PROP) TDUMP(.GET,2) )
       GET = NIL
       PLT = $'   PrOpErTy  LiSt  TaBlE   '
       LST = ITEM(PLT,UNAME)
       IDENT(LST)         :S(RETURN)
       ATOM(LST)          :S(GET.ERROR2)
       NULL(LST)          :S(RETURN)
GET1
       ELEM = POP( .LST)            :F(RETURN)
       ATOM(ELEM)         :S(GET.ERROR2)
       GET = EQUAL(CAR(ELEM),PROP)
+         CDR(ELEM)             :S(RETURN)F(GET1)
GET.ERROR1
       TDUMP(.GET,1)
GET.ERROR2
       |''
       |'Program error:  In GET,'
       |'a property list is not a list of lists.'
       |'The offending object is'
       |LST
       |''
       TDUMP(.GET)
GET.END
*
 DEFINE('GETL(UNAME,LPROP)PLT,LST,ELEM')       :(GETL.END)
GETL
*       UNAME = CONVERT(UNAME,'NAME')     :F(GETL.ERROR1)
       LISTARG( .GETL, 2, .LPROP)
       GETL = NIL
       PLT = $'   PrOpErTy  LiSt  TaBlE   '
       LST = ITEM(PLT,UNAME)
       IDENT(LST)         :S(RETURN)
       ATOM(LST)          :S(GETL.ERROR2)
       NULL(LST)          :S(RETURN)
GETL1
       ELEM = POP( .LST)            :F(RETURN)
       ATOM(ELEM)         :S(GETL.ERROR2)
       GETL = MEMQ(CAR(ELEM),LPROP)
+         ELEM ~ LST             :S(RETURN)F(GETL1)
GETL.ERROR1
       TDUMP(.GETL,1)
GETL.ERROR2
       |''
       |'Program error:   In GETL,'
       |'a property list is not a list of lists.'
       |'The offending object is'
       |LST
       |''
       TDUMP(.GETL)
GETL.END
*
 DEFINE('GETPROP(UNAME,PROP)PLT,LST,ELEM,FLAG,NEW') :(GETPROP.END)
GETPROP
*       UNAME = CONVERT(UNAME,'NAME')    :F(GETPROP.ERROR1)
       (IDENT(PROP) TDUMP(.GETPROP,2))
       ( ~ATOM(PROP) NULL(PROP) TDUMP(.GETPROP,2) )
       PLT = $'   PrOpErTy  LiSt  TaBlE   '
       GETPROP = NIL
       FLAG = ''
       LST = ITEM(PLT,UNAME)
       IDENT(LST)         :S(RETURN)
       ATOM(LST)          :S(GETPROP.ERROR2)
       NULL(LST)          :S(RETURN)
       NEW = NIL
GETPROP1
       ELEM = POP( .LST)            :F(GETPROP2)
       ATOM(ELEM)         :S(GETPROP.ERROR2)
       NEW = ~EQUAL(CAR(ELEM),PROP)
+         ELEM ~ NEW      :S(GETPROP1)
       DIFFER(FLAG)       :S(GETPROP1)
       FLAG = 1
       ELEM = CDR(ELEM)
       GETPROP = POP( .ELEM)
       NEW = (PROP ~ ELEM) ~ NEW    :(GETPROP1)
GETPROP2
       ITEM(PLT,UNAME) = LREVERSE(NEW)  :(RETURN)
GETPROP.ERROR1
       TDUMP(.GETPROP,1)
GETPROP.ERROR2
       |''
       |'Program error:   In GETPROP,'
       |'a property list is not a list of lists.'
       |'The offending object is'
       |LST
       |''
       TDUMP(.GETPROP)
GETPROP.END
*
 DEFINE('GREATER(L)A,B')       :(GREATER.END)
GREATER
       LISTARG( .GREATER, 1, .L)
       A = POP( .L)       :F(RETURN)
       NUMARG( .GREATER,  1, .A)
       B = POP( .L)       :F(RETURN)
       NUMARG( .GREATER,  1, .B)
GREATER1     GT(A,B)      :F(FRETURN)
       A = B
       B = POP( .L)       :F(RETURN)
       NUMARG( .GREATER,  1, .B)    :S(GREATER1)
GREATER.END
*
 DEFINE('GREATERP(L)')    :(GREATERP.END)
GREATERP
       GREATERP =
+         (LISTARG(.GREATERP,1,.L)  GREATER(L))
+         T          :S(RETURN)
       GREATERP = NIL          :(RETURN)
GREATERP.END
*
 DEFINE('INSERT(S,L)')    :(INSERT.END)
INSERT
       LISTARG(.INSERT,2,.L)
       INSERT = MEMQ(S,L) L    :S(RETURN)
       INSERT = S ~ L          :(RETURN)
INSERT.END
*
 DEFINE('INTERSECT(L1,L2)L,A')     :(INTERSECT.END)
INTERSECT
       INTERSECT =
+         (LISTARG(.INTERSECT,1,.L1) LISTARG(.INTERSECT,2,.L2))
+         NIL
INTERSECT1    A = POP( .L1)      :F(INTERSECT2)
       INTERSECT = MEMQ(A,L2) INSERT(A,INTERSECT)      :(INTERSECT1)
INTERSECT2     INTERSECT = LREVERSE(INTERSECT)     :(RETURN)
INTERSECT.END
*
 DEFINE('LAST(L)')        :(LAST.END)
LAST
       LISTARG( .LAST, 1, .L)
       LAST = NULL(L) NIL      :S(RETURN)
       LAST = ATOM( CDR(L)) L       :S(RETURN)
       LAST = NULL( CDR(L)) L       :S(RETURN)
       L = CDR(L)    :(LAST)
LAST.END
*
 DEFINE('LCOPY(L)CA,CD')       :(LCOPY.END)
LCOPY
       LCOPY = ATOM(L) L       :S(RETURN)
       LCOPY = NULL(L) NIL     :S(RETURN)
       LCOPY = EQUAL(L,T) T    :S(RETURN)
       CA = LCOPY(CAR(L))
       CD = LCOPY(CDR(L))
       LCOPY = CA ~ CD         :(RETURN)
LCOPY.END
*
 DEFINE('LENGTH(L)')     :(LENGTH.END)
LENGTH    LENGTH = ATOM(L) SIZE(L)        :S(RETURN)
       LENGTH = 0
LENGTH1  LENGTH = ?POP( .L)    LENGTH + 1
+      :S(LENGTH1)F(RETURN)
LENGTH.END
*
 DEFINE('LESS(L)A,B')     :(LESS.END)
LESS
       LISTARG( .LESS, 1, .L)
       A = POP( .L)        :F(RETURN)
       NUMARG( .LESS, 1, .A)
       B = POP( .L)        :F(RETURN)
       NUMARG( .LESS, 1, .B)
LESS1     LT(A,B)    :F(FRETURN)
       A = B
       B = POP( .L)      :F(RETURN)
       NUMARG( .LESS, 1, .B)  :S(LESS1)
LESS.END
*
 DEFINE('LESSP(L)')       :(LESSP.END)
LESSP
       LISTARG( .LESSP, 1, .L)
       LESSP = NIL
       LESSP = LESS(L) T       :(RETURN)
LESSP.END
*
 DEFINE('LOG(X,B)')      :(LOG.END)
LOG
       NUMARG(.LOG,1,.X)
       (DIFFER(B) NUMARG(.LOG,2,.B))
       (LE(X,0) TDUMP(.LOG,1))
       (LT(B,0) TDUMP(.LOG,2))
       (EQ(B,1) TDUMP(.LOG,2))
       LOG = NE(B) LN(X) / LN(B)       :S(RETURN)
       LOG = EQ(B) LN(X)                  :(RETURN)
LOG.END
*
 DEFINE('LREVERSE(LST)')      :(LREVERSE.END)
LREVERSE
       LREVERSE = LISTARG(.LREVERSE,1,.LST)   NIL
LREVERSE1 LREVERSE = POP( .LST) ~ LREVERSE
+      :S(LREVERSE1)F(RETURN)
LREVERSE.END
*
 DEFINE('LTRACE(PARAM,L)F,TFNAME')      :(LTRACE.END)
LTRACE
       L = IDENT(L) PARAM
       PARAM = ~INTEGER(PARAM) 3
       INTARG( .LTRACE, 1, .PARAM)
       LISTARG( .LTRACE, 2, .L)
       F = POP( .L)       :F(RETURN)
       STRINGARG( .LTRACE, 2, .F)
       F POS(0) 'LAMBDA'     :S(LTRACE)
       F POS(0)
+         (
+             'LTRACE' |
+             'LTRACE1' |
+             'POP' |
+             'PRT.VIA.OUTPUT' |
+             'PRINT' |
+             'ATOM' |
+             ('C' SPAN('AD') 'R') |
+             'TDUMP' |
+             'INTARG' |
+             'NUMARG' |
+             'LISTARG' |
+             'STRINGARG' |
+             'PRINT.IN.FIELD' |
+             'UNREAD' |
+             'NULL' |
+             'UNCONS' |
+             'IN' |
+             'CONCAT' |
+             'MAPCAR' |
+             'LIST' |
+             'UNREAD.NIL' |
+             'UNREAD.DOTPAIR' |
+             'UNREAD.SINGLETON' |
+             'UNREAD.REGULAR' |
+             'UNREAD.ATOM'
+         ) RPOS(0)            :S(LTRACE)
       ( EQ(PARAM,0) STOPTR(F,'CALL') STOPTR(F,'RETURN') )    :S(LTRACE)
LTRACE.A
       TRACE(F,"CALL",,
+         DEXP('LAMBDA() = LTRACE1(.' F ',"CALL",' PARAM ')'))
       TRACE(F,"RETURN",,
+         DEXP('LAMBDA() = LTRACE1(.' F ',"RETURN",' PARAM ')'))
+         :(LTRACE)
*
LTRACE1
       IDENT(LTRACE1...T.,"RETURN")    :S(LTRACE1.B)
       |""
       |(">>> " &LASTNO " ==> " &STNO " ==> " )
       |(" " LTRACE1...F.)
       LTRACE1...I. = 1
LTRACE1.A     LTRACE1...N. = ARG(LTRACE1...F.,LTRACE1...I.)  :F(LTRACE1.F)
       |(5 % " " LTRACE1...N. " = ")
       |(7 % " " !($LTRACE1...N.))
       LTRACE1...I. = LTRACE1...I. + 1     :(LTRACE1.A)
LTRACE1.B |""
       |("<<< " &RTNTYPE " <== " &STNO " <== " &LASTNO)
       |(" " LTRACE1...F. " = ")
       |(5 % " " !($LTRACE1...F.))     LE(LTRACE1...L.,1)   :S(RETURN)
       LTRACE1...I. = 1
LTRACE1.C     LTRACE1...N. = ARG(LTRACE1...F.,LTRACE1...I.)  :F(LTRACE1.D)
       LEQ(LTRACE1...N.,LTRACE1...F.)      :S(LTRACE1.C)
       |(5 % " " LTRACE1...N. " = ")
       |(7 % " " !($LTRACE1...N.))
       LTRACE1...I. = LTRACE1...I. + 1     :(LTRACE1.C)
LTRACE1.D LTRACE1...I. = 1
LTRACE1.E LTRACE1...N. = LOCAL(LTRACE1...F.,LTRACE1...I.)  :F(LTRACE1.F)
       LEQ(LTRACE1...N.,LTRACE1...F.)      :S(LTRACE1.E)
       |(5 % " " LTRACE1...N. " = ")
       |(7 % " " !($LTRACE1...N.))
       LTRACE1...I. = LTRACE1...I. + 1     :(LTRACE1.E)
LTRACE1.F      ( GE(LTRACE1...L.,3) ?EVAL(IN()) )      :(RETURN)
LTRACE.END
*
 DEFINE('MAP(FN,L)')     :(MAP.END)
MAP
       MAP =
+         (STRINGARG(.MAP,1,.FN) LISTARG(.MAP,2,.L))
+         NIL
MAP1   NULL(L)       :S(RETURN)
       APPLY(FN,L)   :F(FRETURN)
       L = CDR(L)    :(MAP1)
MAP.END
*
 DEFINE('MAPC(FN,L)')     :(MAPC.END)
MAPC
       MAPC =
+         (STRINGARG(.MAPC,1,.FN) LISTARG(.MAPC,2,.L))
+         NIL
MAPC1     NULL(L)    :S(RETURN)
       APPLY(FN, POP( .L))   :F(FRETURN)S(MAPC1)
MAPC.END
*
 DEFINE('MAPCAN(FN,L)')       :(MAPCAN.END)
MAPCAN
       (STRINGARG(.MAPCAN,1,.FN) LISTARG(.MAPCAN,2,.L))
       MAPCAN = NCONC(MAPCAR(FN,L))     :(RETURN)
MAPCAN.END
*
 DEFINE('MAPCON(FN,L)')       :(MAPCON.END)
MAPCON
       MAPCON  = NCONC(MAPLIST(FN,L))     :(RETURN)
MAPCON.END
*
 DEFINE('MAPLIST(FN,L)R')     :(MAPLIST.END)
MAPLIST
       MAPLIST =
+      (STRINGARG(.MAPLIST,1,.FN) LISTARG(.MAPLIST,2,.L))
+      NIL
MAPLIST1       NULL(L)    :S(MAPLIST2)
       R  = APPLY(FN,L)     :F(FRETURN)
       MAPLIST = R ~ MAPLIST
       L  = CDR(L)   :(MAPLIST1)
MAPLIST2       MAPLIST = LREVERSE(MAPLIST)     :(RETURN)
MAPLIST.END
*
 DEFINE('MAX(X,Y)')       :(MAX.END)
MAX
       (NUMARG(.MAX,1,.X) NUMARG(.MAX,2,.Y))
       MAX = GE(X,Y) X    :S(RETURN)
       MAX = Y       :(RETURN)
MAX.END
*
 DEFINE('MEMBER(A,MBR)') :(MEMBER.END)
MEMBER
       MEMBER = LISTARG(.MEMBER,2,.MBR)    NIL
MEMBER1  EQUAL(A,CAR(MBR))    :S(MEMBER2)
       POP( .MBR)   :S(MEMBER1)F(RETURN)
MEMBER2   MEMBER = MBR    :(RETURN)
MEMBER.END
*
 DEFINE('MEMQ(A,L)')     :(MEMQ.END)
MEMQ
       (LISTARG( .MEMQ, 2, .L)
+      %MEMBER(A,L))      :S(RETURN)F(FRETURN)
MEMQ.END
*
 DEFINE('MIN(X,Y)')       :(MIN.END)
MIN
       (NUMARG(.MIN,1,.X) NUMARG(.MIN,2,.Y))
       MIN = LE(X,Y) X    :S(RETURN)
       MIN = Y       :(RETURN)
MIN.END
*
 DEFINE('MINUS(X)')     :(MINUS.END)
MINUS
       MINUS = NUMARG(.MINUS,1,.X)   -X       :(RETURN)
MINUS.END
*
 DEFINE('MULT(X,Y)')      :(MULT.END)
MULT
       MULT =
+         (NUMARG(.MULT,1,.X)  NUMARG(.MULT,2,.Y))
+         X * Y           :(RETURN)
MULT.END
*
 DEFINE('NCONC(LOL)LN,L')      :(NCONC.END)
NCONC
       NCONC = LISTARG(.NCONC,1,.LOL)   NIL
NCONC1
       NCONC = POP( .LOL)     :F(RETURN)
       LISTARG( .NCONC, 1, .NCONC)
       LN = ~NULL(LOL) LAST(NCONC)       :F(RETURN)
       NULL(LN)      :S(NCONC1)
NCONC2    L = POP( .LOL)
       LISTARG( .NCONC, 1, .L)
       (~NULL(L)   %RPLACD(LN,L))    :F(NCONC2)
       LN = ~NULL(LOL) LAST(L)      :S(NCONC2)F(RETURN)
NCONC.END
*
 DEFINE('NEG(X)')   :(NEG.END)
NEG
       (NUMARG(.NEG,1,.X) LT(X,0))
+          :S(RETURN)F(FRETURN)
NEG.END
*
 DEFINE('NEGP(X)')        :(NEGP.END)
NEGP
       NEGP = (NUMARG(.NEGP,1,.X) NEG(X))   T      :S(RETURN)
       NEGP = NIL         :(RETURN)
NEGP.END
*
 DEFINE('NTH(L,N)I')      :(NTH.END)
NTH
       (LISTARG(.NTH,1,.L) INTARG(.NTH,2,.N))
       NTH = NEG(N) NTH(L,LENGTH(L) + N + 1)       :S(RETURN)
       NTH = GT(N,LENGTH(L)) NIL    :S(RETURN)
       NTH = L
       I = 1
NTH1   I = LT(I,N) I + 1       :F(RETURN)
       NTH = CDR(NTH) :(NTH1)
NTH.END
*
 DEFINE('NULLP(A)')       :(NULLP.END)
NULLP
       NULLP = (LISTARG(.NULLP,1,.A) NULL(A))  T       :S(RETURN)
       NULLP = NIL        :(RETURN)
NULLP.END
*
 DEFINE('NUMBERP(A)')     :(NUMBERP.END)
NUMBERP
       NUMBERP = NUMBER(A) T        :S(RETURN)
       NUMBERP = NIL          :(RETURN)
NUMBERP.END
*
 DEFINE('PLUS(L)')        :(PLUS.END)
PLUS
       PLUS = LISTARG(.PLUS,1,.L)
+         ARITH(.ADD,L)        :(RETURN)
PLUS.END
*
 DEFINE('PRELIST(L,N)')        :(PRELIST.END)
PRELIST
       (LISTARG(.PRELIST,1,.L) INTARG(.PRELIST,2,.N))
       PRELIST = LREVERSE(SUFLIST(LREVERSE(L),-N))
+          :(RETURN)
PRELIST.END
*
 DEFINE('PRINT.IN.FIELD(PIF...N.,PIF...S.)'
+      'PIF...C.,PIF...V.')
       :(PRINT.IN.FIELD.END)
PRINT.IN.FIELD
       PIF...N. = CONVERT( PIF...N., 'INTEGER' )
+         :F(PRINT.IN.FIELD.ERROR1)
       ATOM(PIF...S.)     :S(PRINT.IN.FIELD1)
       PIF...S. = UNREAD(PIF...S.)
+         :F(PRINT.IN.FIELD.ERROR2)
PRINT.IN.FIELD1
       PIF...S. = CONVERT( PIF...S., 'STRING' )
+         :F(PRINT.IN.FIELD.ERROR2)
       PIF...S.   POS(0)  (SPAN(' ')  | '')
+                 ANY('LCR') . PIF...C.   '.'   =
+                 :S(PRINT.IN.FIELD2)
       PRINT.IN.FIELD = DUPL( PIF...S., PIF...N. )
+         :(RETURN)
PRINT.IN.FIELD2
       PIF...S. = CONVERT( PIF...S., 'EXPRESSION' )
+         :F(PRINT.IN.FIELD.ERROR3)
       PIF...V. = EVAL( PIF...S. )
+          :F(PRINT.IN.FIELD.ERROR3)
       ATOM(PIF...V.)     :S(PRINT.IN.FIELD.BRANCH)
       PIF...V. = UNREAD(PIF...V.)
+         :F(PRINT.IN.FIELD.ERROR4)
PRINT.IN.FIELD.BRANCH
          :( $('PRINT.IN.FIELD.' PIF...C.) )
PRINT.IN.FIELD.L
       PRINT.IN.FIELD = GT(PIF...N.,SIZE(PIF...V.))
+         RPAD(PIF...V., PIF...N.)
+             :S(RETURN)F(PRINT.IN.FIELD3)
PRINT.IN.FIELD.R
       PRINT.IN.FIELD = GT(PIF...N.,SIZE(PIF...V.))
+         LPAD(PIF...V.,  PIF...N.)
+              :S(RETURN)F(PRINT.IN.FIELD3)
PRINT.IN.FIELD.C
       PRINT.IN.FIELD = GT(PIF...N.,SIZE(PIF...V.))
+         RPAD(LPAD(PIF...V.,
+          PIF...N. - FIX((PIF...N. - SIZE(PIF...V.)) / 2)),
+           PIF...N.)
+              :S(RETURN)
PRINT.IN.FIELD3
       PRINT.IN.FIELD = PIF...V.    :(RETURN)
PRINT.IN.FIELD.ERROR1
       |'In PRINT.IN.FIELD (%), the first argument is not an integer.'
       :(PRINT.IN.FIELD.ERRORDUMP)
PRINT.IN.FIELD.ERROR2
       |'In PRINT.IN.FIELD (%), the second argument has no'
+       ' string representation.'
       :(PRINT.IN.FIELD.ERRORDUMP)
PRINT.IN.FIELD.ERROR3
       |'In PRINT.IN.FIELD (%):  In the second argument,'
       |('the part after ' PIF...C.  '. could not be interpreted')
       |'as an expression.'
       :(PRINT.IN.FIELD.ERRORDUMP)
PRINT.IN.FIELD.ERROR4
       |'In PRINT.IN.FIELD (%):  In the second argument,'
       |('the part after ' PIF...C. '. could be interpreted')
       |'as an expression, but it did not evaluate to a legal value.'
PRINT.IN.FIELD.ERRORDUMP
       |''
       |'The values of the arguments and locals were:'
       |''
       |('PIF...N. = ' PIF...N.)
       |('PIF...S. = ' PIF...S.)
       |('PIF...V. = ' PIF...V.)
       |('PIF...C. = ' PIF...C.)
       TDUMP( 'PRINT.IN.FIELD' )
          :(END)
PRINT.IN.FIELD.END
*
 DEFINE('PUT(UNAME,PROP,VAL)PLT,LST,ELEM')   :(PUT.END)
PUT
*       UNAME = CONVERT(UNAME,'NAME')     :F(PUT.ERROR1)
       (IDENT(PROP) TDUMP(.PUT,2))
       ( ~ATOM(PROP) NULL(PROP) TDUMP(.PUT,2) )
       VAL = IDENT(VAL) NIL
       PLT = $'   PrOpErTy  LiSt  TaBlE   '
       PUT = ?( ~ATOM(VAL) NULL(VAL) )
+         (PROP ~ NIL) ~ NIL
+         :S(PUT1)
       PUT = (PROP ~ VAL ~ NIL) ~ NIL
PUT1
       LST = ITEM(PLT,UNAME)
       IDENT(LST)         :S(PUT4)
       ATOM(LST)          :S(PUT.ERROR2)
       NULL(LST)          :S(PUT4)
PUT2
       ELEM = POP( .LST)            :F(PUT3)
       ATOM(ELEM)         :S(PUT.ERROR2)
       PUT = ~EQUAL(CAR(ELEM),PROP)
+         ELEM ~ PUT          :(PUT2)
PUT3
       PUT = LREVERSE(PUT)
PUT4
       ITEM(PLT,UNAME) = PUT        :(RETURN)
PUT.ERROR1
       TDUMP(.PUT,1)
PUT.ERROR2
       |''
       |'Program error: In PUT,'
       |'a property list is not a list of lists.'
       |'The offending object is '
       |LST
       |''
       TDUMP(.PUT)
PUT.END
*
 DEFINE('PUTL(UNL,PROP,VAL)U...NAME.')      :(PUTL.END)
PUTL
       LISTARG( .PUTL, 1, .UNL)
       PUTL = NIL
PUTL1
       U...NAME. = POP( .UNL)      :F(RETURN)
       PUT(U...NAME.,PROP,VAL)       :(PUTL1)
PUTL.END
*
 DEFINE('PUTPROP(UNAME,VAL,PROP)PLT,LST,ELEM,FLAG')
       :(PUTPROP.END)
PUTPROP
*       UNAME = CONVERT(UNAME,'NAME')     :F(PUTPROP.ERROR1)
       (IDENT(PROP) TDUMP(.PUTPROP,2))
       ( ~ATOM(PROP) NULL(PROP) TDUMP(.PUTPROP,2))
       VAL = IDENT(VAL) NIL
       PUTPROP = NIL
       FLAG = ''
       PLT = $'   PrOpErTy  LiSt  TaBlE   '
       LST = ITEM(PLT,UNAME)
       IDENT(LST)         :S(PUTPROP2)
       ATOM(LST)          :S(PUTPROP.ERROR2)
       NULL(LST)          :S(PUTPROP2)
PUTPROP1
       ELEM = POP( .LST)             :F(PUTPROP2)
       ATOM(ELEM)         :S(PUTPROP.ERROR2)
       PUTPROP = ~EQUAL(CAR(ELEM),PROP)
+         ELEM ~ PUTPROP            :S(PUTPROP1)
       DIFFER(FLAG)            :S(PUTPROP1)
       FLAG = 1
       PUTPROP = ?( ~ATOM(VAL) NULL(VAL) )
+         ELEM ~ PUTPROP      :S(PUTPROP1)
       PUTPROP = (PROP ~ VAL ~ CDR(ELEM)) ~ PUTPROP
+         :(PUTPROP1)
PUTPROP2
       PUTPROP = DIFFER(FLAG)
+         LREVERSE(PUTPROP)         :S(PUTPROP4)
       PUTPROP = ?( ~ATOM(VAL) NULL(VAL) )
+         (PROP ~ NIL) ~ LREVERSE(PUTPROP)    :S(PUTPROP4)
       PUTPROP = (PROP ~ VAL ~ NIL) ~ LREVERSE(PUTPROP)
PUTPROP4
       ITEM(PLT,UNAME) = PUTPROP   :(RETURN)
PUTPROP.ERROR1
       TDUMP(.PUTPROP,1)
PUTPROP.ERROR2
       |''
       |'Program error:  In PUTPROP,'
       |'a property list is not a list of lists.'
       |'The offending object is '
       |LST
       |''
       TDUMP(.PUTPROP)
PUTPROP.END
*
 DEFINE('QUOTIENT(L)')        :(QUOTIENT.END)
QUOTIENT
       QUOTIENT = LISTARG(.QUOTIENT,1,.L)
+         ARITH(.DIV,L)       :(RETURN)
QUOTIENT.END
*
 DEFINE('RAC(L)')    :(RAC.END)
RAC
       RAC = LISTARG(.RAC,1,.L)
+         CAR(LREVERSE(L))          :(RETURN)
RAC.END
*
 DEFINE('RAD(D)')    :(RAD.END)
RAD
       RAD = NUMARG(.RAD,1,.D)  D * 0.017453292519943      :(RETURN)
RAD.END
*
 DEFINE('RAISE(X,Y)')    :(RAISE.END)
RAISE
       (NUMARG(.RAISE,1,.X) NUMARG(.RAISE,2,.Y))
       (LT(X,0) TDUMP(.RAISE,2))
       RAISE = EQ(X,0)   0.0       :S(RETURN)
       RAISE = X ** Y              :(RETURN)
RAISE.END
*
 DEFINE('RDC(L)')    :(RDC.END)
RDC
       LISTARG( .RDC, 1, .L)
       RDC = LREVERSE(CDR(LREVERSE(L)))       :(RETURN)
RDC.END
*
 DEFINE('READLIST(L)')        :(READLIST.END)
READLIST
       READLIST = LISTARG(.READLIST,1,.L)
+         READ(CONCAT(L))           :(RETURN)
READLIST.END
*
 DEFINE('REMOVE(L,OLD)PCA,PCD')     :(REMOVE.END)
REMOVE
       ATOM(L)       :F(REMOVE1)
       REMOVE = EQU(OLD,L) NIL      :S(RETURN)
       REMOVE = L    :(RETURN)
REMOVE1  REMOVE = NULL(L) NIL       :S(RETURN)
       REMOVE = EQUAL(L,OLD) NIL    :S(RETURN)
       PCA = REMOVE(CAR(L),OLD)
       PCD = REMOVE(CDR(L),OLD)
       REMOVE = (~ATOM(PCA) NULL(PCA))    PCD      :S(RETURN)
       REMOVE = PCA ~ PCD            :(RETURN)
REMOVE.END
*
 DEFINE('REMPROP(UNAME,PROP)PLT,LST,ELEM,NEW')    :(REMPROP.END)
REMPROP
*       UNAME = CONVERT(UNAME,'NAME')      :F(REMPROP.ERROR1)
       (IDENT(PROP) TDUMP(.REMPROP,2))
       ( ~ATOM(PROP) NULL(PROP) TDUMP(.REMPROP,2))
       PLT = $'   PrOpErTy  LiSt  TaBlE   '
       REMPROP = NIL
       LST = ITEM(PLT,UNAME)
       IDENT(LST)         :S(RETURN)
       ATOM(LST)          :S(REMPROP.ERROR2)
       NULL(LST)          :S(RETURN)
       NEW = NIL
REMPROP1
       ELEM = POP( .LST)             :F(REMPROP2)
       ATOM(ELEM)         :S(REMPROP.ERROR2)
       NEW = ~EQUAL(CAR(ELEM),PROP)
+         ELE ~ NEW            :S(REMPROP1)
       REMPROP = T        :(REMPROP1)
REMPROP2
       ITEM(PLT,UNAME) = LREVERSE(NEW)   :(RETURN)
REMPROP.ERROR1
       TDUMP(.REMPROP,1)
REMPROP.ERROR2
       |''
       |'Program error: In REMPROP,'
       |'a property list is not a list of lists.'
       |'The offending object is'
       |LST
       |''
       TDUMP(.REMPROP)
REMPROP.END
*
 DEFINE('ROUND(X)')      :(ROUND.END)
ROUND
       NUMARG( .ROUND, 1, .X)
       ROUND = LT(X,0) -FIX( -X + 0.5)   :S(RETURN)
       ROUND = FIX(X + 0.5)     :(RETURN)
ROUND.END
*
 DEFINE('RPLACA(L,A)')   :(RPLACA.END)
RPLACA
       CAR(L) = LISTARG(.RPLACA,1,.L)   A
       RPLACA = L    :(RETURN)
RPLACA.END
*
 DEFINE('RPLACD(L,A)')  :(RPLACD.END)
RPLACD
       CDR(L) = LISTARG(.RPLACD,1,.L)   A
       RPLACD = L    :(RETURN)
RPLACD.END
*
 DEFINE('RPLACN(L,N,S)I')      :(RPLACN.END)
RPLACN
       (LISTARG(.RPLACN,1,.L)  INTARG(.RPLACN,2,.N))
       RPLACN = NEG(N)
+         RPLACN(L,LENGTH(L) + N + 1,S)       :S(RETURN)
       RPLACN = GT(N,LENGTH(L))
+         NCONC( L ~ (S ~ NIL) ~ NIL)    :S(RETURN)
       RPLACN = ZERO(N)
+         S ~ L           :S(RETURN)
       I = 1
RPLACN1   I = LT(I,N) I + 1     :F(RPLACN2)
       L  = CDR(L)   :(RPLACN1)
RPLACN2   RPLACN = RPLACA(L,S)      :(RETURN)
RPLACN.END
*
 DEFINE('SET.(SET...N,V)')   :(SET..END)
SET.
       STRINGARG(.SET., 1, .SET...N)
       $SET...N = V
       SET. = V       :(RETURN)
SET..END
*
 DEFINE('SETL(LNV)')       :(SETL.END)
SETL
       SETL = LISTARG(.SETL,1,.LNV)    NIL
       EQ(REMDR(LENGTH(LNV),2),1)      :F(SETL1)
       TDUMP('SETL',1)
SETL1
+      SETL =  %LNV %CDR(LNV)
+         SET.(CAR(LNV),CADR(LNV)) ~ SETL      :F(SETL2)
       LNV = CDDR(LNV)    :(SETL1)
SETL2
+      SETL = LREVERSE(SETL)   :(RETURN)
SETL.END
*
 DEFINE('SIGN(X)')         :(SIGN.END)
SIGN
       NUMARG( .SIGN, 1, .X)
       SIGN = GT(X,0) 1  :S(RETURN)
       SIGN = LT(X,0) -1      :S(RETURN)
       SIGN = 0     :(RETURN)
SIGN.END
*
 DEFINE('SIN(A)K')             :(SIN.END)
SIN
       NUMARG( .SIN, 1, .A)
       SIN = LT(A) -SIN( -A)   :S(RETURN)
       SIN = LT(A, 2 * P...I.) SIN.(A)   :S(RETURN)
       K = FIX(A / (2 * P...I.))
       SIN = SIN.(A - K * 2 * P...I.)    :(RETURN)
SIN.
       A = DFLOAT(A)
       SIN. = EQ(27., 27. - 4 * A * A)  A
+         :S(RETURN)
       A = SIN.(A / 3)
       SIN. = A * (3 - 4  * A * A)             :(RETURN)
SIN.END
*
 DEFINE('SNOC(L,S)')       :(SNOC.END)
SNOC
       LISTARG( .SNOC, 1, .L)
       SNOC = APPEND(L ~ (S ~ NIL) ~ NIL)   :(RETURN)
SNOC.END
*
 DEFINE('SOME(FN,L)A,V')       :(SOME.END)
SOME
       SOME =
+         (STRINGARG(.SOME,1,.FN) LISTARG(.SOME,2,.L))
+         NIL
SOME1    A = POP( .L)  :F(RETURN)
       %APPLY(FN,A)       :F(SOME1)
       SOME = A ~ L       :(RETURN)
SOME.END
*
 DEFINE('SORT.(A,II,JJ,P)IU,IL,M,I,J,K,IJ,T,L,TT')
           :(SORT..END)
SORT.
       (DIFFER('ARRAY',DATATYPE(A)) TDUMP(.SORT., 1))
       (INTARG( .SORT., 2, .II)  INTARG( .SORT., 3, .JJ))
       P POS(0) ('LE' | 'GE' | 'LLE' | 'LGE') RPOS(0)
+          :S(SORT1)
       TDUMP( .SORT., 4)
SORT1
       IU = ARRAY(21)
       IL = COPY(IU)
       M = 1
       I = II
       J = JJ
SORT5  GE(I,J)       :S(SORT70)
SORT10    K = I
       IJ = CONVERT( (J + I) / 2, 'INTEGER' )
       T = A<IJ>
       SORT.LE(A<I>,T)    :S(SORT20)
       A<IJ> = A<I>
       A<I> = T
       T = A<IJ>
SORT20    L = J
       SORT.GE(A<J>,T)    :S(SORT40)
       A<IJ> = A<J>
       A<J> = T
       T = A<IJ>
       SORT.LE(A<I>,T)    :S(SORT40)
       A<IJ> = A<I>
       A<I> = T
       T = A<IJ>      :(SORT40)
SORT30    A<L> = A<K>
       A<K> = TT
SORT40     L = L - 1
       SORT.GT(A<L>,T)    :S(SORT40)
       TT = A<L>
SORT50    K = K + 1
       SORT.LT(A<K>,T)    :S(SORT50)
       LE(K,L)       :S(SORT30)
       LE( L - I, J - K)       :S(SORT60)
       IL<M> = I
       IU<M> = L
       I = K
       M = M + 1    :(SORT80)
SORT60   IL<M> = K
       IU<M> = J
       J = L
       M = M + 1    :(SORT80)
SORT70   M = M - 1
       SORT. = LE(M,0) A        :S(RETURN)
       I = IL<M>
       J = IU<M>
SORT80   GE( J - I, II)        :S(SORT10)
       EQ(I,II)      :S(SORT5)
       I = I - 1
SORT90    I = I + 1
       EQ(I,J)       :S(SORT70)
       T = A<I + 1>
       SORT.LE(A<I>,T)    :S(SORT90)
       K = I
SORT100  A<K + 1> = A<K>
       K = K - 1
       SORT.LT(T,A<K>)    :S(SORT100)
       A<K + 1> = T       :(SORT90)
*
SORT.LE  APPLY(P,X,Y)     :S(RETURN)F(FRETURN)
SORT.GE  APPLY(P,Y,X)     :S(RETURN)F(FRETURN)
SORT.LT  APPLY(P,Y,X)     :S(FRETURN)F(RETURN)
SORT.GT  APPLY(P,X,Y)     :S(FRETURN)F(RETURN)
SORT..END
*
 DEFINE('SQRT(Y)T')       :(SQRT.END)
SQRT
       NUMARG( .SQRT, 1, .Y)
       (LT(Y,0) TDUMP(.SQRT,1))
       SQRT = Y ** 0.5     :(RETURN)
SQRT.END
*
 DEFINE('SUB(X,Y)')     :(SUB.END)
SUB
       SUB =
+         (NUMARG(.SUB,1,.X) NUMARG(.SUB,2,.Y))
+         X - Y           :(RETURN)
SUB.END
*
 DEFINE('SUBSET(FN,L)A,V')   :(SUBSET.END)
SUBSET
       SUBSET =
+         (STRINGARG(.SUBSET,1,.FN) LISTARG(.SUBSET,2,.L))
+         NIL
SUBSET1   A = POP( .L) :F(SUBSET2)
       %APPLY(FN,A)       :F(SUBSET1)
       SUBSET = A ~ SUBSET         :(SUBSET1)
SUBSET2   SUBSET = LREVERSE(SUBSET)       :(RETURN)
SUBSET.END
*
 DEFINE('SUBST(L,OLD,NEW)PCA,PCD')      :(SUBST.END)
SUBST
       ATOM(L)       :F(SUBST1)
       SUBST = EQU(OLD,L) NEW       :S(RETURN)
       SUBST = L     :(RETURN)
SUBST1   SUBST = EQUAL(OLD,L) NEW        :S(RETURN)
       PCA = SUBST(CAR(L),OLD,NEW)
       PCD = SUBST(CDR(L),OLD,NEW)
       SUBST = PCA ~ PCD            :(RETURN)
SUBST.END
*
 DEFINE('SUB1(X)')       :(SUB1.END)
SUB1
       NUMARG( .SUB1, 1, .X)
       SUB1 = X - 1       :(RETURN)
SUB1.END
*
 DEFINE('SUFLIST(L,N)I')     :(SUFLIST.END)
SUFLIST
       (LISTARG(.SUFLIST,1,.L) INTARG(.SUFLIST,2,.N))
       SUFLIST = EQ(N,0)  L    :S(RETURN)
       SUFLIST = LT(N,0) SUFLIST(L,LENGTH(L) + N)
+         :S(RETURN)
       I = 0
       SUFLIST = L
SUFLIST1
+      I = ( LT(I,N) ?POP( .SUFLIST))    I + 1
+          :S(SUFLIST1)F(RETURN)
SUFLIST.END
*
 DEFINE('TAN(Z)')   :(TAN.END)
TAN
       NUMARG( .TAN, 1, .Z)
       TAN = SIN(Z)
       (GT(ABS(TAN),1) TDUMP( .TAN, 1))
       TAN = TAN / COS(Z,TAN)    :(RETURN)
TAN.END
*
  DEFINE('TDUMP(TDUMP...FN.,TDUMP...AN.)'
+      'TDUMP...I.,TDUMP...A.')      :(TDUMP.END)
TDUMP
       ||''
       |(6 % '* ' 'Terminal Error in ' TDUMP...FN.) |""
       |(12 % ' '   'Arguments') |""
       TDUMP...I. = 1
TDUMP1
       TDUMP...A. = ARG(TDUMP...FN.,TDUMP...I.)
+         :F(TDUMP2)
       $TDUMP...A. = ~ATOM($TDUMP...A.) UNREAD($TDUMP...A.)
       EQ(TDUMP...AN.,TDUMP...I.)   :F(TDUMP1.A)
       |(6 % '* ' TDUMP...A.  ' = ' $TDUMP...A.)
       TDUMP...I. = TDUMP...I. + 1        :(TDUMP1)
TDUMP1.A
       |(12 % ' ' TDUMP...A.  ' = ' $TDUMP...A.)
       TDUMP...I. = TDUMP...I. + 1
+         :(TDUMP1)
TDUMP2
       |''
       |(12 % ' '  'Locals') |""
       TDUMP...I. = 1
TDUMP3
       TDUMP...A. = LOCAL(TDUMP...FN.,TDUMP...I.)
+         :F(TDUMP4)
       $TDUMP...A. = ~ATOM($TDUMP...A.) UNREAD($TDUMP...A.)
       |(12 % ' ' TDUMP...A. ' = ' $TDUMP...A.)
       TDUMP...I.  = TDUMP...I. + 1        :(TDUMP3)
TDUMP4
       |''
       $TDUMP...FN. = ~ATOM($TDUMP...FN.) UNREAD($TDUMP...FN.)
       |(12 % ' ' TDUMP...FN. ' = ' $TDUMP...FN.) |""
       |(6 % '* '  'End of SNOLISPIST dump from ' TDUMP...FN.)
       |(6 % '  '  'You can get a SPITBOL dump:')
       |(6 % '  '  'Enter 0 for no dump, 1 for dump')
       &DUMP = MIN(1,MAX(0,CONVERT(IN(),'INTEGER')))
           :(END)
TDUMP.END
*
 DEFINE('TIMES(L)')       :(TIMES.END)
TIMES
       TIMES = LISTARG(.TIMES,1,.L) ARITH(.MULT,L)      :(RETURN)
TIMES.END
*
 DEFINE('UNION(L1,L2)A')      :(UNION.END)
UNION
       UNION =
+         (LISTARG(.UNION,1,.L1) LISTARG(.UNION,2,.L2))
+         NIL
       IDENT(L1,L2)       :S(UNION2)
UNION1    A = POP( .L1)       :F(UNION2)
       UNION = INSERT(A,UNION)   :(UNION1)
UNION2    A = POP( .L2)       :F(RETURN)
       UNION = INSERT(A,UNION)   :(UNION2)
UNION.END
*
 DEFINE('ZERO(X)')       :(ZERO.END)
ZERO
       (NUMARG(.ZERO,1,.X) EQ(X,0))      :S(RETURN)F(FRETURN)
ZERO.END
*
 DEFINE('ZEROP(A)')       :(ZEROP.END)
ZEROP
       ZEROP = (NUMARG(.ZEROP,1,.A) ZERO(A)) T     :S(RETURN)
       ZEROP = NIL        :(RETURN)
ZEROP.END
