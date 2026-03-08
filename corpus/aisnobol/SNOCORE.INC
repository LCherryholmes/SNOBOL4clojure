* SNOCORE.INC - SNOBOL4+ VERSION
*
* These are the core functions of the SNOLISPIST system.
* Functions defined by DEXTERN are in SNOLIB.INC
* and are loaded dynamically when and if they are
* called.
*
* Derived from "Artificial Intelligence Programming in SNOBOL4"
*  by Michael Shafto.  Converted to SNOBOL4+ by Mark Emmer, Catspaw, Inc.
*
* Keyword section
*
-PLUSOPS 1
  &ANCHOR    = 0
  &CASE =    0
  &DUMP =    0
  &FTRACE    = 0
  &FULLSCAN = 1
  &STLIMIT = -1
  &TRACE = 0
  &TRIM =  1
*
* Default I/O assignments for variables INPUT. and OUTPUT.
*
    INPUT(.INPUT.,5)
    OUTPUT(.OUTPUT.,6)
*
* Defined datatypes and global variables
*
    DATA('CONS(CAR,CDR)')
    NIL = CONS('','') ; T = CONS('T','T')
    $'   PrOpErTy  LiSt  TaBlE   ' = TABLE()
*
 DEFINE('PRT.VIA.OUTPUT(S)')        :(PRT.VIA.OUTPUT.END)
PRT.VIA.OUTPUT
      ATOM(S)            :F(PRT.VIA.OUTPUT1)
      S REM $ OUTPUT. $ PRT.VIA.OUTPUT       :(RETURN)
PRT.VIA.OUTPUT1
      UNREAD(S) REM $ OUTPUT. $ PRT.VIA.OUTPUT   :S(RETURN)
      OUTPUT. = "Fatal error:  In PRT.VIA.OUTPUT, UNREAD failed."
      OUTPUT. = " Argument datatype:  " DATATYPE(S)
      :(END)
PRT.VIA.OUTPUT.END
*
      OPSYN('|', .PRT.VIA.OUTPUT,  1)
      OPSYN('PRINT','PRT.VIA.OUTPUT')
*
*
* Functionals used to define functions
*
 DEFINE('DEXP(PROTO)NAME,ARGS')     :(DEXP.END)
DEXP
      PROTO POS(0) SPAN(' ') =
      PROTO BREAK( "(" ) . NAME BAL . ARGS =
+        :F(DEXP2)
      NAME = IDENT(NAME,'LAMBDA') "LAMBDA..." CONVERT(STATEMENTS(0),"REAL")
+        :F(DEXP1)
      DEXP = NAME
DEXP1 CODE( NAME " " NAME PROTO " :S(RETURN)F(FRETURN) ; " )
+        :F(DEXP2)
      DEFINE(  NAME ARGS )    :S(RETURN)
DEXP2
      PRINT(
+         "Fatal error:  In DEXP, an illegal prototype "
+         "or function name was detected.")
      PRINT(
+         "Prototype:  "   PROTO)
               :(END)
