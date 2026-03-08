* PHYSICAL.inc - Routines to maintain physical units during
*		 calculations.
*
*	NORM(X) will normalize a physical quantity, producing a
*	unique specification for comparison purposes.
*
*	XY() will normalize the two arguments of an arithmetic
*	operation (assumed to be X and Y).  It returns success
*	only if neither argument is a physical quantity.
*
* CAUTION:  As presented in the book, the built in arithmetic
*	    functions are redefined in an upwardly compatible manner.
*	    However, SPITBOL does not permit such redefinitions, 
*	    therefore, this is coded in terms of the other function
*	    names.
*
*	New function names		Equivalent
*		MINUS			UNARY -
*		DIFF			BINARY -
*		SUM			BINARY +
*		MULT			BINARY *
*		DIV			BINARY /
*		EQ.			EQ
*
-INCLUDE "order.inc"
*-INCLUDE "redefine.inc"
	DATA('PHYS(VAL,NUM,DEN)')
*	REDEFINE('-', 'MINUS(X)')
*	REDEFINE('+', 'SUM(X,Y)')
*	REDEFINE('-', 'DIFF(X,Y)')
*	REDEFINE('*', 'MULT(X,Y)')
*	REDEFINE('/', 'DIV(X,Y)')
*	REDEFINE( , 'EQ(X,Y)')

	DEFINE('MINUS(X)')
	DEFINE('SUM(X,Y)')
	DEFINE('DIFF(X,Y)')
	DEFINE('MULT(X,Y)')
	DEFINE('DIV(X,Y)')
	DEFINE('EQ.(X,Y)')

	DEFINE('NORM(X)C')			:(NORM_END)
NORM	X  =  DIFFER(DATATYPE(X), 'PHYS')  PHYS(X)
	NORM  =  X
	DEN(X)  =  ORDER(DEN(X))
	NUM(X)  =  ORDER(NUM(X))
NORM_1	IDENT(DEN(X))				:S(RETURN)
	NUM(X)  ANY(DEN(X)) . C  =		:F(RETURN)
	DEN(X)  C  =				:(NORM_1)
NORM_END

	DEFINE('XY()')				:(XY_END)
XY	(DIFFER(DATATYPE(X), 'PHYS')
+	DIFFER(DATATYPE(Y), 'PHYS'))		:S(RETURN)
	X  =  NORM(X)  ;  Y  =  NORM(Y)		:(FRETURN)
XY_END						:(PHYSICAL_END)

MINUS	MINUS  =  XY()  -X			:S(RETURN)
	MINUS  =  PHYS(-VAL(X),NUM(X),DEN(X))	:(RETURN)

SUM	SUM  =  XY()  X + Y			:S(RETURN)
	SUM = PHYS(VAL(X) + VAL(Y), NUM(X), DEN(X))  :(RETURN)

DIFF	DIFF  =  SUM(X,MINUS(Y))		:(RETURN)

MULT	MULT  =  XY()  X * Y			:S(RETURN)
	MULT  =  PHYS(VAL(X) * VAL(Y), NUM(X) NUM(Y),
+	DEN(X) DEN(Y))				:(RETURN)

DIV	DIV  =  XY()  X / Y			:S(RETURN)
	DIV  =  PHYS(VAL(X) / VAL(Y), NUM(X) DEN(Y),
+	DEN(X) NUM(Y))				:(RETURN)

EQ.	XY()					:F(EQ_1)
	EQ(X,Y)					:S(RETURN)F(FRETURN)
EQ_1	(EQ(VAL(X),VAL(Y)) IDENT(NUM(X),NUM(Y))
+	IDENT(DEN(X),DEN(Y)))			:S(RETURN)F(FRETURN)
PHYSICAL_END

*  Sample physical unit definitions:
 	MET. = PHYS(1, 'M')
 	SEC. = PHYS(1, 'S')
 	KG.  = PHYS(1, 'K')
 	NEWT. = DIV(MULT(MET.,MET.), MULT(SEC.,SEC.))
 	JL.  = MULT(NEWT.,MET.)
 	IN.  = DIV(MET.,39.4)
 	FT.  = MULT(12,IN.)
 	MI.  = MULT(5280,FT.)
 	ACRE = DIV(MULT(MI.,MI.),640)
