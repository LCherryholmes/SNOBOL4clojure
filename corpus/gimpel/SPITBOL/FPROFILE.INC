* FPROFILE - FPROFILE() traces program execution, and develops
*	     counts in array FP_ARY of the number of times each
*	     statement is executed.
*
*	     It is far more efficient to use SPITBOL's &PROFILE
*	     keyword than to use FPROFILE.
*
-INCLUDE "lprog.inc"
	DEFINE('FPROFILE()')
	FP_ARY  =  ARRAY(LPROG())
	TRACE(.STCOUNT, 'KEYWORD',,'FPROFILE')
	&TRACE  =  100000			:(FPROFILE_END)
FPROFILE	FP_ARY<&LASTNO> = FP_ARY<&LASTNO> + 1	:(RETURN)
FPROFILE_END