DEXP.END
*
*
* Define external function
*
 DEFINE('DEXTERN(PROTO,LBL)NAME')
 DEFINE('LOADEX(LBL)LIB.FILE,PAT,X,CODE')
    &ALPHABET RTAB(1) REM $ CH
    LOADEX.LAST.LOAD = DUPL(CH,13)
    LOADEX...LIB. =  "SNOLIB.INC"
    LOADEX...IDX. =  "SNOLIB.IDX"
    LOADEX...TBL. =  TABLE(51,25)
    LOADEX...PAT. =  BREAK(',') . LOADEX...NAM. ',' REM . LOADEX...POS.
    INPUT( .LIB.FILE, 15, 'R', LOADEX...IDX.)   :S(DEXTERN0)
    INPUT( .LIB.FILE, 15, 'R', ENVIRONMENT('SNOLIB') ' \' LOADEX...IDX.)
+                              :S(DEXTERN0)
    SCREEN =
+        "Fatal error:  In DEXTERN, could not open library "
+        "index: " LOADEX...IDX.                     :(END)
*
* Read index of functions into table from index file.
*
DEXTERN0
    LIB.FILE LOADEX...PAT.                          :F(DEXTERN2)
    LOADEX...TBL.<LOADEX...NAM.> = LOADEX...POS.    :(DEXTERN0)
DEXTERN2
    ENDFILE(15)

    INPUT( .LIB.FILE, 15, 'R', LOADEX...LIB.)   :S(DEXTERN.END)
    INPUT( .LIB.FILE, 15, 'R', ENVIRONMENT('SNOLIB') ' \' LOADEX...LIB.)
+                              :S(DEXTERN.END)
    SCREEN =
+        "Fatal error:  In DEXTERN, could not open library "
+        "file: " LOADEX...LIB.                     :(END)
*
DEXTERN
      PROTO IDENT(LBL) BREAK("(") . LBL
      CODE( LBL "  LOADEX('"  LBL  "') ; :(" LBL ")" )
+         :F(DEXTERN1)
      DEFINE(PROTO,LBL)       :S(RETURN)
DEXTERN1
      PRINT(
+         "Fatal error:  In DEXTERN, an illegal prototype "
+         "or function body was detected.")
      PRINT(
+         "Prototype:  "   PROTO)
               :(END)
*
*
* Load and code external function
*
LOADEX
      LOADEX...POS. = LOADEX...TBL.<LBL>
      IDENT(LOADEX...POS.)                      :S(LOADEX4)
      SEEK(15, LOADEX...POS., 0)                :F(LOADEX4)
*
      PAT = POS(0) LBL (" " | RPOS(0))
LOADEX1
      CODE = LIB.FILE    :F(LOADEX4)
      CODE PAT      :F(LOADEX1)
      PAT = POS(0) LBL '.END' (" " | RPOS(0))
LOADEX2
      X = LIB.FILE       :F(LOADEX4)
      X PAT    :S(LOADEX3)
      X POS(0) ANY('*-')      :S(LOADEX2)
      X = ';' X
      X POS(0) ';'  ANY('.+') = ' '
      CODE = CODE X      :(LOADEX2)
LOADEX3
      LOADEX.LAST.LOAD = LBL
      CODE(CODE)    :S(RETURN)
LOADEX4
      PRINT(
+       "Fatal error:  In LOADEX, a function was missing or uncodable.")
             :(END)
DEXTERN.END
*
*
* Fatal-error message with optional dump
*
 DEXTERN('TDUMP(TDUMP...FN.,TDUMP...AN.)'
+      'TDUMP...I.,TDUMP...A.')
*
* Argument checking functions
*
 DEFINE('LISTARG(FNAME,ANUM,ARG...NAME.)')
            :(LISTARG.END)
LISTARG
       IDENT(DATATYPE( $ARG...NAME.),  'CONS')     :S(RETURN)
       |''
       |('Argument number ' ANUM " to " FNAME " (" ARG...NAME. ')')
       |("has illegal datatype " DATATYPE( $ARG...NAME.) '.')
       |('Datatype CONS was expected.')
       TDUMP( FNAME, ANUM)
LISTARG.END
*
 DEFINE('NUMARG(FNAME,ANUM,ARG...NAME.)')  :(NUMARG.END)
NUMARG
     NUMBER( $ARG...NAME.)        :S(RETURN)
     |''
     |("Argument number " ANUM " to " FNAME " (" ARG...NAME. ')')
     |("has illegal value " $ARG...NAME. '.')
     |("A NUMERIC value was expected.")
     TDUMP( FNAME, ANUM)
NUMARG.END
*
 DEFINE('INTARG(FNAME,ANUM,ARG...NAME.)')    :(INTARG.END)
INTARG
     INTEGER( $ARG...NAME.)   :S(RETURN)
     |''
     |("Argument number " ANUM " to " FNAME " (" ARG...NAME.  ')')
     |("has illegal value " $ARG...NAME. '.')
     |("An INTEGER was expected.")
     TDUMP( FNAME, ANUM)
INTARG.END
*
 DEFINE('STRINGARG(FNAME,ANUM,ARG...NAME.)')       :(STRINGARG.END)
STRINGARG
      IDENT(DATATYPE( $ARG...NAME.), 'STRING' )
+         :S(RETURN)
      |''
      |("Argument number " ANUM " to " FNAME " (" ARG...NAME.  ')')
      |("has illegal datatype " DATATYPE( $ARG...NAME.) '.')
      |("Datatype STRING or NAME was expected.")
      TDUMP( FNAME, ANUM)
STRINGARG.END
*
*
* CAR/CDR compounds
*
     DEXTERN( 'CAAR(L)' )
     DEXTERN( 'CADR(L)' )
     DEXTERN( 'CDAR(L)' )
     DEXTERN( 'CDDR(L)' )
     DEXTERN( 'CAAAR(L)' )
     DEXTERN( 'CAADR(L)' )
     DEXTERN( 'CADAR(L)' )
     DEXTERN( 'CDAAR(L)' )
     DEXTERN( 'CADDR(L)' )
     DEXTERN( 'CDADR(L)' )
     DEXTERN( 'CDDAR(L)' )
     DEXTERN( 'CDDDR(L)' )
     DEXTERN( 'CAAAAR(L)' )
     DEXTERN( 'CAAADR(L)' )
     DEXTERN( 'CAADAR(L)' )
     DEXTERN( 'CADAAR(L)' )
     DEXTERN( 'CDAAAR(L)' )
     DEXTERN( 'CAADDR(L)' )
     DEXTERN( 'CADADR(L)' )
     DEXTERN( 'CDAADR(L)' )
     DEXTERN( 'CADDAR(L)' )
     DEXTERN( 'CDADAR(L)' )
     DEXTERN( 'CDDAAR(L)' )
     DEXTERN( 'CADDDR(L)' )
     DEXTERN( 'CDADDR(L)' )
     DEXTERN( 'CDDADR(L)' )
     DEXTERN( 'CDDDAR(L)' )
     DEXTERN( 'CDDDDR(L)' )
*
* Predicate:  Is A = NIL?
*
 DEXP('NULL(A) = '
+     '?(LISTARG( .NULL, 1, .A) '
+     'IDENT(CAR(A)) IDENT(CDR(A)))')
     OPSYN(.NOT,.NULL)
*
* Make new CONS cell
*
 DEXP('LIST(S1,S2) = CONS(S1,S2)')
     OPSYN("~",.LIST,2)
*
* Function of zero arguments which returns a unique name
*
 DEXTERN('GENSYM()')
     OPSYN( .NEWSYM, .GENSYM)
-EJECT
*
* I/O functions
*
*
* Formatted output
*
 DEXTERN('PRINT.IN.FIELD(PIF...N.,PIF...S.)'
+            'PIF...C.,PIF...V.')
      OPSYN('%',  .PRINT.IN.FIELD, 2)

*
* Standard Input
*
 DEFINE('IN(IN...N)')      :(IN.END)
IN    IN...N = IDENT(IN...N)  .IN
      STRINGARG(  .IN, 1,  .IN...N)
      $IN...N = INPUT.   :F(FRETURN)
      IN = DIFFER(IN...N,  .IN) $IN...N
          :(RETURN)
IN.END

*
* Interactive tracing
*
 DEXTERN('LTRACE(PARAM,L)F,TFNAME')
 DEFINE('LTRACE1(LTRACE1...F.,LTRACE1...T.,LTRACE1...L.)'
+     'LTRACE1...I.,LTRACE1...N.')
-EJECT
*
* General-purpose and datatype predicates
*
*
 DEXP('FAIL.IF.NIL(A) = '
+     '?(LISTARG( .FAIL.IF.NIL, 1, .A)  ~NULL(A))   A')
      OPSYN('/',  .FAIL.IF.NIL, 1)
*
 DEXP('FAIL.IF.NIL.ELSE.SUCCEED(X) = '
+     '?(LISTARG( .FAIL.IF.NIL.ELSE.SUCCEED, 1, .X) /X)')
      OPSYN("%",  .FAIL.IF.NIL.ELSE.SUCCEED, 1)
*
 DEXTERN('NULLP(A)')
      OPSYN(.NOTP,.NULLP)
*
 DEXP('ATOM(A) = DIFFER(DATATYPE(A),"CONS")')
*
 DEXTERN('ATOMP(A)')
*
  DEXP('NUMBER(X) = INTEGER(X) :S(RETURN) ; ?CONVERT(X,"REAL")')
*
 DEXTERN('NUMBERP(A)')
*
 DEXTERN('EQU(A1,A2)')
*
 DEXTERN('EQP(A1,A2)')
*
 DEXTERN('EQUAL(X,Y)')
*
 DEXTERN('EQUALP(A1,A2)')
*
-EJECT
*      Numeric predicates:
*
*
 DEXTERN('NEG(X)')
*
 DEXTERN('NEGP(X)')
*
 DEXTERN('ZERO(X)')
*
 DEXTERN('ZEROP(X)')
*
 DEXTERN('LESS(L)A,B')
*
 DEXTERN('LESSP(L)')
*
 DEXTERN('GREATER(L)A,B')
*
 DEXTERN('GREATERP(L)')
*
* Numeric functions
*
*      Single argument:
*
 DEXTERN('ABS(X)')
*
 DEXTERN('SIGN(X)')
*
 DEXTERN('ADD1(X)')
*
 DEXTERN('SUB1(X)')
*
 DEXTERN('FLOAT(N)')
*
 DEXTERN('DFLOAT(N)')
*
 DEXTERN('FIX(X)')
*
 DEXTERN('MINUS(X)')
*
 DEXTERN('ROUND(X)')
*
*      Binary:
*
 DEXTERN('ADD(X,Y)')
*
 DEXTERN('SUB(X,Y)')
*
 DEXTERN('MULT(X,Y)')
*
 DEXTERN('DIV(X,Y)')
*
 DEXTERN('MAX(X,Y)')
*
 DEXTERN('MIN(X,Y)')
*
     OPSYN(.REMAINDER, .REMDR)
*
*      List argument:
*
 DEXTERN('PLUS(L)')
*
 DEXTERN('DIFFERENCE(L)')
*
 DEXTERN('TIMES(L)')
*
 DEXTERN('QUOTIENT(L)')
*
 DEXTERN('ARITH(OP,ALIST)A')
*
* List functions
*
*      Composition:
*      CONS operates via datatype definition
*
*
 DEXTERN('APPEND(LOL)L,A')
*
 DEXTERN('EXCLUDE(L,XCL)A')
*
 DEXTERN('INSERT(S,L)')
*
 DEXTERN('INTERSECT(L1,L2)L,A')
*
 DEXTERN('LCOPY(L)CA,CD')
*
 DEXTERN('NCONC(LOL)LN,L')
*
 DEXTERN('PUT(UNAME,PROP,VAL)PLT,LST,ELEM')
*
 DEXTERN('REMPROP(UNAME,PROP)PLT,LST,ELEM,NEW')
*
 DEXTERN('PUTPROP(UNAME,VAL,PROP)PLT,LST,ELEM,FLAG')
*
 DEXTERN('ADDPROP(UNAME,VAL,PROP)PLT,LST,ELEM,FLAG')
*
 DEXTERN('GETPROP(UNAME,PROP)PLT,LST,ELEM,FLAG,NEW')
*
 DEXTERN('DEFPROP(A1,EXP,A2)')
*
 DEXTERN('PUTL(UNL,PROP,VAL)U...NAME.')
*
 DEXTERN('LREVERSE(LST)')
*
 DEXTERN('RPLACA(L,A)')
*
 DEXTERN('RPLACD(L,A)')
*
 DEXTERN('RPLACN(L,N,S)I')
*
 DEXTERN('SNOC(L,S)')
*
 DEXTERN('SUBST(L,OLD,NEW)PCA,PCD')
*
 DEXTERN('UNION(L1,L2)A')
*
 DEXTERN('EXPLODE(A)CH')
*
 DEXTERN('READLIST(L)S')
*
*      Decomposition:
*
 DEXTERN('LAST(L)')
*
 DEXTERN('NTH(L,N)I')
*
 DEXTERN('PRELIST(L,N)')
*
 DEXTERN('RAC(L)')
*
 DEXTERN('RDC(L)')
*
 DEXTERN('REMOVE(L,OLD)PCA,PCD')
*
 DEXTERN('SUFLIST(L,N)I')
*
*
* Pop stack (argument is NAME)
*
 DEFINE('UNCONS(UNCONS...N)')      :(UNCONS.END)
UNCONS
       (ATOM( $UNCONS...N) TDUMP(.UNCONS,1))
       NULL( $UNCONS...N)      :S(FRETURN)
       (ATOM(CDR($UNCONS...N)) TDUMP(.UNCONS,1))
       UNCONS = CAR( $UNCONS...N)
       $UNCONS...N = CDR( $UNCONS...N)   :(RETURN)
UNCONS.END    OPSYN(.POP, .UNCONS)
*
*      Search:
*
 DEXTERN('ASSOC(TG,L)C')
*
 DEXTERN('ASSOCL(LTG,L)A')
*
 DEXTERN('FIND(TG,L)')
*
 DEXTERN('GET(UNAME,PROP)PLT,LST,ELEM')
*
 DEXTERN('GETL(UNAME,LPROP)PLT,LST,ELEM')
*
 DEXTERN('MEMBER(A,MBR)')
*
 DEXTERN('MEMQ(A,L)')

*
*      Miscellaneous:
*
 DEXTERN('LENGTH(L)')
*
 DEXTERN('SET.(SET...N,V)')
*
 DEXTERN('SETL(LNV)')
*
 DEXTERN('EVALCODE(S)')

*
* READ function, Version 2
*
*     Converts string to list.
*
*
* The FASTBAL function is from
*     Gimpel,  J. F.  Algorithms in SNOBOL4,  Chapter 9.
*
 DEFINE('FASTBAL(PARENS,QTS,S)NAME,IBAL,SPCHARS,ELEM'
+       ',LPS,Q,LP,RP')       :(FASTBAL.END)
FASTBAL NAME  = 'FASTBAL...' CONVERT(STATEMENTS(0),"REAL")
       IBAL = CONVERT(NAME,'EXPRESSION')
       IBAL = DIFFER(S,'') FASTBAL(PARENS,QTS,'')
       SPCHARS = PARENS QTS S
       ELEM = NOTANY(PARENS QTS) BREAK(SPCHARS)
FASTBAL1      QTS LEN(1) . Q =     :F(FASTBAL2)
       ELEM = Q BREAK(Q) Q | ELEM  :(FASTBAL1)
FASTBAL2      PARENS LEN(1) . LP RTAB(1) . PARENS LEN(1) . RP
+     :F(FASTBAL3)
       ELEM = LP IBAL RP | ELEM    :(FASTBAL2)
FASTBAL3      FASTBAL =  BREAK(SPCHARS) ARBNO(ELEM)
       $NAME = FASTBAL    :(RETURN)
FASTBAL.END


*
* Patterns used by more than one subroutine of READ
*
 READ...SPB.    = SPAN(" ")
 READ...SPBN.   = SPAN(" ") | ''
 READ...RF. = POS(0) READ...SPBN. "(" READ...SPBN. FENCE
 READ...RF2. = POS(0) READ...SPBN. FENCE
 READ...RE. = READ...SPBN. ')' READ...SPBN. RPOS(0)
 READ...BALQ. =
+     FASTBAL( '(<>)', '"' "'", ' )' ) $ READ...BQ.TEMP
+     *DIFFER(READ...BQ.TEMP)


*
* Recognize and read a T or NIL
*
*     Note: T and NIL are also specially recognized by
*           READ.DOTPAIR
*
 DEFINE('READ.NIL(S)')
     READ...NILPAT. = READ...RF. READ...SPBN. READ...RE.
       :(READ.NIL.END)
READ.NIL
       READ.NIL = IDENT(S,'T') T    :S(RETURN)
       READ.NIL = IDENT(S,'NIL') NIL     :S(RETURN)
       S READ...NILPAT.   :F(FRETURN)
       READ.NIL = NIL     :(RETURN)
READ.NIL.END

*
* Recognize and read a dotted pair
*
 DEFINE('READ.DOTPAIR(S)PCAR,PCDR')
       READ...SPD. = BREAKX(' ') ' . '
       READ...SPBDSPB. = READ...SPB. '.' READ...SPB.
       :(READ.DOTPAIR.END)
*
READ.DOTPAIR
       S READ...SPD.      :F(FRETURN)
       S READ...RF. READ...BALQ. . PCAR
+         READ...SPBDSPB.  =    :F(FRETURN)
       S  READ...RF2. READ...BALQ. . PCDR
+         READ...RE.      :F(READ.DOTPAIR1)
       PCAR = READ(PCAR)  ; PCDR = READ(PCDR)
       READ.DOTPAIR = IDENT(PCAR) IDENT(PCDR)
+         NIL  :S(RETURN)
       READ.DOTPAIR = IDENT(PCAR,'T') IDENT(PCDR,'T')
+         T     :S(RETURN)
       READ.DOTPAIR = PCAR ~ PCDR  :(RETURN)
*
READ.DOTPAIR1
       TDUMP('READ.DOTPAIR',1)
READ.DOTPAIR.END


*
* Recognize and read a list of one element.
*      The element may be a single atom, a single list,
*         or a single dotted pair.
*
 DEFINE('READ.SINGLETON(S)PCAR')
      READ...RJ. = READ...RF.   READ...BALQ. READ...SPB. NOTANY( ')' )
         :(READ.SINGLETON.END)
READ.SINGLETON
       S READ...RJ.       :S(FRETURN)
       S READ...RF. READ...BALQ. . PCAR READ...RE.
+      :F(FRETURN)
       READ.SINGLETON = READ(PCAR) ~ NIL       :(RETURN)
READ.SINGLETON.END


*
* Recognize and read a "regular" list.
*      This means a list of two or more elements
*         (not a dotted pair) such that the final
*         top-level element of the list is NIL.
*
 DEFINE('READ.REGULAR(S)S2,PCAR,RLIST')
       :(READ.REGULAR.END)
READ.REGULAR
       S READ...RF. READ...BALQ. . PCAR READ...SPB. =
+         :F(FRETURN)
       RLIST = PCAR ~ NIL
READ.REGULAR1
       S READ...RF2. READ...BALQ. . PCAR READ...SPB.
+        (NOTANY(')') REM) . S2   =   S2
+        :F(READ.REGULAR2)
       RLIST = PCAR ~ RLIST    :(READ.REGULAR1)
READ.REGULAR2
       S READ...RF2. READ...BALQ. . PCAR READ...RE.
+        :F(READ.REGULAR3)
       RLIST = PCAR ~ RLIST
       READ.REGULAR = MAPCARV( .READ, RLIST)       :(RETURN)
*
READ.REGULAR3
       TDUMP('READ.REGULAR',1)
READ.REGULAR.END


*
* Read an atom
*     "" and '' translate to the null string.
*     An error results (FRETURN) if
*        a)  the beginning of S looks like the
*            beginning of a list;
*        b)  the end of S looks like the end of a list;
*        c)  S is the null string.
*
 DEFINE('READ.ATOM(S)N,PRE')
     READ...RE2. = BREAKX( ')' )   READ...RE.
     READ...EV. = "\"
            :(READ.ATOM.END)
