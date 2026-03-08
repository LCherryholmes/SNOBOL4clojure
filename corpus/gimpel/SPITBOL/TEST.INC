* TEST.inc - TEST(ARG) accepts an unevaluated expression as argument,
*	     and returns a pattern bypasses SNOBOL4's one-character
*	     length assumption (quickscan heuristics).
*
*	     It is never necessary to use TEST(ARG) with SPITBOL,
*	     because SPITBOL does not have a one-character assumption.
*
	DEFINE('TEST(ARG)NAME')			:(TEST_END)
TEST	IDENT(DATATYPE(ARG),'EXPRESSION')	:F(TEST_1)
	NAME  =  'TEST_'  &STCOUNT
	$NAME  =  ARG
	TEST = EVAL("NULL $ *TEST('" NAME "')")	:(RETURN)
TEST_1	TEST  =  ?EVAL($ARG)  .TEST_		:S(NRETURN)F(FRETURN)
TEST_END
