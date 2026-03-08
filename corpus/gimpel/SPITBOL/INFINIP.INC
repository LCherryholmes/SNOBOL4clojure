* INFINIP.inc - Infinite precision integer arithmetic package.
*
* CAUTION:  As presented in the book, the built in arithmetic
*	    functions are redefined in an upwardly compatible manner.
*	    However, SPITBOL does not permit such redefinitions, 
*	    therefore, this is coded in terms of the other function
*	    names.
*
*	New function names		Equivalent
*		MINUS			UNARY -
*		GT.			GT
*		EQ.			EQ
*		GE.			GE
*		NE.			NE
*		LT.			LT
*		LE.			LE
*		DIFF			BINARY -
*		SUM			BINARY +
*		MULT			BINARY *
*		DIV			BINARY /
*		REMDR.			REMDR
*
*-INCLUDE "redefine.inc"
-INCLUDE "swap.inc"
-INCLUDE "lpad.inc"
*	REDEFINE('-','MINUS(X)Y')
*	REDEFINE(   ,'GT(X,Y)')
*	REDEFINE(   ,'EQ(X,Y)')
*	REDEFINE(   ,'GE(X,Y)')
*	REDEFINE(   ,'NE(X,Y)')
*	REDEFINE(   ,'LT(X,Y)')
*	REDEFINE(   ,'LE(X,Y)')
*	REDEFINE('-','DIFF(X,Y)')
*	REDEFINE('+','SUM(X,Y)X1,X2,Y1,Y2,K')
*	REDEFINE('*','MULT(X,Y)X1,X2,K')
*	REDEFINE('/','DIV(X,Y)X1,X2,Y1,Y2,T,T1,T2,KX,KY')
*	REDEFINE(   ,'REMDR(X,Y)')
*
	DEFINE('MINUS(X)Y')
	DEFINE('GT.(X,Y)')
	DEFINE('EQ.(X,Y)')
	DEFINE('GE.(X,Y)')
	DEFINE('NE.(X,Y)')
	DEFINE('LT.(X,Y)')
	DEFINE('LE.(X,Y)')
	DEFINE('DIFF(X,Y)')
	DEFINE('SUM(X,Y)X1,X2,Y1,Y2,K')
	DEFINE('MULT(X,Y)X1,X2,K')
	DEFINE('DIV(X,Y)X1,X2,Y1,Y2,T,T1,T2,KX,KY')
	DEFINE('REMDR.(X,Y)')
	SIGN_OFF   =  POS(0)  '-'
	LDG_ZEROS  =  BREAK('123456789') | RTAB(1)
	NO_DIGITS  =  8
	DEFINE('SMALL()')
	DEFINE('SPLIT(NAME,PAT)')		:(INFINIP_END)
SMALL	(LE(SIZE(X),NO_DIGITS)
+	LE(SIZE(Y),NO_DIGITS))			:S(RETURN)F(FRETURN)
SPLIT	PAT  =  IDENT(PAT)  LEN(SIZE($NAME) / 2)
	$NAME  (PAT | '') . $(NAME 1) @SPLIT  (SPAN('0') | '')
+	REM . $(NAME 2)
	SPLIT  =  SIZE($NAME) - SPLIT		:(RETURN)
MINUS	MINUS  =  SMALL()  -X			:S(RETURN)
	MINUS  =  X
	MINUS     SIGN_OFF  =			:S(RETURN)
	MINUS  =  '-'  X			:(RETURN)
GT.	SMALL()					:F(GT_1)
	GT(X,Y)					:S(RETURN)F(FRETURN)
GT_1	X  SIGN_OFF  =				:F(GT_2)
	Y  SIGN_OFF  =				:F(FRETURN)
	SWAP(.X,.Y)
GT_2	Y  SIGN_OFF  =				:S(RETURN)
	LGT(LPAD(X,SIZE(Y),'0'),
+	LPAD(Y,SIZE(X),'0'))			:S(RETURN)F(FRETURN)

