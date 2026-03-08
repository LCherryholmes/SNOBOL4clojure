* LOG.inc - CLOG(X) will return the common log (base 10) of X.
*	    LOG(X,B) will return the logarithm of X to the base B.
*	    LOG(X) will return the natural logarithm of X.
*
*	Modified for SPITBOL, which provides a LN function.
*
-INCLUDE "dexp.inc"

	LN_10  =  2.3025850929940456840
	NAT_BASE  =  2.718281828459045
	DEXP('LOG(X,B) = NE(B,0) LN(X) / LN(B) ;'
+	'   LOG   = EQ(B,0) LN(X)  ;'   )
* Old code, not needed in SPITBOL.
*	DEXP('LOG(X,B) = NE(B,0) CLOG(X) / CLOG(B) ;'
*+	'   LOG   = EQ(B,0) CLOG(X) * LN_10   ;'  )

	DEFINE('CLOG(X)FACTOR,T,K')		:(CLOG_END)
CLOG
* Next line added for SPITBOL.  Remove for other systems.
	CLOG = LOG(X,10)			:(RETURN)
* Original code, not needed for SPITBOL.
*	FACTOR  =  X / X
*CLOG_1	X  =  LT(X,1)  1 / X			:F(CLOG_2)
*	FACTOR  =  -FACTOR
*CLOG_2	EQ(X,1.0)				:S(RETURN)
*	K  =  SIZE(CONVERT(X,'INTEGER')) - 1	:F(CLOG_4)
*	EQ(K,0)					:S(CLOG_3)
*	CLOG  =  CLOG + K * FACTOR
*	T  =  NE(CLOG,T)  CLOG			:F(RETURN)
*	X  =  X / 10. ** K
*CLOG_3	FACTOR  =  FACTOR / 10.
*	X  =  X ** 10				:(CLOG_1)
*CLOG_4	K  =  10
*	X  =  X / 10. ** K
*	CLOG  =  CLOG + K * FACTOR		:(CLOG_2)
CLOG_END
