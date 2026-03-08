* SUBSTR.inc - Extract substring
*
*  Built-in function in SNOBOL4+ and Spitbol.  Bypass definition:
*	DEFINE('SUBSTR(S,I,L)')			:(SUBSTR_END)
*SUBSTR	S  LEN(*(I - 1)) LEN(*L) . SUBSTR	:S(RETURN)F(FRETURN)
*SUBSTR_END