READ.ATOM
       (DIFFER(S,'""') DIFFER(S,"''"))   :F(RETURN)
       S READ...RF.       :S(FRETURN)
       S READ...RE2.      :S(FRETURN)
       READ.ATOM = DIFFER(S) S      :F(FRETURN)
       READ.ATOM SPAN(READ...EV.) . PRE =     :F(RETURN)
       N = SIZE(PRE)
READ.ATOM1
       (GT(N) ?SET.( .N, N - 1)
+      ?SET.( 'READ.ATOM', EVAL( READ.ATOM)) )
+        :S(READ.ATOM1)F(RETURN)
READ.ATOM.END


*
* This is the main string-to-list conversion routine.
*
  DEFINE('READ(S)')      :(READ.END)
READ  TRIM(S)
+        POS(0) READ...SPBN. REM $ S
       READ = READ.NIL(S)           :S(RETURN)
       READ = READ.DOTPAIR(S)       :S(RETURN)
       READ = READ.SINGLETON(S)     :S(RETURN)
       READ = READ.REGULAR(S)       :S(RETURN)
       READ = READ.ATOM(S)          :S(RETURN)
*
       TDUMP('READ',1)
READ.END
+     OPSYN('#', .READ, 1)
*
* List to string conversion routine.
*
*     CONCAT takes a list of strings and concatenates
*        them into one long string.  PAD is inserted
*        after each substring except the last.  QT can
*        be omitted (treated as the null string); if present
*        it is appended to front and end of each substring.
*
      DEFINE('CONCAT(L,PAD,QT)')     :(CONCAT.END)
