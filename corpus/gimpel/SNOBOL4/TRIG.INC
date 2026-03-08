-MODULE TRIG
*
*  Note:  The SNOBOL4+ system contains the external assembly-
*  langauge function TAN.COM, which requires an 8087 chip.
*  If you are using SNOBOL4+ and have such a chip, it is far
*  more efficient to use that function, and to derive SIN
*  and COS from the tangent.
*
*  Modified by Catspaw 12-30-90.  The original contained the
*  statement:
*
*	DEXP('COS(A) = SQRT(1 - SIN(A) ** 2)')
*
*  which is only correct for -PI/2 <= A <= PI/2.
*
*  To obtain a COS function that produces the correct sign for
*  the entire range -2*PI <= A <= 2*PI, we rely on the fact that
*  SIN and COS are the same waveform phase shifted by PI / 2.
*
*
-INCLUDE "dexp.inc"
	DEFINE('SIN(A)K')
	DEFINE('SIN.(A)')
	PI.  =  3.14159265358979
	PI.2 =  PI. / 2.0			:(SIN_END)
SIN	SIN  =  LT(A,0)  -SIN(-A)		:S(RETURN)
	SIN  =  LT(A,2 * PI.)  SIN.(A)		:S(RETURN)
	K    =  CONVERT(A / (2 * PI.) , 'INTEGER')
	SIN  =  SIN.(A - K * 2 * PI.)		:(RETURN)
SIN.	SIN.  =  EQ(27., 27. - 4 * A * A)  A	:S(RETURN)
	A     =  SIN.(A / 3.)
	SIN.  =  A * (3 - 4 * A * A)		:(RETURN)
SIN_END
	DEXP('COS(A) = SIN(A + PI.2)')
	DEXP('TAN(A) = SIN(A) / COS(A)')
