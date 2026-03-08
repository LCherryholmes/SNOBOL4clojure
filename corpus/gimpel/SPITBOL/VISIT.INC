* VISIT.inc - VISIT(ST,SIGN) will visit every node of a
*	      configuration, once and only once, calling
*	      user-supplied PROCESS(ND) upon arrival, where ND
*	      is the node visited.
*
*	      VISIT(ST) will visit every node of the configuration
*	      headed by structure ST.  VISIT(ST,-1) will reset
*	      the marks left in the structure.
*
* CAUTION - This function tries to be helpful by redefining the
* built-in DATA and FIELD functions to allow for an extra MARK
* field in all structures.  This redefinition is not permitted
* in SPITBOL, hence we create functions VDATA and VFIELD which
* should be used instead with marked structres.
*
-INCLUDE "fld.inc"
	DEFINE('VISIT(SON,SIGN)FATHER,GS,GF,DT,I')

*	OPSYN('OLD_DATA', 'DATA')
	DEFINE('VDATA(S)')			:(VDATA_END)
VDATA	S   ')'  =  ',MARK)'
	DATA(S)					:(RETURN)
VDATA_END
*	OPSYN('OLD_FIELD', 'FIELD')
	DEFINE('VFIELD(DT,I)')			:(VFIELD_END)
VFIELD
	FIELD(DT,I + 1)				:F(FRETURN)
	VFIELD  =  FIELD(DT,I)			:S(RETURN)F(FRETURN)
VFIELD_END
	STND_DT  =  POS(0) ('STRING' | 'INTEGER' | 'REAL'
+	| 'PATTERN' | 'ARRAY' | 'TABLE' | 'NAME' |
+	'EXPRESSION' | 'CODE' | 'EXTERNAL') RPOS(0)
						:(VISIT_END)
VISIT	SIGN  =  EQ(SIGN,0)  1
	DATATYPE(SON)    STND_DT		:S(RETURN)
VISIT_2	PROCESS(SON)
	MARK(SON)  =  SIGN
	I  =  0
VISIT_1	I  =  I + 1
	GS  =  FLD(SON, I)			:F(VISIT_3)
	DATATYPE(GS)   STND_DT			:S(VISIT_1)
	GT(SIGN * MARK(GS), 0)			:S(VISIT_1)
	MARK(SON)  =  SIGN * I
	FLD(SON,I)  =  FATHER
	FATHER  =  SON
	SON  =  GS				:(VISIT_2)
VISIT_3	IDENT(FATHER)				:S(RETURN)
	I  =  SIGN * MARK(FATHER)
	GF  =  FLD(FATHER,I)
	FLD(FATHER,I)  =  SON
	SON  =  FATHER
	FATHER  =  GF				:(VISIT_1)
VISIT_END