CONCAT
       LISTARG( .CONCAT, 1, .L)
       STRINGARG( .CONCAT, 2, .PAD)
       STRINGARG( .CONCAT, 3, .QT)
       CONCAT =
+      CONCAT QT POP( .L) QT PAD    :S(CONCAT)
       CONCAT RTAB(SIZE(PAD)) . CONCAT      :(RETURN)
CONCAT.END
*
* Convert NIL or T
*
      DEFINE('UNREAD.NIL(L)')      :(UNREAD.NIL.END)
UNREAD.NIL
       ATOM(L)       :S(FRETURN)
       UNREAD.NIL = NULL(L) 'NIL'   :S(RETURN)
       UNREAD.NIL = IDENT(L,T) 'T'       :S(RETURN)F(FRETURN)
UNREAD.NIL.END
*
* Convert dotted pair
*
 DEFINE('UNREAD.DOTPAIR(L)PCAR,PCDR') :(UNREAD.DOTPAIR.END)
UNREAD.DOTPAIR
       (~ATOM(L) ATOM( CDR(L)))     :F(FRETURN)
       UNREAD.DOTPAIR =
+        IDENT(CAR(L)) IDENT(CDR(L)) 'NIL'   :S(RETURN)
       UNREAD.DOTPAIR =
+        IDENT(CAR(L),'T') IDENT(CDR(L),'T') 'T'       :S(RETURN)
       PCAR = UNREAD(CAR(L)) ; PCDR = UNREAD(CDR(L))
       UNREAD.DOTPAIR =
