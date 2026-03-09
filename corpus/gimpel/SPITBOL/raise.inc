* RAISE.inc - RAISE(X,Y) will raise X to the Y power, for real X,Y.
*
* Because X ** Y is provided in SPITBOL, most of this routine
* has been commented out.
*
*-INCLUDE "log.inc"
-INCLUDE "dexp.inc"

* Modified for SPITBOL, which provides X ** Y for real X,Y
	DEXP('RAISE(X,Y) = X ** Y')		:(EXP_END)
*	DEXP('RAISE(X,Y) = EXP(Y * LOG(X))')
*
*	DEFINE('EXP(X)TERM,K,T')
*	NAT_BASE  =  2.718281828459045		:(EXP_END)
*EXP	EXP  =  LT(X,0)  1. / EXP(-X)		:S(RETURN)
*	K  =  GT(X,1)  CONVERT(X,'INTEGER')	:F(EXP_1)
*	EXP  =  EXP(X - K) * NAT_BASE ** K	:(RETURN)
*EXP_1	TERM =  1.
*EXP_2	EXP  =  EXP + TERM
*	K    =  K + 1.
*	TERM =  TERM * X / K
*	T    =  NE(T,EXP)  EXP			:S(EXP_2)F(RETURN)
*EXP_END

