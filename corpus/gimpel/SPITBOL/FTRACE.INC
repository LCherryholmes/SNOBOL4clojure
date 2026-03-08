* FTRACE.inc - FTRACE(PROTO,LABEL) used to redefine the DEFINE
*	       function so that subsequent DEFINE calls trace
*	       the new function with user-provided routines
*	       FTR_CALL and FTR_RET.
*
*	DOES NOT WORK IN SPITBOL, WHICH DOES NOT PERMIT BUILT-IN
*	FUNCTIONS TO BE REDEFINED.
*
	DEFINE('FTRACE(PROTO,LABEL)NAME')
	OPSYN('DEFINE.','DEFINE')
	OPSYN('DEFINE','FTRACE')
	&TRACE  =  50000			:(FTRACE_END)
FTRACE	DEFINE.(PROTO, LABEL)
	PROTO   BREAK('(') . NAME
	TRACE(NAME, 'CALL', , 'FTR_CALL')
	TRACE(NAME, 'RETURN', , 'FTR_RET')	:(RETURN)
FTRACE_END