EQ.	SMALL()					:F(EQ_1)
	EQ(X,Y)					:S(RETURN)F(FRETURN)
EQ_1	IDENT(X,Y)				:S(RETURN)F(FRETURN)

GE.	~(~GT.(X,Y) ~EQ.(X,Y))			:S(RETURN)F(FRETURN)
NE.	EQ.(X,Y)				:S(FRETURN)F(RETURN)
LT.	GE.(X,Y)				:S(FRETURN)F(RETURN)
LE.	GT.(X,Y)				:S(FRETURN)F(RETURN)
DIFF	DIFF  =  SUM(X,MINUS(Y))		:(RETURN)
SUM	SUM  =  SMALL()  X + Y			:S(RETURN)
	SUM  =  LT.(X,0)  MINUS(SUM(MINUS(X),MINUS(Y)))	:S(RETURN)
	Y    SIGN_OFF  =			:S(SUM_1)
	(LT.(X,Y)  SWAP(.X,.Y))
	K  =  SPLIT(.X)
	Y  =  SUM(Y,X2)
	SPLIT(.Y,RTAB(K))
	SUM  =  SUM(Y1,X1) LPAD(Y2,K,'0')	:(RETURN)
SUM_1	SUM  =  GT.(Y,X)  MINUS(DIFF(Y,X))	:S(RETURN)
	Y    =  LPAD(Y,SIZE(X),'0')
*       Add 10's complement of Y
	SUM  =  SUM(SUM(X,1),REPLACE(Y,'0123456789','9876543210'))
	SUM  '1'  LDG_ZEROS  REM . SUM		:(RETURN)
MULT	MULT  =  LE(SIZE(X) + SIZE(Y),NO_DIGITS)
+	X * Y					:S(RETURN)
	MULT  =  LT.(X,0)  MULT(MINUS(X),MINUS(Y)) :S(RETURN)
	MULT  =  LT.(Y,0)  MINUS(MULT(X,MINUS(Y))) :S(RETURN)
	(GT.(Y,X)  SWAP(.X,.Y))
	MULT  =  EQ.(Y,0)  0			:S(RETURN)
	K  =  SPLIT(.X)
	MULT  =  MULT(Y,X1)  DUPL('0',K)
	MULT  =  SUM(MULT,MULT(X2,Y))		:(RETURN)
DIV	DIV  =  SMALL()  X / Y			:S(RETURN)
	DIV  =  LT.(X,0)  MINUS(DIV(MINUS(X),Y)) :S(RETURN)
	DIV  =  LT.(Y,0)  MINUS(DIV(X,MINUS(Y))) :S(RETURN)
	DIV  =  GT.(Y,X)  0			:S(RETURN)
* Original statements from book:
*	KY  =  SPLIT(.Y,LEN(NO_DIGITS / 2) | REM)
*	KX  =  SPLIT(.X,LEN(NO_DIGITS))
* Suggested replacement to avoid generating error terms that are
* larger than the incoming numerator.  Such a situation can produce
* cycles.
	KY  =  SPLIT(.Y,LEN(NO_DIGITS / 2) | REM)
	KX  =  SPLIT(.X,LEN(NO_DIGITS / 2) | REM)
	KX  =  LT.(X1,Y1) SPLIT(.X,LEN(NO_DIGITS / 2 + 1) | REM)
* End of replacement statements.
	T1  =  DIV(X1,Y1)
	T2  =  DUPL('0', KX - KY)
	T   =  DIFF(X,MULT(T1,Y)  T2)
	DIV =  T1  T2
	T   =  LT.(T,0)  DIFF(SUM(T,1),Y)
	DIV =  SUM(DIV,DIV(T,Y))		:(RETURN)
REMDR.	REMDR.  =  DIFF(X,MULT(DIV(X,Y),Y))	:(RETURN)
INFINIP_END