+        '(' PCAR ' . ' PCDR ')'   :(RETURN)
UNREAD.DOTPAIR.END
*
* Convert a list of one element
*
 DEXP('UNREAD.SINGLETON(L) = '
+     '(~ATOM(L) NULL( CDR(L))) '
+     '"(" UNREAD( CAR(L)) ")"' )
*
* Convert a regular, multi-element list
*
 DEXP('UNREAD.REGULAR(L) = '
+     '~ATOM(L) '
+     '"(" CONCAT(MAPCAR( .UNREAD,L), " ") ")"' )
*
* Convert an atom
*     Null string ==> ""
*     If the atom contains internal blanks,
*        it will be enclosed in double quotes,
*        unless it is already enclosed in single or
*        double quotes.
*
 DEFINE('UNREAD.ATOM(L)')
     UNREAD...Q. = POS(0) ('"' | "'") $ UNREAD...P.
+     RTAB(1) *UNREAD...P.
+     :(UNREAD.ATOM.END)
UNREAD.ATOM
       L = ATOM(L) CONVERT(L,"STRING")   :F(FRETURN)
       L = IDENT(L) '""'      :S(UNREAD.ATOM1)
       L BREAK(' ')           :F(UNREAD.ATOM1)
       L UNREAD...Q.     :S(UNREAD.ATOM1)
       L = '"' L '"'
