* SQRT.inc - SQRT is a built-in function in SPITBOL,
*	     therefore the entire routine is commented out.
*
*	DEFINE('SQRT(Y)T,ERR,SLOPE')		:(SQRT_END)
*SQRT	LT(Y,0)					:S(FRETURN)
*	EQ(Y,0)					:S(RETURN)
*	SQRT = LT(Y,0.05)  1. / SQRT(1. / Y)	:S(RETURN)
*	SQRT = GT(Y,4096) SQRT(Y / 4096.) * 64.	:S(RETURN)
*	SQRT = GT(Y,16)   SQRT(Y / 16.) * 4.	:S(RETURN)
*	SQRT = (Y + 1.) / 2.
*	T    = SQRT
*SQRT_1	ERR   =  SQRT * SQRT - Y
*	SLOPE =  2. * SQRT
*	SQRT  =  SQRT - (ERR / SLOPE)
*	T     =  LT(SQRT,T)  SQRT		:S(SQRT_1)F(RETURN)
*SQRT_END

