* SKIM.inc - SKIM(S) "skims off" the first appearance of each
*	     different character of S and returns the result.
*	     SKIP("MISSISSIPPI") returns "MISP".
*
-INCLUDE "diff.inc"
	DEFINE('SKIM(S)C')			:(SKIM_END)
SKIM	S   LEN(1) . C  =			:F(RETURN)
	SKIM   C				:S(SKIM_D)
	SKIM  =  SKIM  C			:(SKIM)
SKIM_D	S  =  DIFF(S, SKIM)			:(SKIM)
SKIM_END