UNREAD.ATOM1
       UNREAD.ATOM = L   :(RETURN)
UNREAD.ATOM.END
*
* This is the main conversion routine
*
 DEFINE('UNREAD(L)')    :(UNREAD.END)
UNREAD
       UNREAD = UNREAD.NIL(L)       :S(RETURN)
       UNREAD = UNREAD.DOTPAIR(L)   :S(RETURN)
       UNREAD = UNREAD.SINGLETON(L) :S(RETURN)
       UNREAD = UNREAD.REGULAR(L)   :S(RETURN)
       UNREAD = UNREAD.ATOM(L)      :S(RETURN)
       TDUMP('UNREAD',1)
UNREAD.END   OPSYN('!', .UNREAD, 1)

*
* The mapping function package
*
* MAP, MAPC, MAPLIST, MAPCAR, MAPCON, & MAPCAN
*
 DEXTERN('MAP(FN,L)')
*
 DEXTERN('MAPC(FN,L)')
*
 DEXTERN('MAPLIST(FN,L)R')
*
 DEFINE('MAPCAR(FN,L)A,R')          :(MAPCAR.END)
MAPCAR
      MAPCAR   =
+         ( STRINGARG(.MAPCAR,1,.FN)
+            LISTARG(.MAPCAR,2,.L) )
+              NIL
MAPCAR1  A = POP( .L)    :F(MAPCAR2)
      R = APPLY(FN,A)    :F(FRETURN)
      MAPCAR = R ~ MAPCAR      :(MAPCAR1)
