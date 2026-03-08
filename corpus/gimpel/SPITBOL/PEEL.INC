* PEEL.inc - PEEL(NAME,N) will peel off and return a prefix from the
*	     named string.  The prefix is to be as large as possible
*	     but not longer than N characters.  The named string will
*	     be modified.  The prefix will be broken off from the
*	     named string only at a suitable break point.  The break
*	     may never appear within quotes.  It may occur before any
*	     of the characters in BEFORE or after any of the characters
*	     in AFTER.  If no prefix can be found other than the null
*	     string then PEEL will fail.
* 
-INCLUDE "balx.inc"
	DEFINE('PEEL(NAME.,N.)K1.,K2.')
	BEFORE  =  ') ,>'
	AFTER  =  '( ,<'
	PEEL.K2. =  POS(0)  TAB(*K1.)  (ANY(AFTER) @K2.  |
+	BALX(,'"' "'") (@K2. ANY(BEFORE) | ANY(AFTER) @K2.  |
+	RPOS(0) @K2.))
						:(PEEL_END)
PEEL	LE(SIZE($NAME.),N.)			:F(PEEL_1)
	PEEL  =  $NAME.
	$NAME.  =				:(RETURN)
PEEL_1	$NAME.    PEEL.K2.			:F(ERROR)
	GT(K2.,N.)				:S(PEEL_2)
	K1.  =  K2.				:(PEEL_1)
PEEL_2	EQ(K1.,0)				:S(FRETURN)
	$NAME.    LEN(K1.) . PEEL  =		:(RETURN)
PEEL_END
