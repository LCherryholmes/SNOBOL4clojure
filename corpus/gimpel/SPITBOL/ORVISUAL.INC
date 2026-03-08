* ORVISUAL.inc - Visual version of OR.INC.
*	   This function is identical to OR.INC, except instead of
*	   returning a pattern suitable for immediate usage, it
*	   returns the string equivalent of the pattern.  Because it
*	   is in textual form, this result may be saved to a file.
*	   To convert to a pattern, pass the string to EVAL.
*
*	   OR(LIST) is intended to form the OR (in the pattern sense)
*	   of several strings contained in LIST.  For example,
*
*	   OR(',ABLE,ACTOR,ANCHOR,BAKER,BULL')
*	   is equivalent to
*	   'A' ('BLE' | 'CTOR' | 'NCHOR') | 'B' ('AKER' | 'ULL')
*
*	   Characters in parenthesis impley an ANY-like construction:
*	   OR(',C(AO)D,C(AO)ST') is equivalent to
*	   'C' ANY('AO') ('D' | 'ST')
*
*	   The first character of LIST is used as the delimiter
*	   character between substrings.
*
*  Note 1: Modified with new statement at ORX_0 to detect case where
*	   one string is a prefix of another string: ",LI,LIST".
*	   This reduced to "LI" OR_EXTRACT(",,ST") which case
*	   was not handled properly.
*
*  Note 2: See files ORSORT.INC and ORBREAK.INC for adjunct functions
*	   to improve the efficiency of OR().
*
*  Note 3: When LIST contains many thousands of alternatives, it is
*	   very inefficient to concatenate other patterns (such as POS(0)
*	   or RPOS(0)) with the large pattern tree returned by EVAL.
*	   Use a variable to hold the pattern result of OR, and then use
*	   *variable deferred evaluation to concatenate other patterns to it.
*	   This form of concatenation is very efficient, because it only
*	   is done to the top-level "*" node:
*
*	      S = OR(LIST)		 ;* assume LIST is very large
*	      LIST_P = EVAL(S)		 ;* convert to pattern
*	      P = POS(0) *LIST_P RPOS(0) ;* fast pattern concatenation
* 
-INCLUDE "balrev.inc"
	DEFINE('OR(LIST)BC,SEIZE,ANC,OE')
	DEFINE('OR_EXTRACT()COMMON,IC,P,SUBLIST,T,TLIST,C1,C2')
						:(OR_END)
OR	LIST    LEN(1) . BC			:F(FRETURN)
	SEIZE  =  BREAK(BC) | REM
	ANC  =  POS(0)  BC
	OR  =  OR_EXTRACT()
OR_LOOP OE  =  OR_EXTRACT()			:F(OR_LP2)
	OR  =  (IDENT(OR) "''",OR) " | " (IDENT(OE) "''",OE) :(OR_LOOP)
OR_LP2	OR  =  DIFFER(OR) " (" OR ")"		:(RETURN)
OR_EXTRACT
	TLIST  =  LIST
	LIST    ANC  (BAL . IC  SEIZE) . COMMON	:S(ORX_0)
	IDENT(LIST, NULL)			:S(FRETURN)
	LIST  =  NULL				:(RETURN)
ORX_0	LIST     ANC (BC REM) . LIST		:S(RETURN)
ORX_1	TLIST    ANC  IC			:F(ORX_3)
ORX_2	TLIST    ANC  COMMON  SEIZE  =		:S(ORX_1)
	BALREV(COMMON)      BAL  REM . COMMON	:F(ERROR)
	COMMON  =  BALREV(COMMON)		:(ORX_2)
ORX_3	LIST    ANC  COMMON  SEIZE . T  =	:F(ORX_4)
	SUBLIST  =  SUBLIST  BC  T		:(ORX_3)
ORX_4	COMMON  BREAK('(') . C1 '('  BREAK(')') . C2 ')' =  :F(ORX_5)
	P  =  P  (DIFFER(C1) "'" C1 "' ","") "ANY('" C2 "') "	:(ORX_4)
ORX_5	OR_EXTRACT  =  P  "(" (DIFFER(COMMON) "'" COMMON "'","")
+	 OR(SUBLIST) ")"	:(RETURN)
OR_END
