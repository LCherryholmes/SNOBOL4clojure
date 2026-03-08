* Arc Trig Functions - ACOS, ASIN, ATAN
*
-INCLUDE "sqrt.inc"
-INCLUDE "dexp.inc"
* Note: SPITBOL provides a built-in ATAN function.
*	Therefore, we define ACOS in terms of ATAN.
*
	DEFINE('ACOS(X)K,TERM,T')
	PI.  =  3.14159265358979		:(ACOS_END)
ACOS
* Remove the following line if not SPITBOL 
	ACOS  = (EQ(X,0.0) PI. / 2, ATAN(SQRT(1.0 - X * X) / X)) :(RETURN)

* Following power series expansion not used in SPITBOL,
* provided here for information only.
	ACOS  =  LT(X,0)  PI. - ACOS(-X)	:S(RETURN)
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
* Remove the following comment marker in column 1 if not SPITBOL:
*	DEXP('ATAN(X) = ACOS(1. / SQRT(1 + X * X))')
	DEXP('ASIN(X) = (PI. / 2) - ACOS(X)')
