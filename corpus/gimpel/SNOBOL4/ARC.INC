-MODULE ACOS
*
*  Note:  The SNOBOL4+ system contains the external assembly-
*  langauge function ATAN.COM, which requires an 8087 chip.
*  If you are using SNOBOL4+ and have such a chip, it is far
*  more efficient to use that function, and to derive ASIN
*  and ACOS from the arctangent.
*
-INCLUDE "sqrt.inc"
-INCLUDE "dexp.inc"
	DEFINE('ACOS(X)K,TERM,T')
	PI.  =  3.14159265358979		:(ACOS_END)
ACOS	ACOS  =  LT(X,0)  PI. - ACOS(-X)	:S(RETURN)
	ACOS  =  1.0
	TERM  =  1.0
	X     =  1.0 - X
	K     =  1
ACOS_1	TERM  =  TERM * (2 * K - 1) * X  /  (4 * K)
	ACOS  =  ACOS  +  TERM / (2 * K + 1)
	K     =  K + 1
	T     =  NE(ACOS,T)  ACOS		:S(ACOS_1)
	ACOS  =  SQRT(2 * X) * ACOS		:(RETURN)
ACOS_END
	DEXP('ASIN(X) = (PI. / 2) - ACOS(X)')
	DEXP('ATAN(X) = ACOS(1. / SQRT(1 + X * X))')