MAPCAR2  MAPCAR = LREVERSE(MAPCAR)       :(RETURN)
MAPCAR.END
*
 DEFINE('MAPCARV(FN,L)A,R')    :(MAPCARV.END)
MAPCARV
      MAPCARV  =
+        ( STRINGARG(.MAPCARV,1,.FN)
+           LISTARG(.MAPCARV,2,.L) )
+              NIL
MAPCARV1  A =  POP( .L)  :F(RETURN)
      R = APPLY(FN,A)    :F(FRETURN)
      MAPCARV  = R ~ MAPCARV     :(MAPCARV1)
MAPCARV.END
*
 DEXTERN('MAPCON(FN,L)')
*
 DEXTERN('MAPCAN(FN,L)')
*
 DEXTERN('EVERY(FN,L)A,V')
*
 DEXTERN('EVLIS(EV...L.)EV...T.')
*
 DEXTERN('SOME(FN,L)A,V')
*
 DEXTERN('SUBSET(FN,L)A,V')

*
* A nice arithmetic package from Gimpel (1976), Chapter 15
*
*
* Mathematical constants
*
 P...I. = 3.14159265358979
 LN...10. = 2.3025850929940456840
 NAT...BASE. = 2.718281828459045
*
 DEXTERN('FLOOR(X)')
