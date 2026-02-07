package lang.core.operators

fun operatorPrecedence(): OperatorInfoMap {
    return operatorsInfo {
        level {
            oper(OperatorType.SIZEOF)
            oper(OperatorType.NEW)
            oper(OperatorType.DELETE)
        }
        
        level {
            oper(OperatorType.INCREMENT)
            oper(OperatorType.DECREMENT)
            oper(OperatorType.NON_NULL_ASSERT)
        }
        
        level {
            oper(OperatorType.NOT)
            oper(OperatorType.BIN_NOT)
            oper(OperatorType.AS)
            oper(OperatorType.IS)
        }
        
        level {
            oper(OperatorType.MUL)
            oper(OperatorType.DIV)
            oper(OperatorType.REMAINDER)
        }
        
        level {
            oper(OperatorType.SHIFT_LEFT)
            oper(OperatorType.SHIFT_RIGHT)
        }
        
        level {
            oper(OperatorType.PLUS)
            oper(OperatorType.MINUS)
        }
        
        level {
            oper(OperatorType.DOT)
            oper(OperatorType.SCOPE)
        }
        
        level { oper(OperatorType.AMPERSAND) }
        
        level { oper(OperatorType.BIN_XOR) }
        
        level { oper(OperatorType.BIN_OR) }
        
        level { oper(OperatorType.INFIX) }
        
        level {
            oper(OperatorType.LESS)
            oper(OperatorType.LESS_EQUAL)
            oper(OperatorType.GREATER)
            oper(OperatorType.GREATER_EQUAL)
            oper(OperatorType.EQUAL)
            oper(OperatorType.NOT_EQUAL)
        }
        
        level { oper(OperatorType.AND) }
        
        level { oper(OperatorType.OR) }
        
        level {
            oper(OperatorType.QUESTION)
            oper(OperatorType.COLON)
        }
        
        level {
            oper(OperatorType.ASSIGN)
            oper(OperatorType.PLUS_ASSIGN)
            oper(OperatorType.MINUS_ASSIGN)
            oper(OperatorType.MUL_ASSIGN)
            oper(OperatorType.DIV_ASSIGN)
            oper(OperatorType.REMAINDER_ASSIGN)
            oper(OperatorType.BIN_AND_ASSIGN)
            oper(OperatorType.BIN_OR_ASSIGN)
            oper(OperatorType.BIN_XOR_ASSIGN)
            oper(OperatorType.SHIFT_LEFT_ASSIGN)
            oper(OperatorType.SHIFT_RIGHT_ASSIGN)
        }
        
        level { oper(OperatorType.DOUBLE_DOT) }
        
        level {
            oper(OperatorType.IN)
            oper(OperatorType.UNTIL)
            oper(OperatorType.ELVIS)
            oper(OperatorType.ARROW)
            oper(OperatorType.COMMA)
        }
    }

}