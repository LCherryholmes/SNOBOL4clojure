* RAMM.inc - RAMM(N) uses a Knuth random generator to shuffle the
*	     output of RANDOM.  It redefines RANDOM so that calls
*	     to RANDOM are transferred to RAMM.
*
-INCLUDE "random.inc"
	DEFINE('RAMM(N)K')
	OPSYN('RANDOM.','RANDOM')
	OPSYN('RANDOM','RAMM')
	I  =  0
	RAMM_A  =  ARRAY('0:99')
RAMM_1	RAMM_A<I>  =  RANDOM.(0)		:F(RAMM_END)
	I  =  I + 1				:(RAMM_1)
RAMM	RAM_VAR  =  REMDR(RAM_VAR * 3141 + 11079., 524288)
	K  =  CONVERT((RAM_VAR / 524288.) * 100,'INTEGER')
	RAMM  =  RAMM_A<K>
	RAMM_A<K>  =  RANDOM.(0)
	RAMM  =  NE(N,0) CONVERT(RAMM * N, 'INTEGER') + 1
						:(RETURN)
RAMM_END