*
 DEXTERN('CEIL(X)')
*
 DEXTERN('SQRT(Y)T')
*
 DEXTERN('RAD(D)')
*
 DEXTERN('DEG(R)')
*
 DEXTERN('SIN(A)K')
*
 DEFINE('SIN.(A)K')
*
 DEXTERN('COS(A,S)K')
*
 DEFINE('COS.(A,S)P2')
*
 DEXTERN('TAN(Z)')
*
 DEXTERN('ACOS(X)K,TERM,T')
*
 DEXTERN('ASIN(X)')
*
 DEXTERN('ATAN(X)')
*
 DEXTERN('LOG(X,B)')
*
 DEXTERN('CLOG(X)FACTOR,T,K')
*
 DEXTERN('RAISE(X,Y)')
*
* End of arithmetic package
*
*
* Sort routine:  A variant of Quicksort
*
*
 DEXTERN('SORT.(A,II,JJ,P)IU,IL,M,I,J,K,IJ,T,L,TT')
 DEFINE('SORT.LE(X,Y)')
 DEFINE('SORT.GE(X,Y)')
 DEFINE('SORT.LT(X,Y)')
 DEFINE('SORT.GT(X,Y)')
*
* Convert array to list
 DEXTERN( 'CAL(A)N' )
*
* Convert list to array
*
 DEXTERN( 'CLA(L)N' )
*
******     End of core functions
*
*
