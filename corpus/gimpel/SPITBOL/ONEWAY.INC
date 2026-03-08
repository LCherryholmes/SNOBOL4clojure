* ONEWAY.inc - ONEWAY(S) will return a one-way cipher of the
*	       alphabetic string S.
*
-INCLUDE "lpad.inc"
-INCLUDE "baseb.inc"
-INCLUDE "rpermute.inc"
-INCLUDE "base10.inc"
-INCLUDE "blend.inc"
	DEFINE('ONEWAY(S)A,SIZE,C,K,SB')	:(ONEWAY_END)
ONEWAY	RAN_VAR  =  1
*	The length of A must be a power of 2.
	A  =  'ABCDEFGHIJKLMNOPQRSTUVWXYZ012345'
	PWR  =  5
ONEWAY_1 S  LEN(1) . C  =			:F(ONEWAY_2)
	A    @K  C				:F(ERROR)
	SB  =  SB  LPAD(BASEB(K,2),PWR,'0')
	RAN_VAR  =  REMDR(RAN_VAR * 2 ** PWR + K, 414971)
						:(ONEWAY_1)
ONEWAY_2
	RPERMUTE(BLEND(SB,REPLACE(SB,'01','10')))
+	LEN(SIZE(SB)) . SB
ONEWAY_3
	SB    LEN(PWR) . S  =			:F(RETURN)
	A   POS(BASE10(S,2))  LEN(1) . C
	ONEWAY  =  ONEWAY  C			:(ONEWAY_3)
ONEWAY_END
